# Retreat doctrine (evidence + live read guide)

Mechanics (all in jar since 16:39 clean build):

- Overrule: own squad votes Fight at >= 1.5 skim drops the personal
  verdict-bus hold and the technique latch, both phases.
- Reason rows: `Retreating(<rule> <own>vs<foe>)` on march labels AND
  RetreatPlan acts (shares in tens). `Yes`/`No` = FightOrFlee sim
  verdict, not a bug.
- Floor: withdrawal 96 -> 48f.
- Lonely gate (`RetreatVerdict.lonelyTiles = 12.0`): no enemy warrior
  inside 12 tiles kills the whole retreat vote, both phases. Callers
  pass the measured distance; unmeasured call sites keep the old
  behavior. Dying hulls keep their way out by design.

Origin: BetaStar f8979/f8224 - strong line (ASU 185+/394 skim)
routing with no foe nearby, Retreat/Retreating flaps. Open band
question (rule 17): entry 12 / release 15 proposed, currently a
single 12 edge.

Live reads:

- GAME_C1507F1B (stale jar, no lonely): DEFEAT 69/175, 3187 paren
  rows. Top: OutOfContact 60vs40, No 60vs40, OutmatchedGoon.
- GAME_8222603D (jar WITH lonely): DEFEAT 42/147, 159 paren rows.
  Deaths spike f15600+ while retreat rows stop ~f13200; late deaths
  end on bare `Retreat` (technique arm). Open: fought-to-death by
  design (overrule) or outmassed? Needs Crona/Terminus before
  judging lonely.
