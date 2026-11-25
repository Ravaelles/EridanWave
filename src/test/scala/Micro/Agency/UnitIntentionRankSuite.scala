package Micro.Agency

import org.scalatest.funsuite.AnyFunSuite

class UnitIntentionRankSuite extends AnyFunSuite {

  test("survival outranks every other reason") {
    assert(UnitIntentionRank.rank(UnitIntentionReason.Survival) > UnitIntentionRank.rank(UnitIntentionReason.Dodge))
    assert(UnitIntentionRank.rank(UnitIntentionReason.Survival) > UnitIntentionRank.rank(UnitIntentionReason.Duel))
    assert(UnitIntentionRank.rank(UnitIntentionReason.Survival) > UnitIntentionRank.rank(UnitIntentionReason.Formation))
    assert(UnitIntentionRank.rank(UnitIntentionReason.Survival) > UnitIntentionRank.rank(UnitIntentionReason.March))
    assert(UnitIntentionRank.rank(UnitIntentionReason.Survival) > UnitIntentionRank.rank(UnitIntentionReason.Macro))
  }

  test("dodge outranks the fight but not survival") {
    assert(UnitIntentionRank.rank(UnitIntentionReason.Dodge) > UnitIntentionRank.rank(UnitIntentionReason.Duel))
    assert(UnitIntentionRank.rank(UnitIntentionReason.Dodge) > UnitIntentionRank.rank(UnitIntentionReason.Formation))
    assert(UnitIntentionRank.rank(UnitIntentionReason.Dodge) > UnitIntentionRank.rank(UnitIntentionReason.March))
  }

  test("the duel outranks the line and the march") {
    assert(UnitIntentionRank.rank(UnitIntentionReason.Duel) > UnitIntentionRank.rank(UnitIntentionReason.Formation))
    assert(UnitIntentionRank.rank(UnitIntentionReason.Duel) > UnitIntentionRank.rank(UnitIntentionReason.March))
  }

  test("the line outranks the march, the march outranks upkeep") {
    assert(UnitIntentionRank.rank(UnitIntentionReason.Formation) > UnitIntentionRank.rank(UnitIntentionReason.March))
    assert(UnitIntentionRank.rank(UnitIntentionReason.March) > UnitIntentionRank.rank(UnitIntentionReason.Macro))
  }

  test("the outranked key names loser-vs-winner at the class") {
    assert(UnitIntentionRank.outrankedKey(UnitIntentionReason.March, UnitIntentionReason.Duel, "Dragoon") ==
      "outrankedBid.MarchVsDuel.at.Dragoon")
  }

  test("the stood-down key names the direct winner at the class") {
    assert(UnitIntentionRank.stoodDownKey("CommanderMove.move", "Zealot") ==
      "bidStoodDown.CommanderMove.move.at.Zealot")
  }
}
