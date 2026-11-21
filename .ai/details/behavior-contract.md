# Kontrakt zachowań: bramka kontekstu jednostki (PLAN, bez implementacji)

Decyzje właściciela (2026-11-21): zakres W1 (kontrakt, nie hierarchia);
`appliesToThisUnit` abstrakcyjna (wymusza kompilator); implementacji
NIE zaczynamy, dopóki plan nie zostanie zaakceptowany.

## Po co to robimy

Powtarzający się kształt błędów z audytu 2026-11 (14+ instancji):
zachowanie odpala się bez sprawdzenia, czy sytuacja w ogóle dotyczy
TEJ jednostki. Nie wiemy, czy dotyczy workerów. Nie wiemy, czy
jednostka jest wykryta. Mylimy zasięg wroga z własnym. Przykłady:

- Cudzy zasięg zamiast własnego: TankHold (swój max zamiast zasięgu
  czołgu), HoldToShoot-vs-Vulture (zasięg Vulture'a zamiast własnego),
  KiteTo (ścieżka lądowa dla jednostek powietrznych),
  ScoutStandoff (środek statica zamiast krawędzi), ZealotPack feed
  (środek zamiast krawędzi), MarchUnit (attacksGround zamiast
  canAttack; środki zamiast krawędzi).
- Duchy z mgły jako pretekst: KiteAway (marginesy, holdingShot,
  melee-zealoty kolegów z drużyny), Abuse (threatDeepest),
  DarkTemplarHunt/Carrier pick, MineDefuse veto, SuicideEngage.
- Zła jednostka: SlotDiscipline (budynki/pracownicy wetują dress),
  Brawl bez oceny szans, ChokeHold-commit bez oceny szans.
- Ostre krawędzie bez release: dogfight 0.25, carrier-hunt 11.0,
  minedefuse 12.0, escaping-toggle.

## Co jest nie tak strukturalnie (fakty)

1. 23 obiekty `*Behavior` to wolne singletony: zero wspólnego rodzica,
   zero wymuszonego kontraktu, 4 różne sygnatury `run`.
2. Rejestry (TechniquePipeline / EngagePipeline /
   HandleEnemyCombatBuildings) to case classy z lambdami - kolejność
   jest, kontraktu nie ma. Trzeci rejestr to nawet nie klasa, tylko
   krotki.
3. Bramki kontekstowe leżą w 3 miejscach naraz: lambda stage'a,
   predykat transition, early-return w `run`. Przykłady klas bez
   ŻADNEJ bramki w `run` (ufają callerowi): Push, Reposition,
   Superkite, Aim. Brak bramki = brak miejsca, które review sprawdza.

## Podejście W1 (zaakceptowane): kontrakt, nie hierarchia

Wspólny przodek wpisów pipeline'ów, nie drzewko zachowań:

```scala
abstract class BehaviorStage {
  // KONTEKST JEDNOSTKI (abstrakcyjna - brak metody = brak kompilacji):
  // rasa/klasa/lot, worker/warrior, widoczność i wykrycie w obie
  // strony, symetria broni (czyj zasięg?), domena (edge/center,
  // tiles/piksele). Odpowiedzi na 4 pytania z checklisty poniżej.
  protected def appliesToThisUnit(unit: FriendlyUnitInfo): Boolean
  // Logika doktryny przy założeniu, że jednostka pasuje.
  protected def logicApplies(unit: FriendlyUnitInfo, combat: Combat): Boolean
  // Jedyny wołany punkt: template, nie do nadpisania.
  final def shouldRun(unit: FriendlyUnitInfo, combat: Combat): Boolean =
    appliesToThisUnit(unit) && logicApplies(unit, combat)
  protected def execute(unit: FriendlyUnitInfo, combat: Combat): Boolean
}
```

- Migracja jest mechaniczna i bez zmian zachowań: istniejące
  early-returny rozjeżdżają się na dwie metody, lambdy w pipeline'ach
  zamieniają się w klasy. Suite'y pilnują (base green przed/po).
- Rejestr pinuje suite: jawna lista wszystkich stage'ów w teście;
  nowy stage bez wpisu (i bez suity) nie przechodzi gate'a.
- Checklista `appliesToThisUnit` (do konwencji, reguła 2):
  1. Jakie jednostki? (rasa/klasa/lot/worker/caster/transport)
  2. Detekcja w obie strony? (czy my visible? czy oni visible/detected?)
  3. Symetria broni? (czy my trafimy? czy oni trafią? CZYJ zasięg?)
  4. Domena? (ziemia/powietrze, edge/center, tiles/piksele?)

## Entry/exit per behavior: rozstrzygnięcie

Musowanie właściciela: zatrzask per behavior (warunek początku
i końca jak w Latch). Decyzja: NIE latchować arbitrażu per behavior
(to robił HoldingDuel - zamrożony właściciel ramki). Podział:
- Arbitraż (KTO teraz): werdykty z busem (Retreat/Motion/Tank) -
  one mają entry/release. Detektor cykli weryfikuje (CROSS = 0).
- Ciągłość wewnątrz zachowania (pogoń, leash, evade, holdFarWait):
  lokalny Latch ma sens i już tam jest. Kandydaci bez: dogfight
  0.25, carrier-hunt 11.0, minedefuse 12.0, escaping-toggle -
  PARK (wymagają gier, nie teorii).

## Brawl: pochodzenie i decyzja

Brawl pochodzi z Atlantis (2022, "new Combat") jako executor
anty-glitch: w zwarciu melee surowe rozkazy ataku rwą animacje,
stand-and-swing przez jeden rozkaz tego unika. Dlatego nie patrzy
na szanse - to nie jest jego warstwa. Szanse należą do werdyktu
walki (sim), nie do executora. Opcja scopingu (tylko UMS albo
supply>190 / minerały 1000+) istnieje jako przełącznik doktrynalny,
ale niesie ryzyko powrotu glitchy na drabince - NIE zalecane bez
gry, która by tego wymagała. Sprawa zealota została zamknięta
choke-gatem (brak commitu umierającego -> odwrót działa), nie
ruszeniem Brawla.

## Harness do testów run() (osobny temat)

TestKit dziś umie: seedWorld, mockUnit (typ/pozycja/HP), friendly()
(gotowe FriendlyUnitInfo). Brakuje: matchups z wrogami (UnitTracker
do zasilenia mockami), agent (intent/decision/forces), With.*
(battles, skim, latency, ścieżki). Ścieżka: builder scenariuszy
w TestKicie (seed N jednostek + wiring), potem StarEngine pilots.
Do tego czasu: kernele pokryte suitami, wiring w run() walidowany
live + detektorem CROSS.

## Kolejność migracji (gdy będzie zielone światło)

1. BehaviorStage + suite rejestru (pusty szablon, zero zachowań).
2. Partiami po pipeline'ach: StaticKeepOff, potem Technique, potem
   Engage. Każda partia: mechaniczny split, base green, 1 mapa UMS
   + detector (CROSS musi zostać 0).
3. Dopiero potem: Brawl odds-gate (JEŚLI właściciel potwierdzi),
   W2-hierarchia (JEŚLI W1 okaże się za słabe).
