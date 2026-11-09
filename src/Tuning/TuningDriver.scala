package Tuning

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

/**
 * Thin CLI over the loop (Atlantis TunerDriver parity): reads the hall of
 * fame plus the batch manifest, saves the archive, and writes the next
 * per-game override fragment (NAME=value lines the batch feeds back as
 * --tune-param flags). All decisions live in TuningProposer/HallOfFame
 * (tested); this class only moves files and parses flags.
 *
 * Usage: TuningDriver --strategy NAME --hof HOF --manifest MANIFEST
 *   --out FRAGMENT [--radius D] [--seed N]
 *
 * Stdout carries one machine line, BEST=<fitness>, for the loop schedule.
 */
object TuningDriver {

  def main(args: Array[String]): Unit = {
    val flags = parse(args)
    val strategyName = flags.getOrElse("--strategy", sys.error("missing --strategy NAME"))
    val hofPath = Paths.get(flags.getOrElse("--hof", sys.error("missing --hof HOF")))
    val manifest = new String(Files.readAllBytes(Paths.get(flags.getOrElse("--manifest", sys.error("missing --manifest MANIFEST")))), StandardCharsets.UTF_8)
    val outPath = Paths.get(flags.getOrElse("--out", sys.error("missing --out FRAGMENT")))
    val radius = flags.get("--radius").map(_.toDouble).getOrElse(0.10)
    val seed = flags.get("--seed").map(_.toLong).getOrElse(System.nanoTime())

    val strategy = Tuning.resolve(strategyName)
    var hallOfFame = if (Files.exists(hofPath)) TuningHallOfFame.load(new String(Files.readAllBytes(hofPath), StandardCharsets.UTF_8)) else Seq.empty
    TuningManifest.parseGames(manifest).foreach(game =>
      hallOfFame = TuningHallOfFame.offer(hallOfFame, strategy.fitness(game.win, game.killed, game.lost, game.frames), game.vector))
    Files.write(hofPath, TuningHallOfFame.save(hallOfFame).getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

    val proposal = TuningProposer.propose(strategy, hallOfFame, radius, new java.util.Random(seed))
    if (proposal.isEmpty) {
      System.out.println("BEST=unknown (cold start, no rows yet)")
      System.out.println("no proposal: archive empty, batch randomizes")

      return
    }

    if (outPath.getParent != null) Files.createDirectories(outPath.getParent)
    Files.write(outPath, TuningProposer.formatFragment(proposal, strategy).getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

    val ranked = TuningHallOfFame.rank(hallOfFame)
    System.out.println(f"BEST=${ranked.head.fitness}%.4f")
    System.out.println("RADIUS=" + radius)
    System.out.println(f"champion: fitness=${ranked.head.fitness}%.4f")
    System.out.println("proposal: " + proposal.size + " params -> " + outPath)
  }

  private def parse(args: Array[String]): Map[String, String] = {
    args.toSeq.flatMap(arg => {
      val eq = arg.indexOf('=')
      if (arg.startsWith("--") && eq > 2) Some(arg.substring(0, eq) -> arg.substring(eq + 1)) else None
    }).toMap
  }
}

