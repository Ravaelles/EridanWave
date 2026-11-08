package Tuning

import scala.collection.mutable

/**
 * Tuning registry (OCP seam): resolves a strategy by name, randomizes or
 * overrides its params at game start, and remembers the played vector for
 * the game-end score row.
 *
 * Atlantis applyOrRandomize parity: an explicit override is used verbatim
 * (validation mode), an absent key randomizes uniformly within bounds
 * (exploration mode), garbage falls back to random. Inactive unless
 * --tuning-strategy= was passed: default play is untouched.
 */
object Tuning {

  private val strategiesPackage = "Tuning.Strategies."

  private val scoreDirectory  = "tuning"
  private val scoreLastFile   = scoreDirectory + "/last.csv"

  var current: Option[TuningStrategy] = None

  private val applied = new mutable.LinkedHashMap[String, String]

  def active: Boolean = current.isDefined

  /** Played name -> value vector, for the game-end score row. */
  def appliedParams: Map[String, String] = applied.toMap

  def install(): Unit = {
    val strategyName = Option(System.getProperty("purplewave.tuningStrategy")).map(_.trim).filter(_.nonEmpty)
    if (strategyName.isEmpty) return
    current = Some(resolve(strategyName.get))
    applyParams()
  }

  def resolve(name: String): TuningStrategy = {
    // Short names resolve two ways: the literal class name and the strategy
    // name with a Strategy suffix (CLI-friendly: --tuning-strategy=KiteAwayDuel).
    val candidates =
      if (name.contains(".")) Seq(name)
      else Seq(
        strategiesPackage + name,
        name,
        strategiesPackage + name + "Strategy",
        name + "Strategy")
    val errors = new mutable.ArrayBuffer[String]()
    var found: Option[TuningStrategy] = None
    var i = 0
    while (i < candidates.size && found.isEmpty) {
      instantiate(candidates(i)) match {
        case scala.util.Success(strategy) => found = Some(strategy)
        case scala.util.Failure(error) => errors += (candidates(i) + ": " + error)
      }
      i += 1
    }
    found.getOrElse(throw new IllegalArgumentException(
      f"Unknown tuning strategy: $name (${errors.mkString("; ")})"))
  }

  private def instantiate(className: String): scala.util.Try[TuningStrategy] = scala.util.Try {
    val loaded = getClass.getClassLoader.loadClass(className)
    loaded.getDeclaredConstructor().newInstance().asInstanceOf[TuningStrategy]
  }

  def tuneOverride(name: String): Option[String] =
    Option(System.getProperty("purplewave.tune." + name)).map(_.trim).filter(_.nonEmpty)

  /** Explicit override verbatim; otherwise uniform in bounds. Returns (value, fromOverride). */
  def resolveValue(raw: Option[String], min: Double, max: Double, random: java.util.Random): (Double, Boolean) = {
    raw.flatMap(text => try Some(text.toDouble) catch { case _: NumberFormatException => None }) match {
      case Some(explicit) => (explicit, true)
      case None => (min + random.nextDouble() * (max - min), false)
    }
  }

  def formatValue(value: Double, isInt: Boolean): String =
    if (isInt) value.round.toLong.toString else value.toString

  /**
   * Game-end score row, Atlantis __tweaker.csv parity: header plus one row,
   * semicolon-delimited, vector columns in registration order. The fixed-name
   * file is overwritten every game (the batch copies it before the next one);
   * the dated copy stays as the archive. Only runs while tuning is active.
   */
  def finish(isWinner: Boolean): Unit = {
    if ( ! active) return
    try {
      val strategy = current.get
      val vector = appliedParams
      val fields = Seq(
        strategy.mapFile,
        if (isWinner) "W" else "L",
        Lifecycle.With.frame.toString,
        Information.Battles.KillLedger.killed.toString,
        Information.Battles.KillLedger.lost.toString,
        Information.Battles.KillLedger.killedResources.toString,
        Information.Battles.KillLedger.lostResources.toString,
        vectorDigest(vector))
      val header = (Seq("map", "result", "frames", "killed", "lost", "killedRes", "lostRes", "digest") ++ vector.keys.toSeq).mkString(";")
      val row = (fields ++ vector.values.toSeq).mkString(";")
      val content = header + "\n" + row + "\n"
      new java.io.File(Lifecycle.With.bwapiData.write + "/" + scoreDirectory).mkdirs()
      Lifecycle.With.bwapiData.writeToFile(scoreLastFile, content)
      val date = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date())
      Lifecycle.With.bwapiData.writeToFile(scoreDirectory + "/" + date + ".csv", content)
      System.out.println(f"[tuning] score: $row")
    } catch { case exception: Exception => Lifecycle.With.logger.onException(exception) }
  }

  def vectorDigest(vector: Map[String, String]): String = {
    val digest = java.security.MessageDigest.getInstance("MD5")
    val bytes = digest.digest(vector.toSeq.map(pair => pair._1 + "=" + pair._2).mkString(";").getBytes("UTF-8"))
    bytes.map(b => f"$b%02x").mkString
  }

  def applyParams(random: java.util.Random = new java.util.Random()): Unit = {
    applied.clear()
    current.foreach(strategy => applyParamsTo(strategy, random))
  }

  def applyParamsTo(strategy: TuningStrategy, random: java.util.Random): Unit = {
    strategy.params.foreach(param => {
      val (value, fromOverride) = resolveValue(tuneOverride(param.name), param.min, param.max, random)
      val text = formatValue(value, param.isInt)
      param.set(text)
      applied(param.name) = text
      System.out.println(f"[tuning] ${param.name} = $text (${if (fromOverride) "override" else "random"})")
    })
  }
}
