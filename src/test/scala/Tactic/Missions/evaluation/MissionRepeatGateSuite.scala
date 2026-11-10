package Tactic.Missions.evaluation

import org.scalatest.funsuite.AnyFunSuite

class MissionRepeatGateSuite extends AnyFunSuite {

  private def run(level: Int): MissionRepeatGate.PastRun =
    MissionRepeatGate.PastRun(level)

  test("verdict names map to levels") {
    assert(MissionRepeatGate.levelOf("Success") == 2)
    assert(MissionRepeatGate.levelOf("Unclear") == 1)
    assert(MissionRepeatGate.levelOf("Failure") == 0)
    assert(MissionRepeatGate.levelOf("garbage") == 0)
  }

  test("unknown verdicts drop the row instead of counting Failure") {
    assert(MissionRepeatGate.parseRow("123,map,enemy,MissionDTDrop,Bogus,200,500,80,true").isEmpty)
  }

  test("legacy nine-column rows parse, harass defaults away") {
    val row = MissionRepeatGate.parseRow("123,map,enemy,MissionDTDrop,Success,200,500,80,true")
    assert(row.exists(_._1 == "MissionDTDrop"))
    assert(row.exists(_._2 == MissionRepeatGate.PastRun(2)))
  }

  test("ten-column rows parse the same run") {
    val row = MissionRepeatGate.parseRow("123,map,enemy,MissionDTDrop,Failure,-50,100,80,false,900")
    assert(row.exists(_._2 == MissionRepeatGate.PastRun(0)))
  }

  test("short rows drop out, profit garbage survives on verdict") {
    assert(MissionRepeatGate.parseRow("a,b,c").isEmpty)
    assert(MissionRepeatGate.parseRow("1,2,3,4,Failure,NaN,7,8,9").exists(_._2 == MissionRepeatGate.PastRun(0)))
  }

  test("groupRows collects past launches per mission") {
    val grouped = MissionRepeatGate.groupRows(Vector(
      "123,map,enemy,MissionDTDrop,Success,200,500,80,true",
      "124,map,enemy,MissionDTDrop,Failure,-50,100,80,false",
      "125,map,enemy,MissionReaverDrop,Unclear,0,300,80,true"))
    assert(grouped.keySet == Set("MissionDTDrop", "MissionReaverDrop"))
    assert(grouped("MissionDTDrop") == Vector(MissionRepeatGate.PastRun(2), MissionRepeatGate.PastRun(0)))
    assert(MissionRepeatGate.averageLevel(grouped("MissionDTDrop")) == 1.0)
  }

  test("below three runs anything forms") {
    assert(MissionRepeatGate.repeatAllowed(Seq.empty))
    assert(MissionRepeatGate.repeatAllowed(Seq(run(0), run(0))))
  }

  test("three failures stand the mission down") {
    assert( ! MissionRepeatGate.repeatAllowed(Seq(run(0), run(0), run(0))))
  }

  test("unclear average keeps forming") {
    assert(MissionRepeatGate.repeatAllowed(Seq(run(0), run(1), run(2))))
    assert(MissionRepeatGate.repeatAllowed(Seq(run(2), run(2), run(2))))
  }
}
