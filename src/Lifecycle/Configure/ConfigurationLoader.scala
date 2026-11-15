package Lifecycle.Configure

import Lifecycle.With
import Strategery.{HumanPlaybook, PretrainingPlaybook, TestingPlaybook, TournamentPlaybook}
import mjson.Json

import java.io.File

object ConfigurationLoader {

  def opponentFixedBuild(text: String, enemyName: String, fallback: String): String = {
    if (text.trim.isEmpty) return fallback

    val entries = Json.read(text).asJsonMap().entrySet().iterator()
    while (entries.hasNext) {
      val entry = entries.next()
      if (entry.getKey.equalsIgnoreCase(enemyName)) return entry.getValue.asString()
    }
    fallback
  }

  def getOrDefault[T](json: Json, property: String, default: T): T = {
    val value = json.at(property)
    if (value == null || value.isNull) {
      return default
    }

    val (ok, wanted) = default match {
      case _: Boolean => (value.isBoolean, "a boolean")
      case _: Int     => (value.isNumber, "an integer")
      case _: Double  => (value.isNumber, "a number")
      case _          => (value.isString, "a string")
    }
    if ( ! ok) {
      With.logger.warn(f"Ignoring $property (want $wanted)")

      return default
    }

    (default match {
      case _: Boolean => value.asBoolean()
      case _: Int     => value.asInteger()
      case _: Double  => value.asDouble()
      case _          => value.asString()
    }).asInstanceOf[T]
  }

  /** Every parsed key in one place: name, default, and how it lands. */
  case class Flags(
    human: Boolean,
    ladder: Boolean,
    livestream: Boolean,
    tournament: Boolean,
    roundrobin: Boolean,
    elimination: Boolean,
    pretraining: Boolean,
    debugging: Boolean,
    debugginglive: Boolean,
    simviz: Boolean,
    cherryvis: Boolean,
    logstd: Boolean,
    multiCPU: Boolean,
    randomStrategy: Boolean,
    frameMsTarget: Int,
    frameMsLimit: Int,
    auditFormationPaths: Boolean,
    logCombatForecasts: Boolean,
    formationAudit: Boolean,
    fixedBuilds: String)

  private def flags(config: Json, base: Configuration): Flags = Flags(
    human          = getOrDefault(config, "human", false),
    ladder         = getOrDefault(config, "ladder", false),
    livestream     = getOrDefault(config, "livestream", false),
    tournament     = getOrDefault(config, "tournament", false),
    roundrobin     = getOrDefault(config, "roundrobin", false),
    elimination    = getOrDefault(config, "elimination", false),
    pretraining    = getOrDefault(config, "pretraining", false),
    debugging      = getOrDefault(config, "debugging", false),
    debugginglive  = getOrDefault(config, "debugginglive", false),
    simviz         = getOrDefault(config, "simviz", false),
    cherryvis      = getOrDefault(config, "cherryvis", true),
    logstd         = getOrDefault(config, "logstd", false),
    multiCPU       = getOrDefault(config, "multiCPU", true),
    randomStrategy = getOrDefault(config, "strategySelectionRandom", false),
    frameMsTarget  = getOrDefault(config, "framemstarget", base.frameTargetMs),
    frameMsLimit   = getOrDefault(config, "framemslimit", base.frameLimitMs),
    auditFormationPaths = getOrDefault(config, "auditFormationPaths", false),
    logCombatForecasts  = getOrDefault(config, "logCombatForecasts", false),
    formationAudit      = getOrDefault(config, "formationAudit", false),
    fixedBuilds    = opponentFixedBuild(
      new FileFlag("fixedbuilds-by-opponent.json").contents,
      With.enemy.name,
      new FileFlag("fixedbuilds.txt").contents))

  /** Incompatible mode pairs, as data: a warning per true row. */
  def validate(flags: Flags): Seq[String] = Seq(
    (flags.tournament && flags.ladder, "Both tournament and ladder modes are enabled"),
    (flags.tournament && flags.livestream, "Both tournament and livestream modes are enabled"),
    (flags.tournament && flags.pretraining, "Both tournament and pretraining modes are enabled"),
    (flags.pretraining && flags.ladder, "Both pretraining and laddermodes are enabled"),
    (flags.pretraining && flags.livestream, "Both pretraining and livestream modes are enabled"),
    (flags.roundrobin && flags.elimination, "Both round robin and elimination modes are enabled"),
    (flags.debugginglive && ! flags.debugging, "debugginglive enabled without debugging enabled"))
    .collect { case (true, warning) => warning }

  /** Mode rules in application order (later rows win shared fields). */
  def applyModes(flags: Flags, base: Configuration): Configuration = {
    val modes: Seq[(Boolean, Configuration => Configuration)] = Seq(
      flags.livestream    -> (_.copy(enableChat = true, enableSurrenders = true)),
      flags.human         -> (_.copy(humanMode = true, enableChat = true, enableSurrenders = true, forcedPlaybook = Some(HumanPlaybook))),
      flags.ladder        -> (_.copy(historyHalfLife = 40, recentFingerprints = 4)),
      flags.tournament    -> (_.copy(forcedPlaybook = Some(TournamentPlaybook))),
      flags.roundrobin    -> (_.copy(historyHalfLife = 30, recentFingerprints = 3)),
      flags.elimination   -> (_.copy(recentFingerprints = 2)),
      flags.pretraining   -> (_.copy(forcedPlaybook = Some(PretrainingPlaybook))),
      flags.debugging     -> (_.copy(enableChat = true, enableSurrenders = true, debugging = true)),
      flags.debugginglive -> (_.copy(enableChat = true, enableSurrenders = true, debugging = true, camera = true, visualizeDebug = true, detectBreakpoints = true, simulationRecordingPeriod = 24 * 3)),
      flags.logstd        -> (_.copy(logstd = true)),
      flags.randomStrategy -> (_.copy(forcedPlaybook = Some(new TestingPlaybook))))
    modes.foldLeft(base) { case (config, (on, apply)) => if (on) apply(config) else config }
  }

