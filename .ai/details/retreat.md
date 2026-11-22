# Retreat doctrine (evidence + live read guide)

Mechanics (both phases read one permission):

- Overrule: own squad votes Fight at >= 1.5 skim drops the personal
  verdict-bus hold and the technique latch, both phases.
- Reason rows: `Retreating(<rule> <own>vs<foe>)` on march labels AND
  RetreatPlan acts. `Yes`/`No` = FightOrFlee sim verdict, not a bug.
- Floor: withdrawal 96 -> 48f. Dying hulls (hp <= 35) and known hard
  counters keep the old Flee.
- Lonely gate (`RetreatVerdict.lonelyTiles` = 12.0): no enemy warrior
  inside 12 tiles kills the whole retreat vote, both phases.
- Technique terminal reuses the same `RetreatAllowed.forUnit` the march
  reads, so both phases agree by construction; without permission the
  frame falls to Walk. ASU >= 250 attacks without asking the sim
  (`attackUnitsScore`); lurkers, DTs, carriers and siege keep the vote.

Open band question (rule 18): entry 12 / release 15 proposed, currently
a single 12 edge. Needs a game flapping on the edge before coding it.
