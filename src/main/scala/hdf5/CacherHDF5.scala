package main.scala.hdf5

import java.io.File

import ch.systemsx.cisd.hdf5.HDF5Factory
import ncsa.hdf.`object`.h5.H5Datatype
import ncsa.hdf.`object`.{Datatype, HObject, Group, FileFormat}
import ncsa.hdf.hdf5lib.{HDF5Constants, H5}
import scala.collection.JavaConversions._
import scala.collection._

/**
 * created by aaronmck on 1/7/15
 *
 * Copyright (c) 2015, aaronmck
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. 
 * 2.  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 *
 */


class CacherHDF5(filename: File) {

}

object CacherHDF5 {
  /**
   * Open a specified HDF5 file
   * @param filename the file to open
   * @return a FileFormat handle
   */
  def openHDF5File(filename: File): FileFormat = {
    // get a hdf5 fileformat object
    val fileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5)
    // check to make sure we got a formatter
    if (fileFormat == null) throw new IllegalArgumentException("Unable to get HDF5 file format for file: " + filename)

    // open the file with read and write access
    val fl = fileFormat.createInstance(filename.getAbsolutePath, FileFormat.WRITE)

    if (fl == null) throw new IllegalArgumentException("Failed to open HDF5 file: " + filename)

    // open the file and retrieve the file structure
    fl.open()

