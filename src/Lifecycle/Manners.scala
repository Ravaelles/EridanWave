package Lifecycle

import Performance.Tasks.TimedTask
import Utilities.UnitFilters.{IsWarrior, IsWorker}
import Utilities.Time.Seconds

class Manners extends TimedTask {
  
  def enabled: Boolean = With.configuration.enableChat
  
  override def onRun(budgetMs: Long): Unit = {
    if (With.configuration.enableSurrenders
      && With.self.supplyUsed400 == 0
      && With.self.minerals < 50
      && With.units.existsEnemy(IsWorker)
      && With.units.existsEnemy(_.unitClass.isResourceDepot)) {
      surrender()
    } else if (With.configuration.enableSurrenders
      && HopelessGame.shouldConcede(
        frame = With.frame,
        ourWorkers = With.units.countOurs(IsWorker),
        ourCombatUnits = With.units.countOurs(IsWarrior),
        enemyCombatUnits = With.units.countEnemy(IsWarrior),
        relativeStrengthPct = Information.Battles.ArmyStrength.score(
          With.battles.globalAttack.us.skimStrengthUnits,
          With.battles.globalAttack.enemy.skimStrengthUnits))) {
      surrender()
    }
    if (With.frame == Seconds(5)()) {
      if (With.configuration.humanMode) {
        chat("Good luck, " + With.enemy.name + ", and have fun!")
      } else {
        With.history.message.foreach(chat)
      }
    }
  }
  
  private def surrender(): Unit = {
    chat("Well played.")
    With.game.leaveGame()
  }

  def chat(text: String): Unit = {
    if (enabled) {
      With.game.sendText(text)
    }
  }
  
  def onEnd(isWinner: Boolean): Unit = {
    chat(
      if (With.configuration.humanMode)
        f"Good game, ${With.enemy.name}!"
      else if (isWinner)
        "Good game! I still think you're beautiful."
      else
        "Good game! Let's pretend this never happened.")
  }
}
