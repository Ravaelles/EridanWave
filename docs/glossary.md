# Słowniczek pojęć PurpleWave / EridanWave

Krótka ściąga z konceptów używanych w kodzie. Przy każdym pojęciu jest
wskazane, gdzie żyje w kodzie.

## Strategy (strategia)

Deklaracja *co gramy*, nie *jak*. Pojedynczy obiekt, np. `PvTZZCoreZ`
(opening) albo `PvTArbiter` (kompozycja końcowa). Niesie ze sobą: przeciw
jakim rasom obowiązuje, jakie odgałęzienia otwiera (`addChoice`), jakie
ślady wroga ją skreślają (`blacklistVs`/`whitelistVs`), wymagania aktywacji
(np. „mamy RoboticsFacility") oraz opcjonalnie własny gameplan.
Kod: `src/Strategery/Strategies/Strategy.scala`, konkretne strategie w
`src/Strategery/Strategies/Protoss/`, `Terran/`, `Zerg/`.

## StrategyBranch (gałąź strategii)

Łańcuch strategii wybrany na start meczu, np. `PvTZZCoreZ + PvTArbiter`:
opener plus kontynuacja. Gałąź wybiera raz na grę polityka selekcji
(`StrategySelectionPolicy`) na podstawie historii gier. W trakcie gry gałąź
może się zmieniać tylko przez `swapIn`/`swapOut` pojedynczych strategii.
Kod: `src/Strategery/Strategies/StrategyBranch.scala`.

## Opening

Strategia otwierająca — ta, która ma `choices` (otwiera dalsze opcje), np.
`PvTZZCoreZ`, `PvPGateCore`. Potocznie też pierwsze minuty gry i budowany
wtedy build order. W logu startowym (`Strategist.logOpening`) openinery
lądują w wierszu `Opening`.

## Gameplan

Wykonawca: *jak* gramy, klatka po klatce. Wystawia rozkazy makro (budynki,
jednostki, gaz) i intencje taktyczne (atak, ekspansja, harass). Cykl życia:
`activated` (czy wchodzi do gry), `executeBuild` (build order),
`executeMain` (gra główna), `completed` (czy już oddaje pole).
Bazowa klasa: `GameplanImperative`
(`src/Gameplans/All/GameplanImperative.scala`). Przykłady: `PvPOpening`
(otwarcie), `PvPLateGame` (późna gra), `PvZFE` (ekspansja).

## ModalGameplan i StandardGameplan

Przełączniki między gameplanami. `ModalGameplan` trzyma listę gameplanów i
każdą klatkę odpala pierwszy aktywny (np. `ProtossVsProtoss`: najpierw
nasze cannon-rushe, potem `PvPOpening`, na końcu `PvPLateGame`).
`StandardGameplan` wybiera modal dopasowany do naszej rasy.
Kod: `src/Gameplans/All/ModalGameplan.scala`,
`src/Gameplans/All/StandardGameplan.scala`.

## Gameplan Tech (PrimaryTech)

Tak, istnieje: wybór gałęzi technologicznej *wewnątrz* gameplanu.
W `PvPLateGame` (`src/Gameplans/Protoss/PvP/PvPLateGame.scala:19-66`)
to rozstrzygnięcie `RoboTech` kontra `TemplarTech` — raz podjęte, ciągnie
za sobą całą dalszą produkcję (Reavery/Obsy albo DT/Storm). W logu
startowym kompozycje końcowe (np. `PvTArbiter`) lądują w wierszu `Tech`.

## Tactic (taktyka)

Zachowanie armii na mapie: gdzie stać, co scoutować, kiedy się wycofać.
Żyje w `src/Tactic/` pod `Tacticianem`, np. `TacticScoutForCannonRush`
(wczesny zwiad pod cannon rusha), `MaximumCannonRush` (nasz rush).
Taktyka nie buduje — ona ustawia intencje jednostkom.

## Squad i Unit (mikro)

`Squad` to grupa jednostek o wspólnym zadaniu (atak, obrona, drop),
`Unit` to pojedyncza jednostka z łańcuchem roszczeń (claims-chain):
kto nią dowodzi i co ma robić. Szczegóły w `.ai/architecture.md`.

## Fingerprint (ślad)

Rozpoznany wzorzec gry wroga, np. `cannonRush` (proxowany Cannon przed
4:30), `twoGate`, `forgeFe`. Fingerprinty napędzają reakcje gameplanów
(przełączenia strategii) i zwiad. Kod: `src/Information/Fingerprinting/`.

## Playbook i polityka selekcji

`Playbook` to książka wszystkich legalnych gałęzi, a
`StrategySelectionPolicy` to sposób wyboru jednej z nich (zachłannie,
turniejowo, losowo). Kod: `src/Strategery/Playbook.scala`,
`src/Strategery/Selection/Policies/`.

## MacroActions: get / once / pump

Czasowniki rozkazów makro w gameplanach: `get` (chcę N sztuk),
`once` (jednorazowo, z deduplikacją), `pump` (produkuj w kółko).
Kod: `src/Macro/Actions/MacroActions.scala`.

## Blackboard

Współdzielona tablica stanu między podsystemami (flagi i wskaźniki),
np. `cannonRushBase`, `wantToAttack`. Kod: `src/Planning/Blackboard.scala`.

## Jak to się składa w całość

1. Start meczu: polityka selekcji wybiera `StrategyBranch` (log `OPENING`).
2. `Strategist.gameplan` znajduje gameplan (własny albo standardowy).
3. `ModalGameplan` odpala pierwszy aktywny gameplan z listy.
4. Gameplan wystawia `get/once/pump` (makro) oraz `attack/harass`
   (taktka); `Tactician` zamienia intencje w rozkazy squadów.
5. Fingerprinty wroga mogą w trakcie gry doswapować strategie
   (`swapIn`/`swapOut`), a gameplan przełącza sekwencje (np. `GCTech`
   po wykryciu cannon rusha).
