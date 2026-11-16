package starengine

import org.scalatest.Tag

/**
 * Topic tags for StarEngine scenarios (one home for the vocabulary):
 * `scripts/starengine.sh --filter reaver` runs every scenario tagged
 * `Reaver`, `--filter reaver,shuttle` runs scenarios carrying either.
 * Tags mark TOPICS, not verdicts - one scenario usually proves both
 * the allow and the deny leg of its rule (a board suite shows when
 * the hull embarks and when it stays out). Suites gain tags
 * incrementally, next to the scenarios they describe; never invent a
 * tag without tagging a scenario with it.
 */
object StarEngineTags {

  /** The Reaver half of the pair: boarding, fighting and unloading it. */
  object Reaver extends Tag("starengine.reaver")

  /** The Shuttle half: fetching, escorting, fleeing and extracting. */
  object Shuttle extends Tag("starengine.shuttle")

  /** Embark discipline: when the hull boards and when it stays out. */
  object Board extends Tag("starengine.board")

  /** Drop discipline: when the ride unloads and when it holds cargo. */
  object Drop extends Tag("starengine.drop")
}
