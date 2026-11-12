# 20 najdłuższych plików: metody >20 linii

Lista z `wc -l` po `src/**/*.scala` (bez testów) i przegląd metod dłuższych niż
20 linii. Długości są mierzone po nawiasach/wcięciach, więc **liczą też
zagnieżdżone `def`-y i komentarze**, a przy gęstych blokach potrafią się
pomylić — traktować jako sygnał, nie pomiar AST. Kolumna „stan” mówi, co już
zrobione, a co zostaje.

| # | Plik | Linie | Metody >20 linii | Stan |
|---|------|-------|------------------|------|
| 1 | `ProxyBwapi/UnitClasses/UnitClass.scala` | 854 | ~~`_buildUnitsEnabling` 76~~, ~~`_buildUnitsBorrowed` 49~~, ~~`_buildUnitsSpent` 48~~; zostały listy 26–29 linii (dane + komentarze) | **zrobione**: `_enabling*`, `_borrowed*`, `_spent*` per rasa |
| 2 | `Debugging/EridanSimViz.scala` | 725 | `parseAllSims` 79, `reloadIfChanged` 78, `computeAutoZoomBounds` 50, `splitPW2BlocksWithRemainder` 29, `readAppended` 25, `loadSimAt` 24, `splitPW2Blocks` 21, `loadMap` 21 | do zrobienia |
| 3 | `Micro/Formation/WallGaps.scala` | 644 | `waypoint` 57, `allocate` 55, `updateGeometry` 43, `between` 42, `reinforcementCandidates` 38, `assign` 37, `candidates` 34, `route` 34, `centerUnpaired` 33 | do zrobienia (wysokie ryzyko) |
| 4 | `ProxyBwapi/UnitInfo/UnitInfo.scala` | 635 | `pixelToFireAtExhaustive` 171, `pixelToFireAt` 170, `update` 37 | do zrobienia |
| 5 | `Placement/Generation/TemplatesProtoss.scala` | 617 | brak (dane) | — |
| 6 | `Placement/Walls/WallFinder.scala` | 607 | `placeRequiredBuildings` 186, `tryPlace` 130, `findHallway` 50, `fill2x2Offset` 44, `canPlace` 44, `scoreWall` 41, `fill2X2` 31, `tryFill` 29, `selectWallWithFiller` 26, `scoreFillerGeometry` 22 | do zrobienia |
| 7 | `Macro/Scheduling/Simulation/MacroSim.scala` | 518 | `trulyUnoccupied` 137, `simulate` 136, `tryInsertRequest` 90, `tryInsertAfter` 72, `constructStepsForRequest` 47, `ourSuppliers` 38, `updateStatesFrom` 37 | do zrobienia |
| 8 | `Gameplans/Protoss/PvZ/PvZ1BaseReactive.scala` | 469 | `buildTech` 79, `buildArmy` 65, `ensureCompositionTech` 63, `executeMain` 60, `chooseOpening` 39, `goo` 33, `chooseComposition` 31, `executeBuild` 26 | do zrobienia |
| 9 | `Information/Battles/Prediction/Simulation/Grids/SimulationGrid.scala` | 445 | `applySplash` 75, `sampleGlaveTarget` 66 | do zrobienia |
| 10 | `Tactic/Tactician.scala` | 407 | ~~`fillAttackSquad` 44~~; zostały `defendVsDrops` 26, `attachSCVs` 25, `enforceClusterSplit` 23, `runCoreTactics` 23 | **częściowo** |
| 11 | `Tactic/Squads/defense/SquadDefendBase.scala` | 402 | `run` 84, `recruitWorkers` 35, `zoneAndChokeFor` 33, `scour` 25, `guardWithoutTargets` 22, `perform` 21 | do zrobienia |
| 12 | `Tactic/Missions/reaver/MissionReaverDrop.scala` | 402 | `refreshItinerary` 50, `appendTargets` 49, `shuttleMustJoinArmy` 34, `recruit` 28, `confidentToRaid` 27 | do zrobienia |
| 13 | `Debugging/CombatVisSimLog.scala` | 401 | `writeCompressedSimDumpIfNeededAsync` 29 | do zrobienia |
| 14 | `Debugging/SimVizView.scala` | 396 | `currentFrameData` 263, `paintComponent` 261 (klasa wewnętrzna) i 88 | do zrobienia |
| 15 | `ProxyBwapi/Orders.scala` | 391 | brak | — |
| 16 | `Mathematics/Points/Tile.scala` | 390 | brak | — |
| 17 | `Placement/Cache/PlacementCacheFormat.scala` | 374 | ~~`writeWall` 22~~ | **zrobione**: `writeConstraint`/`writeBuildings`/`writeHallway` |
| 18 | `Gameplans/Protoss/PvP/opening/PvPOpeningBuilds.scala` | 363 | brak (dane) | — |
| 19 | `Gameplans/Protoss/PvP/PvPLateGame.scala` | 358 | `skimEquivalent` 132, `executeBuild` 130, `executeMain` 46, `doTrainArmyHigh` 27 | do zrobienia |
| 20 | `Micro/Agency/CommanderWork.scala` | 357 | `gather` (od L64 do L208 — miara gubi się na zagnieżdżonych blokach), `build` | do sprawdzenia ręcznie |

## Co to znaczy dla dalszej pracy

Trzy pliki z listy są już rozbite (`UnitClass`, `PlacementCacheFormat`, częściowo
`Tactician`). Reszta to głównie **algorytmy**, nie zbiory danych, i dzieli się
na dwie klasy:

1. **Czysta matematyka / polityka** — tnie się łatwo i bezpiecznie:
   `WallFinder.canPlace`, `scoreWall`, `selectWallWithFiller`,
   `SimulationGrid.applySplash`, `WallGaps.between` i `route`,
   `SimVizView.paintComponent` (rysowanie klatek).
2. **Sterowanie stanem gry** — `MacroSim.simulate`, `PvZ1BaseReactive.buildTech`,
   `SquadDefendBase.run`, `MissionReaverDrop.refreshItinerary` — tu każda
   ekstrakcja zmienia kolejność efektów, więc wymaga gry weryfikującej, nie
   tylko testów.

Sugerowana kolejność: (1) `WallFinder` + `SimulationGrid` (czysta matematyka,
testy jednostkowe), (2) `Debugging/*` (UI, zero wpływu na grę), (3)
`PvZ1BaseReactive` i `SquadDefendBase` na końcu — po jednej metodzie na grę.
