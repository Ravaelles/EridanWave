# Najdłuższe pliki: gdzie jest logika do podziału

Lista z `wc -l` po `src/**/*.scala` (bez testów), posortowana po długości.
Metody >20 linii mierzone po nawiasach/wcięciach — **liczą też zagnieżdżone
`def`-y i komentarze**, więc to sygnał, nie pomiar AST.

Kolumna „typ” mówi, czy plik warto dzielić:
- **logika** — realne decyzje/algorytmy, tu SRP ma sens,
- **dane** — tabele i listy, dzielenie tylko przenosi linie,
- **typ** — `Unit`, `UnitType`, `Tile` itp. trzymają spójny model i zostają.

| # | Plik | Linie | Typ | Metody >20 linii | Stan |
|---|------|-------|-----|------------------|------|
| 1 | `ProxyBwapi/UnitClasses/UnitClass.scala` | 854 | typ | listy 26–29 (dane) | **zrobione**: tabele per rasa |
| 2 | `Debugging/EridanSimViz.scala` | 725 | logika (UI) | `parseAllSims` 79, `reloadIfChanged` 78, `computeAutoZoomBounds` 50, `splitPW2Blocks*` 29/21, `readAppended` 25, `loadSimAt` 24, `loadMap` 21 | do zrobienia |
| 3 | `Micro/Formation/WallGaps.scala` | 644 | logika | `waypoint` 57, `allocate` 55, `updateGeometry` 43, `between` 42, `reinforcementCandidates` 38, `assign` 37, `candidates` 34, `route` 34, `centerUnpaired` 33 | do zrobienia (najwyższe ryzyko) |
| 4 | `ProxyBwapi/UnitInfo/UnitInfo.scala` | 635 | typ | `pixelToFireAt*` 171/170, `update` 37 | nie dzielimy klasy; patrz niżej |
| 5 | `Placement/Generation/TemplatesProtoss.scala` | 617 | dane | brak | — |
| 6 | `Placement/Walls/WallFinder.scala` | 566 | logika | `placeRequiredBuildings` 186, `tryPlace` 130, `findHallway` 50, `fill2x2Offset` 44, `fill2X2` 31, `tryFill` 29, `selectWallWithFiller` 26, `scoreWall` 24, `scoreFillerGeometry` 22 | **częściowo** (`canPlace`/`scoreWall` → `WallPlacement`/`WallScoring`) |
| 7 | `Macro/Scheduling/Simulation/MacroSim.scala` | 518 | logika | `trulyUnoccupied` 137, `simulate` 136, `tryInsertRequest` 90, `tryInsertAfter` 72, `constructStepsForRequest` 47, `ourSuppliers` 38, `updateStatesFrom` 37 | do zrobienia |
| 8 | `Tactic/Squads/defense/SquadDefendBase.scala` | 447 | logika | `recruitWorkers` 35, `zoneAndChokeFor` 33, `scour` 25, `run` 23, `guardWithoutTargets` 22, `defensePlan` 22, `perform` 21 | **zrobione**: `run` 84 → plan + 6 postaw |
| 9 | `Information/Battles/Prediction/Simulation/Grids/SimulationGrid.scala` | 440 | logika | `applySplash` 70, `sampleGlaveTarget` 66 | **częściowo** (splash → `SimulationSplash`) |
| 10 | `Tactic/Tactician.scala` | 407 | logika | `defendVsDrops` 26, `attachSCVs` 25, `enforceClusterSplit` 23, `runCoreTactics` 23 | **częściowo** (`fillAttackSquad` → 4 metody) |
| 11 | `Tactic/Missions/reaver/MissionReaverDrop.scala` | 402 | logika | `refreshItinerary` 50, `appendTargets` 49, `shuttleMustJoinArmy` 34, `recruit` 28, `confidentToRaid` 27 | do zrobienia |
| 12 | `Debugging/CombatVisSimLog.scala` | 401 | logika (UI) | `writeCompressedSimDumpIfNeededAsync` 29 | do zrobienia |
| 13 | `Debugging/SimVizView.scala` | 396 | logika (UI) | `currentFrameData` 263, `paintComponent` 261/88 | do zrobienia |
| 14 | `ProxyBwapi/Orders.scala` | 391 | typ | brak | — |
| 15 | `Mathematics/Points/Tile.scala` | 390 | typ | brak | — |
| 16 | `Placement/Cache/PlacementCacheFormat.scala` | 374 | logika | brak (po podziale) | **zrobione** |
| 17 | `Gameplans/Protoss/PvP/opening/PvPOpeningBuilds.scala` | 363 | dane | brak | — |
| 18 | `Gameplans/Protoss/PvP/PvPLateGame.scala` | 358 | logika | `skimEquivalent` 132, `executeBuild` 130, `executeMain` 46, `doTrainArmyHigh` 27 | do zrobienia |
| 19 | `Micro/Agency/CommanderWork.scala` | 357 | logika | `gather` (L64–L208, miara gubi się na zagnieżdżeniach), `build` | do zrobienia |
| 20 | `Micro/Formation/FormationStandard.scala` | 347 | logika | `arcSlots` 74, `slotsByClass` 43, `parameterize` 39, `overlookSlots` 24, `findGoalPath` 23, `findGoalRoute` 21 | do zrobienia |

