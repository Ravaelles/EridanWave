package Lifecycle.Configure

import mjson.Json
import org.scalatest.funsuite.AnyFunSuite

class ConfigurationLoaderSuite extends AnyFunSuite {

  private val off = ConfigurationLoader.Flags(
    human = false,
    ladder = false,
    livestream = false,
    tournament = false,
    roundrobin = false,
    elimination = false,
    pretraining = false,
    debugging = false,
    debugginglive = false,
    simviz = false,
    cherryvis = true,
    logstd = false,
    multiCPU = true,
    randomStrategy = false,
    frameMsTarget = 20,
    frameMsLimit = 35,
    auditFormationPaths = false,
    logCombatForecasts = false,
    formationAudit = false,
    fixedBuilds = "")

  test("incompatible mode pairs warn") {
    assert(ConfigurationLoader.validate(off).isEmpty)
    assert(ConfigurationLoader.validate(off.copy(tournament = true, ladder = true)) ==
      Seq("Both tournament and ladder modes are enabled"))
    assert(ConfigurationLoader.validate(off.copy(debugginglive = true)) ==
      Seq("debugginglive enabled without debugging enabled"))
    assert(ConfigurationLoader.validate(off.copy(roundrobin = true, elimination = true)).size == 1)
  }

  test("ladder mode banks history, later modes win shared fields") {
    val laddered = ConfigurationLoader.applyModes(off.copy(ladder = true), new Configuration)
    assert(laddered.historyHalfLife == 40)
    assert(laddered.recentFingerprints == 4)
    val raided = ConfigurationLoader.applyModes(
      off.copy(pretraining = true, randomStrategy = true), new Configuration)
    assert(raided.forcedPlaybook.exists(_.isInstanceOf[Strategery.TestingPlaybook]))
  }

  test("typed values read, missing keys fall back") {
    val json = Json.read("""{"a": 1, "b": "x"}""")
    assert(ConfigurationLoader.getOrDefault(json, "a", 0) == 1)
    assert(ConfigurationLoader.getOrDefault(json, "missing", 7) == 7)
  }
}
