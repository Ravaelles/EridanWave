package Tuning.Strategies

import Micro.Actions.Combat.Decisionmaking.KiteAway
import Tuning.{TuneParam, TuningStrategy}

/**
 * KiteAway duel tuning on the dragoon micro map.
 * Thin lambdas assigning KiteAway's public vars directly - no builders,
 * no call chains; the values stay readable and editable in KiteAway itself.
 */
class KiteAwayDuelStrategy extends TuningStrategy {

  override def name: String = "KiteAwayDuel"

  override def params: Seq[TuneParam] = Seq(
    TuneParam("KiteAway.seriousWoundHp", () => KiteAway.seriousWoundHp.toString, text => KiteAway.seriousWoundHp = text.toDouble.round.toInt, 50, 80, isInt = true),
    TuneParam("KiteAway.criticalWoundHp", () => KiteAway.criticalWoundHp.toString, text => KiteAway.criticalWoundHp = text.toDouble.round.toInt, 10, 40, isInt = true),
    TuneParam("KiteAway.woundScopeFraction", () => KiteAway.woundScopeFraction.toString, text => KiteAway.woundScopeFraction = text.toDouble, 0.0, 0.2, isInt = false),
    TuneParam("KiteAway.seriousCooldown", () => KiteAway.seriousCooldown.toString, text => KiteAway.seriousCooldown = text.toDouble.round.toInt, 6, 14, isInt = true),
    TuneParam("KiteAway.criticalCooldown", () => KiteAway.criticalCooldown.toString, text => KiteAway.criticalCooldown = text.toDouble.round.toInt, 2, 8, isInt = true),
    TuneParam("KiteAway.mildCooldown", () => KiteAway.mildCooldown.toString, text => KiteAway.mildCooldown = text.toDouble.round.toInt, 10, 20, isInt = true),
    TuneParam("KiteAway.enemyMarginTiles", () => KiteAway.enemyMarginTiles.toString, text => KiteAway.enemyMarginTiles = text.toDouble, 1.0, 2.5, isInt = false),
    TuneParam("KiteAway.maxOffsetTiles", () => KiteAway.maxOffsetTiles.toString, text => KiteAway.maxOffsetTiles = text.toDouble, 0.5, 2.5, isInt = false))

  override def mapFile: String = "maps/ums/rav/dragoons_v_map.scx"
  override def gamesPerRound: Int = 5
  override def gameTimeoutSeconds: Int = 750

  /** Wins first, then kill margin (preservation is the point of KiteAway). */
  override def fitness(win: Boolean, killed: Int, lost: Int, frames: Int): Double =
    (if (win) 1.0 else 0.0) + 0.02 * (killed - lost)
}