  def findConfigFile(): Option[File] = {
    val aiDir = new File(With.bwapiData.ai)
    if ( ! aiDir.isDirectory) {
      With.logger.debug(f"No bwapi-data/AI directory at ${aiDir.getPath}; using default configuration")

      return None
    }

    val configFiles: Array[File] = Option(aiDir.listFiles).getOrElse(Array.empty)
      .filter(f => Seq("Eridan", ".config", ".json").forall(f.getName.contains))
    val configFilesMatching = Seq(configFiles.filter(_.getName.contains(With.self.name)), configFiles).find(_.nonEmpty).getOrElse(Array.empty)
    if (configFilesMatching.isEmpty) {
      With.logger.warn("Didn't find any matching configurations")

      return None
    }

    if (configFilesMatching.length > 1) {
      With.logger.warn("Found multiple matching configurations; using newest: " + configFilesMatching.map(f => f"${f.getName} (${f.lastModified()})").mkString(", "))
    }

    val configFile = configFilesMatching.maxBy(_.lastModified())
    With.logger.debug(f"Using configuration file ${configFile.getName}")
    Some(configFile)
  }

  def load(): Unit = {
    val configFile = findConfigFile().getOrElse(return)
    try {
      val configSource = scala.io.Source.fromFile(configFile)
      val configText   = configSource.mkString
      configSource.close()
      val config = Json.read(configText)
      val parsed = flags(config, With.configuration)
      var next = With.configuration.copy(
        fixedBuilds = parsed.fixedBuilds,
        frameTargetMs = parsed.frameMsTarget,
        frameLimitMs = parsed.frameMsLimit,
        simviz = parsed.simviz,
        auditFormationPaths = parsed.auditFormationPaths,
        logCombatForecasts = parsed.logCombatForecasts,
        simulationAsynchronous = parsed.multiCPU,
        cherryVis = parsed.cherryvis,
        formationAudit = parsed.formationAudit)
      next = cherryVisFlagOverride().map(v => next.copy(cherryVis = v)).getOrElse(next)
      next = formationAuditFlagOverride().map(v => next.copy(formationAudit = v)).getOrElse(next)
      validate(parsed).foreach(With.logger.warn)
      next = applyModes(parsed, next)
      With.configuration = next
      logSummary(parsed, next)
    } catch { case exception: Exception => With.logger.onException(exception) }
  }

  private def logSummary(flags: Flags, config: Configuration): Unit = {
    Seq(
      ("Human",           flags.human.toString),
      ("Ladder",          flags.ladder.toString),
      ("Livestream",      flags.livestream.toString),
      ("Tournament",      flags.tournament.toString),
      ("Round-robin",     flags.roundrobin.toString),
      ("Elimination",     flags.elimination.toString),
      ("Pretraining",     flags.pretraining.toString),
      ("Debugging",       flags.debugging.toString),
      ("DebuggingLive",   flags.debugginglive.toString),
      ("SimViz",          flags.simviz.toString),
      ("CherryVis",       config.cherryVis.toString),
      ("Log stdout",      flags.logstd.toString),
      ("Random strategy", flags.randomStrategy.toString),
      ("Frame MS target", config.frameTargetMs),
      ("Frame MS limit",  config.frameLimitMs),
      ("Fixed build",     config.fixedBuilds)
    )
    .foreach(pair => With.logger.debug(pair._1 + ": " + pair._2))
  }

  /**
   * Explicit --cherryvis control travels outside the config file:
   * JVM property (same-JVM launch) wins over the environment (Wine/scripts),
   * which wins over the JSON value. Accepts 1/true/yes and 0/false/no;
   * garbage warns and keeps the JSON value.
   */
  private def cherryVisFlagOverride(): Option[Boolean] =
    boolFlagOverride(
      Option(System.getProperty("eridanwave.cherryvis")).orElse(Option(System.getProperty("purplewave.cherryvis"))),
      scala.sys.env.get("ERIDANWAVE_CHERRYVIS").orElse(scala.sys.env.get("PURPLEWAVE_CHERRYVIS")),
      "CherryVis")

  /**
   * Explicit --formation-audit control, same channel as --cherryvis above:
   * writes the behavior tally and formation-break distances to
   * bwapi-data/write/formation-audit.txt for post-game reading.
   */
  private def formationAuditFlagOverride(): Option[Boolean] =
    boolFlagOverride(
      Option(System.getProperty("eridanwave.formationAudit")),
      scala.sys.env.get("ERIDANWAVE_FORMATION_AUDIT"),
      "FormationAudit")

  private def boolFlagOverride(fromProperty: Option[String], fromEnv: Option[String], label: String): Option[Boolean] = {
    fromProperty.orElse(fromEnv).flatMap(raw => {
      raw.trim.toLowerCase match {
        case "1" | "true" | "yes" => Some(true)
        case "0" | "false" | "no" => Some(false)
        case _ =>
          With.logger.warn(f"Ignoring invalid $label flag value: $raw (want 1/true/yes or 0/false/no)")
          None
      }
    })
  }
}

