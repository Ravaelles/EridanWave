package Tuning

import TuningHallOfFame.Entry
import org.scalatest.funsuite.AnyFunSuite

class TuningLoopSuite extends AnyFunSuite {

  private val manifestText =
    "map=maps/ums/rav/dragoons_v_map.scx\n" +
    "jar=abc123\n" +
    "games=2\n" +
    "game-1|WIN|8|1|2400|digest1|KiteAway.seriousCooldown=8,KiteAway.maxOffsetTiles=1.2\n" +
    "game-2|LOSS|3|7|3100|digest2|KiteAway.seriousCooldown=12,KiteAway.maxOffsetTiles=2.0\n"

  test("manifest parses game rows with vectors") {
    val games = TuningManifest.parseGames(manifestText)
    assert(games.size == 2)
    assert(games.head.id == "game-1" && games.head.win && games.head.killed == 8 && games.head.lost == 1)
    assert(games.head.vector("KiteAway.seriousCooldown") == 8.0)
    assert( ! games(1).win)
  }

  test("manifest skips header and malformed lines") {
    val games = TuningManifest.parseGames("map=x\nbogus\n" + manifestText.split("\n")(3) + "\n")
    assert(games.size == 1)
  }

  test("hall of fame ranks by fitness and caps capacity") {
    val many = (1 to 60).map(i => Entry(i.toDouble, Map("k" -> i.toDouble)))
    val kept = TuningHallOfFame.keep(many)
    assert(kept.size == TuningHallOfFame.capacity)
    assert(kept.head.fitness == 60.0)
  }

  test("hall of fame round-trips through text") {
    val entries = Seq(Entry(1.5, Map("a" -> 1.0)), Entry(0.5, Map("a" -> 2.0)))
    val reloaded = TuningHallOfFame.load(TuningHallOfFame.save(entries))
    assert(reloaded == TuningHallOfFame.rank(entries))
  }

  test("cold start proposes nothing") {
    val strategy = Tuning.resolve("KiteAwayDuel")
    assert(TuningProposer.propose(strategy, Seq.empty, 0.1, new java.util.Random(7)).isEmpty)
  }

  test("proposal perturbs the champion inside bounds") {
    val strategy = Tuning.resolve("KiteAwayDuel")
    val hof = Seq(Entry(2.0, Map("KiteAway.seriousCooldown" -> 8.0)), Entry(0.5, Map("KiteAway.seriousCooldown" -> 12.0)))
    val proposal = TuningProposer.propose(strategy, hof, 0.1, new java.util.Random(7))
    assert(proposal.size == strategy.params.size)
    val cooldown = proposal("KiteAway.seriousCooldown")
    assert(cooldown >= 6.0 && cooldown <= 14.0)
  }

  test("fragment formats ints without decimals") {
    val strategy = Tuning.resolve("KiteAwayDuel")
    val text = TuningProposer.formatFragment(Map("KiteAway.seriousCooldown" -> 8.6, "KiteAway.maxOffsetTiles" -> 1.25), strategy)
    assert(text.contains("KiteAway.seriousCooldown=9\n"))
    assert(text.contains("KiteAway.maxOffsetTiles=1.25\n"))
  }
}
