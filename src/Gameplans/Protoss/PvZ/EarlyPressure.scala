package Gameplans.Protoss.PvZ

/**
 * Pure early-pressure gate (no game access): a normal PvZ opener waits at
 * home until three complete warriors exist. Two Zealots pushed forward
 * cannot beat a speedling response and only drag the miners off the line.
 */
object EarlyPressure {

  /** Complete warriors required before the first push. */
  val minimumWarriors: Int = 3

  def mayPush(completeWarriors: Int): Boolean =
    completeWarriors >= minimumWarriors
}
