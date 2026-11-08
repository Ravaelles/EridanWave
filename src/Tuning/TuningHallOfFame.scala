package Tuning

/**
 * Hall of fame: top entries by fitness, capped, one line per entry
 * (fitness|NAME=value,NAME=value). One file per (scenario, map) - rows carry
 * no map tag, so sharing a file across maps would mix surfaces the gradient
 * cannot separate. The loop keeps it inside its own OUTDIR.
 */
object TuningHallOfFame {

  case class Entry(fitness: Double, vector: Map[String, Double])

  val capacity: Int = 50

  def rank(entries: Seq[Entry]): Seq[Entry] =
    entries.sortBy(-_.fitness)

  def keep(entries: Seq[Entry]): Seq[Entry] =
    rank(entries).take(capacity)

  def offer(entries: Seq[Entry], fitness: Double, vector: Map[String, Double]): Seq[Entry] =
    keep(entries :+ Entry(fitness, vector))

  def load(text: String): Seq[Entry] = {
    text.linesIterator.toSeq.flatMap(line => {
      val bar = line.indexOf('|')
      if (bar < 0) None
      else {
        try {
          val fitness = line.substring(0, bar).toDouble
          val vector = line.substring(bar + 1).split(',').flatMap(pair => {
            val eq = pair.indexOf('=')
            if (eq < 0) None else Some(pair.substring(0, eq) -> pair.substring(eq + 1).toDouble)
          }).toMap
          Some(Entry(fitness, vector))
        } catch { case _: NumberFormatException => None }
      }
    })
  }

  def save(entries: Seq[Entry]): String = {
    rank(entries).map(e =>
      e.fitness.toString + "|" + e.vector.toSeq.map(pair => pair._1 + "=" + pair._2).mkString(",")).mkString("\n") + "\n"
  }
}
