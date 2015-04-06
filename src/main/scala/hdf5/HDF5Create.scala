package main.scala.hdf5

import ncsa.hdf.`object`.FileFormat

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
class HDF5Create {
  val fname = "testFl.hd5"
  // Retrieve an instance of the implementing class for the HDF5 format
  val fileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);

  // If the implementing class wasn't found, it's an error.
  if (fileFormat == null) {
    System.err.println("Cannot find HDF5 FileFormat.");

  } else {
    // If the implementing class was found, use it to create a new HDF5 file
    // with a specific file name.
    //
    // If the specified file already exists, it is truncated.
    // The default HDF5 file creation and access properties are used.
    //
    val testFile = fileFormat.createFile(fname, FileFormat.FILE_CREATE_DELETE);

    // Check for error condition and report.
    if (testFile == null) {
      System.err.println("Failed to create file: " + fname);
    }

    // End of example that creates an empty HDF5 file named H5FileCreate.h5.
  }
}
