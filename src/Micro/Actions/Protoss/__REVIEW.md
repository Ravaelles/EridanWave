# Code review: `src/Micro/Actions/Protoss`

Przegląd po refaktorze, który pogrupował luźne klasy w foldery jednostek
(`Air/`, `Arbiter/`, `DarkTemplar/`, `Detector/`, `Dragoon/`, `Zealot/`) i
rozbił `BeRushingZealot` na czyste moduły. Zakres: 39 plików, ~1960 linii.

## Co już działa dobrze

- **Podział na foldery jednostek** — `Carrier/`, `Reaver/`, `Shuttle/` były
  wzorcem, teraz cała reszta go trzyma. `find` po nazwie jednostki wystarcza,
  żeby zobaczyć jej mikrozarządzanie.
- **`Reaver/ReaverSituation`** — wzorcowy seam: jeden odczyt gry na klatkę,
  reszta decyzji jako funkcje na `case class`. Tak powinny wyglądać pozostałe
  jednostki.
- **`Detector/DetectorSpread`** — czysty kernel bez `With`, jedyny plik w
  folderze z testem (`DetectorSpreadSuite`). Wzór do powtórzenia.
- **`Zealot/`** po refaktorze: `ZealotRushScoring` (wagi), `ZealotRushAssessment`
  (walczyć czy nie), `ZealotRushTargets` (co bić), `ZealotShieldBattery`
  (gdzie się doładować), `BeRushingZealot` tylko wydaje rozkaz.

## Znaleziska

Kolejność: najpierw te, które najbardziej utrudniają czytanie i testowanie.

### 1. `Carrier/BeCarrier.perform` — ~95 linii, pięć decyzji w jednym ciele

Metoda wybiera cel (brawl / orderTarget / `Target.choose`), liczy dwa marginesy
bezpieczeństwa i prowadzi pięciostanową maszynę (`Brawl`, `TankHunt`, `Chase`,
`Attack`, `OpenLeash`, `HoldLeash`). Progi są rozsiane jako literały:
`32 * 10`, `32 * 9`, `32 * 8`, `7.5 * 32`.

Propozycja:
- `CarrierBrawl` (już istnieje) niech trzyma **wszystkie** promienie:
  `leashTiles = 10`, `launchTiles = 9`, `openLeashTiles = 8`, `homeLeashTiles = 7.5`.
- `CarrierEngagement.decide(unit, brawling, interceptorsActive, margins)` →
  `CarrierOrder` (ADT), a `perform` tylko go wykonuje. Wtedy stan Carrierów
  daje się przetestować bez gry, tak jak `ReaverSituation`.
- `def brawlTarget` / `def target` to domknięcia liczone przy każdym użyciu;
  jako `val` (albo funkcje w module) są czytelniejsze i nie przeliczają
  `matchups.targets` trzy razy.

Ryzyko: średnie (zachowanie w walkach Carrierów jest wrażliwe); zrobić razem
z prostym testem na `CarrierOrder`.

### 2. `Shuttle/BeShuttle.perform` — ~85 linii i `return` wewnątrz `foreach`

`AirFleeGeometry.focus(...).foreach(focus => { ...; return })` oraz
`emergencyHailer.foreach(u => { ...; return })` to `NonLocalReturnControl`
(czysty wyjątek na ścieżce Hot). Trudno to śledzić i kosztuje.

Propozycja:
- `AirFleeGeometry.fleeTarget(shuttle): Option[Pixel]` — decyzja bez rozkazu;
  `perform` robi `if (fleeTarget.isDefined) { ...; }` zamiast `return`.
- Lista `quests` + `ShuttleQuest.finalCost` to model kosztów: wydzielić
  `ShuttleQuestCost.score(quest)` i dać mu test (czysta arytmetyka, dziś
  nietestowalna).
- `fightRadius = 320` jest lokalną stałą, obok `AirFleeGeometry.threatRadiusTiles`
  (12 kafli) — oba „ile kafli to jeszcze walka” powinny mieszkać w jednym miejscu.

### 3. `Reaver/ReaverSituation.read` — ~70 linii, 30 pól w jednym rekordzie

