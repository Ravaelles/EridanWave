package starengine

import org.scalatest.{Suite, Tag}

/**
 * Marker for every StarEngine scenario: the default suite excludes this
 * tag, so engine scenarios run ad hoc only (singly via -Dsuites or all
 * via scripts/starengine.sh), never as part of the routine build.
 * Mix into every scenario suite: `class XSuite extends AnyFunSuite
 * with StarEngineSuite`.
 */
object StarEngineSlow extends Tag("starengine.Slow")

trait StarEngineSuite extends Suite {

  abstract override def tags: Map[String, Set[String]] = {
    val base = super.tags
    testNames.map(name => name -> (base.getOrElse(name, Set.empty[String]) + StarEngineSlow.name)).toMap
  }
}