## Klasy monolityczne — nie dzielimy

`UnitClass`, `UnitInfo`, `Orders`, `Tile`, `TemplatesProtoss`,
`PvPOpeningBuilds` to model danych albo typy domenowe. Ich metody >20 linii
są długie z powodu tabel (`pixelToFireAt` mapuje broń na geometrię,
`Orders` to lista stałych). Dzielenie ich na klasy „pomocnicze” rozbija
spójny model i nic nie zyskuje; jeśli kiedyś trzeba, to przez wyciągnięcie
**danych** do osobnego obiektu (`UnitClassTables`), nie przez pocięcie typu.

## Co realnie zostaje do podziału (kolejność)

1. **`WallFinder.placeRequiredBuildings` (186) + `tryPlace` (130).** To
   wyszukiwanie permutacji z deadline'em; da się wydzielić
   `WallPlacementSearch` (pętla + budżet czasu) i `WallPlacementTry`
   (jedna permutacja), a polityka już jest w `WallPlacement`/`WallScoring`.
2. **`MacroSim.simulate`/`trulyUnoccupied` (136/137).** Jedna klatka
   symulacji makro plus „czy miejsce jest wolne”. Podział na
   `MacroSimTick.advance(requests)` i `MacroSimOccupancy` z czystymi
   predykatami; test na occupancy jest tani.
3. **`EridanSimViz.parseAllSims`/`reloadIfChanged` (79/78) + `SimVizView`.**
   Debug UI: parsowanie plików i rysowanie klatek. Tu testów nie ma i nie
   będzie, ale podział na `SimLogFiles` (I/O) i `SimFramePainter` (rysowanie)
   czyni plik czytelnym.
4. **`MissionReaverDrop.refreshItinerary`/`appendTargets` (50/49).**
   Logika biznesowa misji; `appendTargets` to w istocie budowa trasy —
   wydzielić `ReaverItinerary` (czysta lista celów + koszty).
5. **`PvPLateGame.skimEquivalent`/`executeBuild` (132/130).**
   Dwa razy po 130 linii w jednym gameplanie; podział per faza
   (`PvPLateTech`, `PvPLateArmy`) tak jak zrobiono w `PvZ/reactive`.
6. **`CommanderWork.gather` (~145 linii w środku).** Wyciągnąć
   `GatherStep` (dojazd, wydobycie, powrót) — najczęściej wołana ścieżka
   mikro, więc warto, ale wymaga gry weryfikującej.
7. **`Tactician.defendVsDrops`/`attachSCVs`/`enforceClusterSplit`/
   `runCoreTactics` (23–26).** Krótkie, ale każda to inna decyzja;
   po jednej klasie albo przynajmniej po jednej metodzie z nazwą.

Próg >10 linii z prośby właściciela: po powyższych cięciach zostaną
metody 10–20 linii, głównie `get`/`have`-owe listy w gameplanach. Dla nich
kryterium jest inne: jeśli metoda miesza **decyzję** z **listą zakupów**,
dzielimy na `decide…` i `order…`; jeśli tylko wymienia zakupy, zostaje.
