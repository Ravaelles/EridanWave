# Kite doctrine (evidence + live read guide)

- Ranged: only the dying yield (below 61 total HP while cooling past
  10, `KiteAway.kiteVsRangedHp`); everything else holds its volleys.
  Mild-wound rotation deleted - in even mirrors it donated volleys
  (StarEngine: focus-kite 12 kills vs focus-plant 14 down two guns).
- Melee: kites slow lings; speedlings only as an intact pack
  (`MeleeKiteAway.packIntact`, seam reads nearest zealot mate) - alone
  a blade stands. 2Zeal parked hard (upgraded 2v6, 1W/7L): the ceiling
  without a melee shared pick (declined).
- Far dress-and-wait (`MarchDress.holdFarWait`: fires at five out,
  holds through the creep, releases on contact or past six; warriors
  only, UMS mirrors, never under reavers) outranks memory duels; race
  read off seen units.
- UMS endgame (`NoRetreat.umsEndgame`): past ten seconds a unit with
  nothing in range holds instead of withdrawing - duels keep the skim
  vote.
- Death log (`DeathWatch.watches`): micro maps dump frame, unit and
  last orders on every warrior death; first-dragoon-dead frame judges
  kite and dress on 14v16.
- 1DragWounded regressed post-audit (was ~50/50, now losing with k0):
  open item with evidence in the queue; the kite kernel itself still
  pins the doctrine in `WoundedDuelSuite`.
