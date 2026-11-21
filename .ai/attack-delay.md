# Attack delay (why the code holds orders after a shot)

Withhold the follow-up until its projected arrival clears (attack start
frame + stop frames + 2). The +2 is the empirical margin: consecutive
shots otherwise miss. Dragoon iScript puts the safe move at ~frame 9
after the order executes; jaj22 and JohnJ converge on 8-9 frames after
isStartingAttack, latency aside. Consequence: micro must run on every
frame to hit the earliest safe window.

Full archaeology (fallout timelines, 2017 chats, spreadsheet): git
history of `src/Micro/Agency/AttackDelay.scala`.
