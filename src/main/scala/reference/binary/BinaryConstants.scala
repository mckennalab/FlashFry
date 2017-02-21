package reference.binary

/**
  * store binary constants associated with reading and writing our custom file format
  */
object BinaryConstants {
  val magicNumber = 0x1234ABCDE123890l // looks magic enough -- just there to make sure the file what we expect
  val version = 1l
  val bytesPerTarget = 8
  val bytesPerGenomeLocation = 8

  val headerSize = 24 // a long for the magic number, the version, and the number of bins
}
