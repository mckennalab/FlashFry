package main.scala.hdf5

import ch.systemsx.cisd.hdf5.HDF5Factory

/**
 * Created by aaronmck on 6/16/15.
 */
class HDF5Example {

  def createHDF5Paper(): Unit = {
    val mydata = new Array[Double](1000)
    val writer = HDF5Factory.open("farray.h5")
    writer.writeDoubleArray("mydata", mydata)
    writer.close()
  }

}
