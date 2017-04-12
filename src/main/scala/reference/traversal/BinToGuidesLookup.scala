package reference.traversal

import crispr.GuideIndex

/**
  * A bin lookup object -- the bin name and long-encoded guides to search against.
  * This is translated into searches by a traverser
  */
case class BinToGuidesLookup(bin: String, guides: Array[GuideIndex])
