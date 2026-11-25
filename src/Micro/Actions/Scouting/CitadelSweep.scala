package Micro.Actions.Scouting

import Lifecycle.{Enemy, With}
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.Latch

/**
 * Citadel-sweep mission (owner order): a PvP scout probe inside the
 * enemy base sweeps for an unseen Citadel instead of fleeing the first
 * blade on top of it - the Citadel spotted under a Dragoon's guns is
 * worth more than the probe, and the Forge that answers it never starts
 * on a rumour. Entry is sharp (plausible Citadel, unanswered question,
 * unswept base); release answers the question, sweeps the base, walks
 * out or reassigns the probe. Latched per unit, so a hull jittering on
 * the metro edge stays committed instead of flap-retreating every other
 * frame while the base stays dark (and dying uninformative).
 */
object CitadelSweep {

  def fires(
    pvp: Boolean,
    umsMode: Boolean,
    isScoutProbe: Boolean,
    inEnemyMetro: Boolean,
    coreKnown: Boolean,
    answered: Boolean,
    unscoutedBases: Boolean): Boolean =
    pvp && ! umsMode && isScoutProbe && inEnemyMetro && coreKnown && ! answered && unscoutedBases

  def releases(answered: Boolean, swept: Boolean, leftMetro: Boolean, isScout: Boolean): Boolean =
    answered || swept || leftMetro || ! isScout

  private val mission = new Latch

  /**
   * Latched mission verdict (one verdict for every gate): PreserveScout
   * and the helpScout pull-home read this instead of their own knives -
   * while it holds, the Search sweep owns the probe alone.
   */
  def held(unit: FriendlyUnitInfo): Boolean = {
    val isScoutProbe = Protoss.Probe(unit) && unit.agent.isScout
    val inEnemyMetro = unit.metro.exists(_.bases.exists(_.isEnemy))
    val coreKnown = With.units.enemy.exists(e => Protoss.CyberneticsCore(e))
    val answered = With.unitsShown.allEnemies(
      Protoss.CitadelOfAdun,
      Protoss.TemplarArchives,
      Protoss.DarkTemplar,
      Protoss.ArbiterTribunal,
      Protoss.Arbiter) > 0
    val unscouted = With.geography.enemyBases.exists(! _.scoutedByUs)
    mission(unit.id,
      fires = fires(Enemy.protoss(), With.umsMode, isScoutProbe, inEnemyMetro, coreKnown, answered, unscouted),
      releases = releases(answered, ! unscouted, ! inEnemyMetro, isScoutProbe))
  }
}
