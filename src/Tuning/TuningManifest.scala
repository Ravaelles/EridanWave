package Tuning

/**
 * Batch manifest rows: game id, verdict, kills, losses, frames, param digest
 * and the played vector (NAME=value,NAME=value). Mirrors the manifest.txt
 * that run-batch.sh writes, so the loop can rank games without re-walking
 * per-game dirs.
 */
object TuningManifest {

  case class Game(id: String, win: Boolean, killed: Int, lost: Int, frames: Int, digest: String, vector: Map[String, Double])

  def parseGames(manifest: String): Seq[Game] = {
    manifest.linesIterator.toSeq.flatMap(line => {
      val parts = line.split('|')
      if (parts.length != 7 || ! parts(0).startsWith("game-")) None
      else {
        try {
          val vector = parts(6).split(',').flatMap(pair => {
            val eq = pair.indexOf('=')
            if (eq < 0) None else Some(pair.substring(0, eq) -> pair.substring(eq + 1).toDouble)
          }).toMap
          Some(Game(parts(0), parts(1) == "WIN", parts(2).toInt, parts(3).toInt, parts(4).toInt, parts(5), vector))
        } catch { case _: NumberFormatException => None }
      }
    })
  }
}
