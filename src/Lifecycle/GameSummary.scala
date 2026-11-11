package Lifecycle

/**
 * End-of-game scoreboard, Atlantis GameSummary parity.
 * Printed to stdout (the client log tees it to the terminal) right where
 * the run used to end with only "[launcher] wine-run.sh finished".
 * Pure text builders so the exact box stays pinned by tests.
 */
object GameSummary {

  def box(isWinner: Boolean, killed: Int, lost: Int): String = {
    val result = if (isWinner) "VICTORY!" else "Defeat..."
    "#####################################\n" +
    "############ " + result + " ###############\n" +
    "############ Lost: " + lost + " ###############\n" +
    "########## Killed: " + killed + " ################\n" +
    "#####################################"
  }

  def details(frames: Int, killed: Int, lost: Int, killedResources: Int, lostResources: Int, timesHit: Int): String = {
    val seconds = frames / 24
    val balance = killedResources - lostResources
    "### Total time: " + seconds + " seconds. ########\n" +
    "### Units killed/lost: " + killed + "/" + lost + " ########\n" +
    "### Resource killed/lost: " + (if (balance > 0) "+" + balance else balance.toString) + " #####\n" +
    "### Times hit: " + timesHit + " ########\n" +
    "##########################################"
  }
}
