# PvZ gameplans: co oferuje folder

Przegląd po reorganizacji: 17 plików w trzech folderach + rejestr modów.
Poniżej mapa funkcji i to, co jeszcze warto rozbić.

## Mapa

```
PvZ/
  ProtossVsZerg.scala     rejestr modów: najpierw Eridan (PvZEridanStandard),
                          potem fallbacki PurpleWave (kolejność = priorytet)

  openings/               jak grać otwarcie - konkretne, imperatywne listy
    PvZ1BaseBuildOrders   build ordery: 910, 1012 (gas/expand), CoreZ,
                          ZCoreZ, ZZCoreZ, GateNexus, NexusFirst, speedlingi
    PvZ1BaseOpenings      starszy wybór otwarcia (`open()`); dziś tylko dla
                          modów, które nie używają reactive
    PvZ1GateCoreExpand    2-gate core expand
    PvZ1GateCoreYolo*     yolo speedlot / yolo dragoon
    PvZ1BaseAllIn         all-in z jednej bazy
    PvZFE                 wall + natural Nexus (FFE/GFE)

  reactive/               co robić w trakcie gry, zależnie od scoutingu
    PvZ1BaseReactive      orkiestrator: opening w executeBuild, decyzje
                          (early / timing / all-in / obrona) i produkcja
                          w executeMain
    PvZReactiveOpenings   wybór otwarcia pod ekonomię i timing poola
    PvZReactiveComposition wybór kompozycji (Goon/Speedlot/Reaver/Stargate)
    PvZReactiveTech       struktury i anulowania pod kompozycję, gas floor,
                          detekcja na Lurkery
    PvZReactiveArmy       potrzeby i cele Compositora + pump fillerów
    PvZArmyWeights         czysty model wag armii (test: PvZArmyWeightsSuite)
    PvZ1BaseReactiveUtilities  enumy (Economy/Opening/Composition) i drabinka
                          timingów poola (4p/9p/op/12p/10h/12h/12hh)
    Pv4Pool               reakcja na 4-pool

  policy/                 czyste bramki, bez dostępu do gry, każda z testem
    GasBank               ile gazu musi leżeć, by kolejkować Dragoona
    ReaverGas             to samo dla drugiego/trzeciego Reavera
    RecoveryGas           nieudany gasless all-in → dokup gaz
    StormResearch         czy badać Psionic Storm
    DefenseWithoutDragoons  bez Dragoonów i słabi → bronić zamiast feedować
    EarlyPressure         czy w ogóle wolno wypchnąć armię przed trzecim wojem
```

## Jak to płynie

1. `ProtossVsZerg` wybiera mod; `PvZ1BaseReactive` jest fallbackiem PurpleWave.
2. `PvZ1BaseReactive.executeBuild` ustawia `economy`, wybiera `opening`
   (`PvZReactiveOpenings`) i kompozycję (`PvZReactiveComposition`), po czym
   odpala konkretny build order z `openings/`. Otwarte speedlingi mają
   osobną ścieżkę (`openVsSpeedlings`).
3. `executeMain` co klatkę: policz `earlyAttack`/`timingAttack`/`allIn`,
   wydaj `attack`, potem `SwapIf(buildArmy, buildTech)` — przy supply powyżej
   progu armia wypiera tech, dlatego `ensureCompositionTech` gra rolę
   bezpiecznika.
4. `policy/` jest wołane z `reactive/` i z Eridanowego `ZealotIntoGoon`;
   bramki nie znają gry, więc mają testy.

## Znaleziska

1. **Dwie hierarchie `Opening`.** `PvZ1BaseReactiveUtilities.Opening`
   (7 wartości) i `PvZ1BaseOpenings.Opening` (4 wartości, `Open910` itd.)
   opisują to samo innymi nazwami; `PvZFE` mostkuje je przez
   `new PvZ1BaseReactive`. Warto zostawić jedną (najlepiej w `openings/`)
   i usunąć drugą razem z `open()`.
2. **`PvZFE` (298 linii) i `PvZ1BaseBuildOrders` (262).** Po jednym
   `executeBuild`/`open*` na otwarcie, każdy po 20–40 linii. Propozycja:
   `PvZFeBuild` (Nexus, Pylon, wall, cannons) i po jednej metodzie
   `order910`/`order1012`/`orderCoreZ`… na otwarcie, z `openings/steps/`
   gdyby miało ich być więcej.
3. **`PvZReactiveTech.buildTech` nadal ~70 linii**, cztery gałęzie
   `composition.foreach`. Propozycja: `techGoon`, `techSpeedlot`,
   `techReaver`, `techStargate` po ≤20 linii; `buildTech` tylko dispatch.
4. **`PvZ1BaseReactiveUtilities`**: `pool`, `poolMinHistory`, `poolMaxHistory`
   i `poolMin` to cztery prawie identyczne drabinki `if`-ów po odczytach
   `With.fingerprints`. Jedna tabela `Seq[(fingerprint, timing)]` i trzy
   funkcje na niej.
5. **`Pv4Pool`** jest wywoływane tylko przez `reactVs4Pool()` w
   `PvZ1BaseBuildOrders`; to jedna linia pośrednictwa — może zniknąć.
6. **`PvZReactiveArmy`** po wyciągnięciu `PvZArmyWeights` ma 73 linie i jedną
   metodę; następny krok to podział na `needDragoons`, `needReavers`,
   `needCasters`, gdyby doszły kolejne jednostki.

## Kolejność

1. `PvZReactiveTech` → cztery `tech*` (największy zysk, zero ryzyka zmian).
2. Jedna hierarchia `Opening` (usunięcie `PvZ1BaseOpenings`).
3. `PvZFE` i `PvZ1BaseBuildOrders` na pojedyncze metody per otwarcie.
4. Tabela timingów poola w `PvZ1BaseReactiveUtilities`.
