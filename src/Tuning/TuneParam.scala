package Tuning

/**
 * One tunable knob. The tuned value lives as a public var on its owner
 * module (Atlantis style: readable and directly editable in the file);
 * this descriptor only names it and bounds exploration. get/set are thin
 * lambdas assigning the var directly - no builders, no call chains.
 */
case class TuneParam(
  name: String,
  get: () => String,
  set: String => Unit,
  min: Double,
  max: Double,
  isInt: Boolean)