Odczyt miesza trzy światy: własny stan Reavera, zagrożenia i misję (contain,
drop, unload-veto). 30 pól w jednym `case class` sprawia, że testy ustawiają
kilkanaście wartości, żeby sprawdzić jedną regułę.

Propozycja:
- `read` = `Own.read(unit)`, `Threats.read(unit)`, `Mission.read(unit)` i
  złożenie; każdy z tych rekordów ma ≤10 pól.
- `ReaverSituation` zostaje fasadą (`derived` pola jak `pressured`), więc
  zachowania w `Reaver/Reaver*.scala` się nie zmieniają.

### 4. `ReaverLegacy` ↔ `Reaver` — cykl i dwa żywe paradygmaty

`Reaver/BeReaver` importuje `ReaverLegacy.BeReaverLegacy`, a
`ReaverLegacy/BeReaverLegacy` importuje `Reaver.BeReaver` (po `clickShuttle`).
Dodatkowo dwa style prowadzenia Reaverów żyją równolegle pod
`With.blackboard.reaverAtlantisBehavior`.

Propozycja:
- `clickShuttle` przenieść do `Reaver/ReaverActions` (albo `Commander`),
  żeby cykl zniknął.
- Ustalić kryterium usunięcia `ReaverLegacy` (np. „po N grach bez regresji
  w dropach”) i wpisać je do `.ai/todo.md`; inaczej zostanie na zawsze.
- `ReaverLegacy/` przenieść pod `Reaver/Legacy/`, gdy będzie jasne, że to
  kod do wygaszenia, a nie druga gałąź rozwoju.

### 5. `Zealot/BeRushingZealot.retreat` dubluje wycofywanie Dragoona

`retreat` w Zealocie i w `Dragoon/BeDragoonVsSlowlot` robi to samo: bierze
sojuszników dalej od zagrożenia, wybiera dom, ustawia `destinationNext` i
`Commander.move`. Trzecia kopia siedzi w `Combat/Maneuvering/Retreat`.

Propozycja: jeden `Withdraw.toAllies(unit, marginPixels)` w
`Micro/Actions/Combat/Maneuvering/`, używany przez oba pliki. Trzy kopie tej
samej reguły to trzy miejsca do poprawiania, gdy zmieni się definicja
„bezpiecznego sojusznika”.

### 6. `Dragoon/BeDragoonVsSlowlot` — polityka w `perform` i martwe wywołanie

`buffer / hitsLeft / safety / marginRequired = Math.max(32, 128 - 8 * hitsLeft)`
to czysta polityka („jak blisko wolno podejść, mając N strzałów zapasu”),
wrzucona w środek wydawania rozkazów. Dodatkowo w `retreat` pierwsza linia:

```scala
val force = unit.agent.forces.sum.radians
MicroPathing.getWaypointInDirection(unit, force, requireSafety = true)
```

liczy waypoint i **wyrzuca wynik** — poniżej `lazy val waypointSimple` robi to
samo jeszcze raz. To albo relikt, albo zagubiony efekt uboczny; do usunięcia
po potwierdzeniu w grze.

Propozycja: `DragoonSlowlotPolicy` (`margin`, `priority`, `marginRequired`) +
`Suite`; `perform` tylko dispatch.

### 7. `Air/BeFlier` — dwie odpowiedzialności i pełna ścieżka w treści

`fleeScourgeToRear` dotyczy wyłącznie Shuttle'a z Reaverami, a siedzi w ogólnym
zachowaniu lotniczym; w środku używa pełnych ścieżek
(`Micro.Actions.Protoss.Shuttle.ShuttleFleeGeometry`), mimo że import byłby
krótszy.

Propozycja: `Shuttle/ShuttleScourgeFlee` (z `Option[Pixel]`, patrz punkt 2) i
`BeFlier` woła je jednym `if`. `BeFlier` zostaje: „lotnicze unikanie Scourge'ów”.

### 8. `Zealot/BeZealotVsZealot.perform` — cztery branche zapisują to samo

Każdy z czterech warunków ustawia `fightReason` + `shouldFight` i robi `return`.
Propozycja: `DuelDecision` (`Outnumbered`, `Dying`, `Duelling`, `Outnumbering`,
`None`) zwracany przez czystą funkcję; jedno miejsce zapisuje decyzję do agenta.
Zysk: całą logikę pojedynków można przetestować bez jednostek.