    // return the open file
    fl
  }

  /**
   * create an HDF5 group within a file
   * @param openFile the open HDF5 object
   * @param groupName the group to create
   * @param withinGroup the subgroup to place the group in (i.e. /groupa/groupb/groupc/etc). Optional
   * @param createIntermediateGroups should we create any missing groups, or exception out if we don't see them
   * @return the created group
   */
  def createGroup(openFile: FileFormat, groupName: String, withinGroup: Option[String], createIntermediateGroups: Boolean = true): Group = {
    if (openFile == null) throw new IllegalArgumentException("File handle passed in isn't open")

    // the dangerous casting to a root group through various intermediates.  Old school ugliness, hence the wrapping
    var rootGroup = getRootNode(openFile)

    // now try and make the group (or sub-group)
    try {
      var groupToAddTo = rootGroup
      if (withinGroup.isDefined) {
        // traverse down to the specified group's parent, and if createIntermediateGroups, create any needed intermediates
        withinGroup.get.split("/").foreach { tk => {
          val subGroup = (groupToAddTo.getMemberList.toList.find { case (hgr) => (hgr.getName == tk)})
          if (!(createIntermediateGroups || subGroup.isDefined)) throw new IllegalArgumentException("Unable to create group, subgroup " + tk + " doesn't exist")
          else if (!subGroup.isDefined) groupToAddTo = openFile.createGroup(tk, groupToAddTo)
          else groupToAddTo = subGroup.get.asInstanceOf[Group]
        }
        }

      }
      openFile.createGroup(groupName, groupToAddTo)
    } catch {
      case e: Exception => throw new IllegalArgumentException("Unable to add specified group, with message: " + e.getMessage)
    }

  }

  /**
   * get a group with a specific path
   * @param openFile the open HDF5 object
   * @param groupName the group to create
   * @return the group to create
   */
  def getGroup(openFile: FileFormat, groupName: String): Group = {
    if (openFile == null) throw new IllegalArgumentException("File handle passed in isn't open")

    // the dangerous casting to a root group through various intermediates.  Old school ugliness, hence the wrapping
    var rootGroup = getRootNode(openFile)

    // now try and make the group (or sub-group)
    try {
      var groupToAddTo = rootGroup
      groupName.split("/").foreach { tk => {
        println("tk = " + tk)
        val subGroup = (groupToAddTo.getMemberList.toList.find { case (hgr) => (hgr.getName == tk)})
        if (!subGroup.isDefined) throw new IllegalArgumentException("Unable to find specified subgroup")
        else groupToAddTo = subGroup.get.asInstanceOf[Group]
      }
      }
      groupToAddTo
    } catch {
      case e: Exception => {
        e.printStackTrace()
        throw new IllegalArgumentException("Unable to find specified group, with message: " + e.getMessage)
      }
    }
  }

  /**
   * get the open file's root node
   * @param openFile the open file
   * @return a group representing the root node
   */
  def getRootNode(openFile: FileFormat): Group = {
    // the dangerous casting to a root group through various intermediates.  Old school ugliness, hence the wrapping
    try {
      openFile.getRootNode.asInstanceOf[javax.swing.tree.DefaultMutableTreeNode].getUserObject.asInstanceOf[Group]
    } catch {
      case e: Exception => throw new IllegalAccessError("Unable to cast the root node, with message: " + e.getMessage)
    }
  }

  /**
   * Given a filename, create a new HDF5 file
   * @param filename the input filename
   * @return a FileFormat handle to the created file
   */
  def createHDF5File(filename: File): FileFormat = {
    // Retrieve an instance of the implementing class for the HDF5 format
    val fileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5)

    // If the implementing class wasn't found, it's an error.
    if (fileFormat == null) throw new IllegalStateException("Cannot find HDF5 FileFormat.")
    // If the implementing class was found, use it to create a new HDF5 file
    // with a specific file name.
    //
    // If the specified file already exists, it is truncated.
    // The default HDF5 file creation and access properties are used.
    val fl = fileFormat.createFile(filename.getAbsolutePath, FileFormat.FILE_CREATE_DELETE)

    // Check for error condition and report.
    if (fl == null) throw new IllegalStateException("Failed to create file: " + filename)

    // open the file and retrieve the file structure
    fl.open()

    return fl
  }

  /**
   * get the first data member from a group as a mapping from a key type K to a value type V
   * @param openFile the open file handle (as a FileFormat object)
   * @param groupName the group name to get it from
   * @tparam K the key type
   * @tparam V the value type
   * @return a map object from keys to values of the given type
   */
  def getMapFromGroup[K, V](openFile: FileFormat, groupName: String) {
    // : Map[K,V] = {
    val group = getGroup(openFile, groupName)
    val dtype = openFile.createDatatype(Datatype.CLASS_STRING, 4, Datatype.NATIVE, Datatype.NATIVE)

    getGroup(openFile: FileFormat, groupName: String)
  }

  /**
   * get the first data member from a group as a mapping from a key type K to a value type V
   * @param openFile the open file handle (as a FileFormat object)
   * @param groupName the group name to get it from
   * @return a map object from keys to values of the given type
   */
  def writeCRISPRMap(openFile: FileFormat, groupName: String, mp: Map[String, Int], keyLength: Int, mapName: String) {
    val group = getGroup(openFile, groupName)
    val dtype = openFile.createDatatype(Datatype.CLASS_STRING, keyLength, Datatype.NATIVE, Datatype.NATIVE)
    val root = getRootNode(openFile)

    val memberDatatypes = Array[Datatype](
      new H5Datatype(Datatype.CLASS_STRING, keyLength, Datatype.NATIVE, Datatype.NATIVE),
      new H5Datatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE, Datatype.NATIVE))
    val memberNames = Array[String]("key", "value")

    val flattened = mp.map { case (k,v) => (k.getBytes, v) }(collection.breakOut): List[(Array[Byte], Int)]
    val d = openFile.createCompoundDS(
      mapName,
      root,
      Array[Long](mp.size),
      Array[Long](mp.size),
      Array[Long](1),
      0,
      memberNames,
      memberDatatypes,
      Array[Int](mp.size),
      flattened)

  }


  /**
   * get the first data member from a group as a mapping from a key type K to a value type V
   * @param filename the file to write to
   * @param groupName the group name to get it from
   * @return a map object from keys to values of the given type
   */
  def writeCRISPRMap2(filename: String, groupName: String, mp: Map[String, Int]) {
    val writer = HDF5Factory.open("myfile.h5")
    //writer.
    writer.writeStringArray(groupName + "/keys" , mp.keys.toArray)
    writer.writeByteArray(groupName + "/values" , mp.values.map{case(e) => if (e>= 128) 128.toByte else e.toByte}.toArray)
    writer.close()

  }

  /**
   * get the first data member from a group as a mapping from a key type K to a value type V
   * @param filename the file to write to
   * @param groupName the group name to get it from
   * @return a map object from keys to values of the given type
   */
  def readCRISPRMap2(filename: String, groupName: String): Map[String, Byte] = {
    val reader = HDF5Factory.openForReading("myfile.h5")
    val keys = reader.readStringArray(groupName + "/keys")
    val values= reader.readByteArray(groupName + "/values")
    keys.zip(values).toMap
  }
  /**
   * put a map
   * @param openFile the open file handle (as a FileFormat object)
   * @param groupName the group name to get it from
   * @tparam K the key type
   * @tparam V the value type
   * @return a map object from keys to values of the given type
   */
  def putMapFromGroup[K, V](openFile: FileFormat, groupName: String, mp: Map[K, V]) {


  }
}
