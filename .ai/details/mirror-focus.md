# Mirror focus + plant (StarEngine crossed, d274575c6)

- Focus (shared weakest target) beats spread (nearest-first),
  both seats: dragoons 5 kills vs 4; zealots hold too.
  Spread-vs-spread bleeds both hulls (control row).
- Plant (hold inside gun range on the shared weakest) beats milling
  crossed: 6 kills vs 1. Plant vs kite ties crossed (3 vs 3):
  never yields ground, never trades down.
- Shipped gate stays shut below 4 shooters
  (`FocusFire.minShooters = 4`, src/Micro/Targeting/FocusFire.scala):
  3v3 lines duel even, no shared target.

OWNER DECISION taken: gate lowered 4 -> 3 everywhere (b5233fb43),
`enoughShooters` pins the edge (2 mills, 3 shares). Validating live on
14/15/16Drag + ZealDrag. If small lines still spread, the next suspect
is reach (wings out of shared-target range), not the count.
