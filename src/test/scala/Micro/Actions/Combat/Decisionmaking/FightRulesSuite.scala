package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class FightRulesSuite extends AnyFunSuite {

  private val off = FightFacts(
    pacifist = false, cantFight = false, cantFlee = false, static = false,
    committed = false, yolo = false, irradiated = false, safe = false,
    detonated = false, hugTank = false, archon = false, coverFire = false,
    berzerk = false, cloaked = false, cloakable = false, lurking = false,
    towers = false, scourge = false, scarabless = false, birdless = false,
    noEnergy = false, disrupted = false, bideSiege = false, carrierVuln = false,
    raze = false, miners = false, reave = false, energized = false)

  test("every rule fires in isolation with its verdict and label") {
    val cases = Seq(
      (off.copy(pacifist = true),    (false, "Pacifist")),
      (off.copy(cantFight = true),   (false, "CantFight")),
      (off.copy(cantFlee = true),    (true,  "CantFlee")),
      (off.copy(static = true),      (true,  "Static")),
      (off.copy(committed = true),   (true,  "Committed")),
      (off.copy(yolo = true),        (true,  "YOLO")),
      (off.copy(irradiated = true),  (true,  "Irradiated")),
      (off.copy(safe = true),        (true,  "Safe")),
      (off.copy(outOfContact = true), (false, "OutOfContact")),
      (off.copy(detonated = true),   (true,  "Detonated")),
      (off.copy(hugTank = true),     (true,  "HugTank")),
      (off.copy(archon = true),      (true,  "Archon")),
      (off.copy(coverFire = true),   (true,  "CoverFire")),
      (off.copy(berzerk = true),     (true,  "Berzerk")),
      (off.copy(cloaked = true),     (true,  "Cloaked")),
      (off.copy(cloakable = true),   (true,  "Cloakable")),
      (off.copy(lurking = true),     (true,  "Lurking")),
      (off.copy(towers = true),      (true,  "Towers")),
      (off.copy(scourge = true),     (false, "Scourge")),
      (off.copy(scarabless = true),  (false, "Scarabless")),
      (off.copy(birdless = true),    (false, "Birdless")),
      (off.copy(noEnergy = true),    (false, "NoEnergy")),
      (off.copy(disrupted = true),   (false, "Disrupted")),
      (off.copy(bideSiege = true),   (false, "BideSiege")),
      (off.copy(carrierVuln = true), (false, "CarrierVuln")),
      (off.copy(raze = true),        (true,  "Raze")),
      (off.copy(miners = true),      (true,  "Miners")),
      (off.copy(reave = true),       (true,  "Reave")),
      (off.copy(energized = true),   (true,  "Energized")))
    assert(cases.size == 29)
    cases.foreach { case (facts, expected) =>
      assert(FightRules.decide(facts).contains(expected), expected)
    }
  }

  test("pacifist outranks everything, including other refuse-to-fight rules") {
    val facts = off.copy(pacifist = true, cantFlee = true, committed = true, yolo = true, scourge = true)
    assert(FightRules.decide(facts).contains((false, "Pacifist")))
  }

  test("refusals keep their historical order: cantFight, then cantFlee, then static") {
    assert(FightRules.decide(off.copy(cantFight = true, cantFlee = true, static = true)).contains((false, "CantFight")))
    assert(FightRules.decide(off.copy(cantFlee = true, static = true, committed = true)).contains((true, "CantFlee")))
  }

  test("commitment outranks enthusiasm: committed before YOLO, YOLO before reasons to stay") {
    assert(FightRules.decide(off.copy(committed = true, yolo = true)).contains((true, "Committed")))
    assert(FightRules.decide(off.copy(yolo = true, safe = true)).contains((true, "YOLO")))
  }

  test("the false-block keeps its order: scourge before casters, casters before siege") {
    assert(FightRules.decide(off.copy(scourge = true, noEnergy = true)).contains((false, "Scourge")))
    assert(FightRules.decide(off.copy(noEnergy = true, bideSiege = true)).contains((false, "NoEnergy")))
    assert(FightRules.decide(off.copy(bideSiege = true, carrierVuln = true)).contains((false, "BideSiege")))
  }

  test("siege-breaking outranks economy defense outranks the reaver outranks the battery") {
    assert(FightRules.decide(off.copy(raze = true, miners = true)).contains((true, "Raze")))
    assert(FightRules.decide(off.copy(miners = true, reave = true)).contains((true, "Miners")))
    assert(FightRules.decide(off.copy(reave = true, energized = true)).contains((true, "Reave")))
  }

  test("lost contact outranks combat but not safety: reform, unless nothing can hurt us") {
    assert(FightRules.decide(off.copy(outOfContact = true, hugTank = true)).contains((false, "OutOfContact")))
    assert(FightRules.decide(off.copy(safe = true, outOfContact = true)).contains((true, "Safe")))
  }

  test("silence means no rule fired and the estimator decides instead") {
    assert(FightRules.decide(off).isEmpty)
  }
}