### 9. `DarkTemplar/BeDarkTemplar` — zagnieżdżone `if` + magiczne 96

Trzy warunki (`orderTarget`, samotna mina w promieniu, widoczna, nie ukryta)
da się zebrać w `SpiderMineSniping.snipeTarget(unit): Option[UnitInfo]`;
`val radius = 96` → `SpiderMineSniping.radiusPixels`. Dziś to 30 linii, z
których żadna nie jest testowalna.

### 10. `Detector/BeDetector.perform` — pięć obowiązków, ale seam jest dobry

`BeFlier` → `Spot` → `Recharge` → `spread` → `agent.combat` to łańcuch, nie
metoda. Warto zostawić, ale `spread` (dobór „starszego” oka) wyciągnąć do
`DetectorSpread.partner(us, others)` — wtedy cały dobór pary ląduje w pliku,
który już ma test.

### 11. Reguła nazw pakietów: `Zealot`, `DarkTemplar`, `Arbiter`, `Dragoon`, `Carrier`, `Reaver`

Te pakiety **przesłaniają** funkcje z `ProxyBwapi.Races.Protoss`
(`Protoss.DarkTemplar`, `Protoss.Zealot`, ...). Dziś kompiluje się, bo pliki
importują `ProxyBwapi.Races.{Protoss, ...}` jawnie — wildcard
(`import ProxyBwapi.Races._` albo `import ProxyBwapi.Races.Protoss._`) w tych
pakietach da się rozwiązać na pakiet i da mylący błąd „value DarkTemplar is not
a function”.

Propozycja: dopisać do `.ai/conventions.md` jedną linijkę: *w pakietach
`Micro.Actions.Protoss.<Jednostka>` importować rasy jawnie, nigdy wildcardem*.
Alternatywa (kosztowna, nie polecam teraz): liczba mnoga folderów
(`Zealots/`, `DarkTemplars/`).

### 12. `Support/Meld` — `Retreat` bez `return`

`if (framesOfSafety < ...) Retreat(unit)` nie przerywa metody: po wycofaniu
jednostka i tak dostaje rozkaz meldu z najlepszym kumplem w metrze. Jeśli to
zamierzone (meld ma priorytet nad ucieczką), wart komentarz; jeśli nie —
brakuje `return`, i to jest błąd, który kosztuje Archanona.

### 13. Testy

W całym folderze jest **jeden** test (`DetectorSpreadSuite`). Po punkcie 1, 3,
6 i 8 dochodzą cztery czyste kernele, dla których test jest tańszy niż gra:
`CarrierOrder`, `ShuttleQuestCost`, `DragoonSlowlotPolicy`, `DuelDecision`.
To najlepszy stosunek zysku do ryzyka w tym folderze.

## Proponowana kolejność prac

| # | Zmiana | Ryzyko | Zysk |
|---|--------|--------|------|
| 1 | `DragoonSlowlotPolicy` + usunięcie martwego `getWaypointInDirection` | niskie | polityka testowalna, mniej kodu |
| 2 | `Withdraw.toAllies` dla Zealota i Dragoona | niskie | jedna reguła wycofywania |
| 3 | `ShuttleQuestCost` + `Option`-owe `AirFlee` | średnie | koniec `NonLocalReturn` w hot path |
| 4 | `DuelDecision` + `SpiderMineSniping` | niskie | dwa małe kernele z testami |
| 5 | `ReaverSituation` na trzy rekordy | średnie | czytelniejsze testy Reaverów |
| 6 | `CarrierOrder` i stałe promieni | wyższe | Carrier wreszcie testowalny |

## Pytania otwarte

- `Meld`: czy meld ma wygrać z ucieczką? (punkt 12)
- `BeDragoonVsSlowlot`: czy pierwszy `getWaypointInDirection` ma efekt uboczny
  (cache w `MicroPathing`), czy to zwykły relikt? (punkt 6)
- Kiedy `ReaverLegacy` idzie do usunięcia? (punkt 4)
