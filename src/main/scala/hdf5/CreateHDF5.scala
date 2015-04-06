package main.scala.hdf5

import ncsa.hdf.hdf5lib.exceptions.HDF5Exception
import ncsa.hdf.hdf5lib.{H5, HDF5Constants}
import scala.collection.JavaConversions._
/**
 * created by aaronmck on 1/6/15
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
case class CreateHDF5(flName: String) {
  //val flName = "QuickRead1M_targets.hd5"
  val GROUPNAME = "MyGroup"
  val GROUPNAME_A = "GroupA"
  val DATASETNAME1 = "dset1"
  val DATASETNAME2 = "dset2"
  val DIM1_X = 3
  val DIM1_Y = 3
  val DIM2_X = 2
  val DIM2_Y = 10
  var dataset_id = -1
  var dset1_data = Array.ofDim[Int](DIM1_X,DIM1_Y)
  var dset2_data = Array.ofDim[Int](DIM2_X,DIM2_Y)
  var dims1 = Array[Long](DIM1_X, DIM1_Y )
  var dims2 = Array[Long](DIM2_X, DIM2_Y )
  var group_id = -1

  try {
    val file = H5.H5Fopen(flName, HDF5Constants.H5F_ACC_RDWR, HDF5Constants.H5P_DEFAULT)
    if (file >= 0) {
      println("creating a group")
      val group1_id = H5.H5Gcreate(file, "/" + GROUPNAME, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT)
      // Create group "Group_A" in group "MyGroup" using absolute name.
      if (group1_id >= 0) {
        val group2_id = H5.H5Gcreate(file, "/" + GROUPNAME + "/" + GROUPNAME_A, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT)
        if (group2_id >= 0)
          H5.H5Gclose(group2_id)
      }
      if (group1_id >= 0)
        H5.H5Gclose(group1_id)

      var dataspace_id = H5.H5Screate_simple(2, dims1, null)

      if ((file >= 0) && (dataspace_id >= 0))
        dataset_id = H5.H5Dcreate(file, "/" + GROUPNAME + "/" + DATASETNAME1, HDF5Constants.H5T_STD_I32BE, dataspace_id, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT)

      if (dataset_id >= 0)
        H5.H5Dwrite(dataset_id, HDF5Constants.H5T_NATIVE_INT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dset1_data)

      if (dataspace_id >= 0)
        H5.H5Sclose(dataspace_id)

      if (dataset_id >= 0)
        H5.H5Dclose(dataset_id)

      if (group_id >= 0)
        H5.H5Gclose(group_id)

      if (file >= 0)
        H5.H5Fclose(file)
    }
  } catch {
    case ex: HDF5Exception => println("Failed to open file: " + ex.printStackTrace())
  }
}
