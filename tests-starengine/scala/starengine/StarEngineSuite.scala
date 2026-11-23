package starengine

import org.scalatest.{Outcome, Tag, TestSuite}

/**
 * Marker for every StarEngine scenario: the default suite excludes this
 * tag, so engine scenarios run ad hoc only (singly via -Dsuites or all
 * via scripts/starengine.sh), never as part of the routine build.
 * Mix into every scenario suite: `class XSuite extends AnyFunSuite
 * with StarEngineSuite`.
 */
object StarEngineSlow extends Tag("starengine.Slow")

trait StarEngineSuite extends TestSuite {

  abstract override def tags: Map[String, Set[String]] = {
    val base = super.tags
    testNames.map(name => name -> (base.getOrElse(name, Set.empty[String]) + StarEngineSlow.name)).toMap
  }

  /**
   * Names the live game for the visualizer: with STARENGINE_VISUALIZE=1
   * the window retitles to suite.test as scenarios run one after another.
   * A no-op string write when visualization is off.
   */
  abstract override def withFixture(test: NoArgTest): Outcome = {
    Visualizer.testStarting(suiteName, test.name)
    super.withFixture(test)
  }
}
