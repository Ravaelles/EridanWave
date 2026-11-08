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
    val candidates = if (name.contains(".")) Seq(name) else Seq(strategiesPackage + name, name)
    candidates.view.flatMap(candidate => {
      try Some(Class.forName(candidate).getDeclaredConstructor().newInstance().asInstanceOf[TuningStrategy])
      catch { case _: Exception => None }
    }).headOption.getOrElse(throw new IllegalArgumentException(
      f"Unknown tuning strategy: $name (tried ${candidates.mkString(", ")})"))
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
