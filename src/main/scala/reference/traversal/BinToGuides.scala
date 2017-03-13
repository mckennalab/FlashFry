package reference.traversal

/**
  * A bin lookup object -- the bin name and long-encoded guides to search against.
  * This is translated into searches by the
  */
case class BinToGuides(bin: String, guides: Array[Long])
