package Micro.Actions.Combat.Decisionmaking

import FormationApproach._
import org.scalatest.funsuite.AnyFunSuite

class FormationApproachSuite extends AnyFunSuite {

  private val evenLine  = Some(LineStats(meanTiles = 3.0, stddevTiles = 1.0, count = 8))
  private val tunedLine = Some(LineStats(meanTiles = 3.0, stddevTiles = 0.2, count = 8))

  test("marches freely with no known enemy") {
    assert(decide(ownBeyondTiles = 3.0, stats = None, posture = Cautious) == FreeMarch)
  }

  test("marches freely while the line averages beyond the dress band") {
    val far = Some(LineStats(meanTiles = 6.0, stddevTiles = 1.0, count = 8))
    assert(decide(ownBeyondTiles = 6.0, stats = far, posture = Cautious) == FreeMarch)
  }

  test("steps back only when protruding past tolerance") {
    assert(decide(ownBeyondTiles = 3.5, stats = evenLine, posture = Cautious) == StepBack)
    assert(decide(ownBeyondTiles = 3.4, stats = evenLine, posture = Cautious) == HoldLine)
  }

  test("marches on when lagging past tolerance, holds inside it") {
    assert(decide(ownBeyondTiles = 2.5, stats = evenLine, posture = Cautious) == StepForward)
    assert(decide(ownBeyondTiles = 2.7, stats = evenLine, posture = Cautious) == HoldLine)
  }

  test("even line closes together, style depends on enemy race posture") {
    assert(decide(ownBeyondTiles = 3.0, stats = tunedLine, posture = Cautious) == CloseCautious)
    assert(decide(ownBeyondTiles = 3.0, stats = tunedLine, posture = Decisive) == BreakThrough)
  }

  test("line statistics average member distances beyond their own range") {
    val stats = lineStats(Seq(2.0, 4.0))
    assert(stats.exists(s => s.meanTiles == 3.0 && s.stddevTiles == 1.0 && s.count == 2))
  }

  test("no measured members means no statistics") {
    assert(lineStats(Seq.empty) == None)
    assert(lineStats(Seq(3.0)).exists(s => s.meanTiles == 3.0 && s.stddevTiles == 0.0 && s.count == 1))
  }
}
