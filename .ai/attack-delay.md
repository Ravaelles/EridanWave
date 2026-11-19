# Attack delay (why the code holds orders after a shot)

Units stick when a move order lands right after the attack animation.
Withhold the follow-up until its projected arrival clears
(attack start frame + stop frames + 2). The +2 is the empirical
margin: consecutive shots otherwise miss. The implementation keeps
this rule and its numbers; this file keeps the history.

## Sources

- Dragoon iScript (`DragoonGndAttkInit/Rpt`): attack frame lands
  ~8 frames after the order executes; switching to move at frame 9
  is safe.
- jaj22 (stuck-goon discovery): goons stick only continuing the same
  direction after the shot; 9-minus-latency after isStartingAttack
  is the optimal move-order time; opposite-direction orders unstick
  (<45 degrees still sticks).
- JohnJ: leave the unit alone for 8 frames after isStartingAttack;
  (now + 9 - frames before execution) is the exact timing, latency
  aside. The fallback keeps the +2 margin on top.
- Consequence: reaching the earliest safe frame needs micro running
  on *every* frame.

Full archaeology (fallout timelines, 2017 chats, spreadsheet):
git history of `src/Micro/Agency/AttackDelay.scala`.
