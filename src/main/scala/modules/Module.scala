package modules

/**
  * a trait that standardize how options are passed from the main module
  */
trait Module {
  def runWithOptions(remainingOptions: Seq[String])
}
