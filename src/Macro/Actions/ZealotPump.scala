package Macro.Actions

/**
 * Zealot overstock (no game access): a line past fifteen Zealots with ten
 * times as many Zealots as Dragoons is not an army - it walks into
 * everything that shoots and dies to it while the gates keep making more.
 * Past that point the gates should be making Dragoons, so the pump stops
 * asking for Zealots and the composition breathes.
 */
object ZealotPump {

  /** The rule only starts past this many Zealots. */
  val stockedZealots: Int = 15

  /** Zealots at or above this multiple of Dragoons stop production. */
  val dragoonMultiple: Int = 10

  def shouldStop(zealots: Int, dragoons: Int): Boolean =
    zealots > stockedZealots && zealots >= dragoons * dragoonMultiple
}
