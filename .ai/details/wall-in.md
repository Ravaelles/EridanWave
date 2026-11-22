# Wall-in doctrine (PvT)

- Depot-only: WallBreachTarget, EnemyWallInDetected, BreakTerranWallIn
  chew the Depot while the wall stands. Barracks duels re-point to a
  depot. DTs are never claimed by the breach (own brain). The SCV
  repairing the depot dies first. A lifted piece means the lane is
  open: no wall-in.
- Outgunned ban (latched): below 5 dragoons per sieged tank the
  wall-in is forbidden for the rest of the game. Release at supply
  >= 195 or minerals >= 1200; re-arm at supply <= 165.
- Melee: no slots inside 8 tiles of a building, blades never inside
  4 of an enemy. A blade on a sieged tank (1.1) never withdraws and
  the leash never yanks it off. Dress veto covers the pose leg only;
  rally and join still reform.
- Live check: depot chew + SCV snipe + zero Rax picks + no dress
  flap at the breach.

## PvP mission (212f964, needs a live PvP wall-in)

- Detect: 3+ Protoss buildings inside 4 tiles of the enemy natural
  choke, at or before minute 9. Latched: the mission holds past the
  entry while 2 stand. Release: 1 or fewer left, or minute 9 over.
  No enemy natural (baseless UMS) means no mission, ever.
- Dragoons only: chew the building nearest the cluster centroid
  (one shared pick, no re-pick flap). Zealots are not claimed -
  blades have no business chewing into cannons. DTs keep their brain.
- Wounded hulls hold off: withdraw at 15 wound (shields plus hull),
  back at 13, or everyone fights at supply >= 180 / minerals >= 1000.
  Holding hulls act `ProtossWallHold` and let normal logic own them.
- Hold discipline (owner order, 2026-11): holders stand only unpressured
  and shielded, at most nine tiles out. Mobile guns on top or shields
  below 20 lift the mission - the hull retreats instead of donating
  itself under the wall (static fire alone never lifts it: grinding
  under it is the job). A held retreat verdict outranks the mission
  outright - the wall never outvotes a retreat. Out of nine tiles the
  march owns the approach instead of chewing across the map.
- Chain order in `acquireTarget`: Terran wall-in, then Protoss, then
  the generic breach fallback. No new movement writer (attack and
  hold are both exempt legs).
