# Predlog projekta: Sistem za preporuku akcija u borbi u igri World of Warcraft

## Članovi tima

- Ilija Jordanovski SV 73/2022

---

## Opis problema

### Motivacija

World of Warcraft (WoW) je MMORPG(massive multiplayer online role playing game) u kojoj igrači upravljaju likovima koji se bore protiv protivnika koristeći skup sposobnosti — napada i magija. Svaki lik ima ograničene resurse (npr. bes, energiju ili manu) i sposobnosti s vremenima punjenja. **Rotacija** je optimalan redosled korišćenja sposobnosti koji maksimizuje štetu nanesenu protivniku. Određivanje optimalnog poteza u svakom trenutku zahteva uzimanje u obzir desetina faktora: trenutnih resursa lika, aktivnih vremenski ograničenih efekata, dostupnosti sposobnosti i stanja protivnika. Ovo znanje iskusnih igrača prirodno se izražava kao skup pravila oblika *„ako je stanje X, upotrebi sposobnost Y"*, što ga čini idealnim kandidatom za sistem baziran na pravilima.

### Pregled problema

Postojeća rešenja za upravljanje rotacijama su:

- **SimulationCraft** — moćan simulacioni alat koji zahteva imperativno programiranje; nema objašnjivosti (igrač zna *šta* da koristi, ali ne i *zašto*)
- **In-game dodaci (Hekili, MaxDps)** — preporučuju akcije u realnom vremenu, ali bez transparentnog mehanizma zaključivanja i bez podrške za složene vremenske obrasce
- **Statički vodiči (Icy Veins, Wowhead)** — tekstualni opisi prioriteta koji ne uzimaju u obzir trenutno stanje borbe

Predloženi sistem se razlikuje po dva ključna aspekta: **transparentno zaključivanje** (svaka preporuka praćena je nizom pravila koja su je prouzrokovala) i **detekcija vremenskih obrazaca** relevantnih za domen — npr. kada se nekoliko pojačavajućih efekata istovremeno aktivira u kratkom vremenskom prozoru.

### Metodologija rada

#### Ulazi u sistem

Sistem prima JSON reprezentaciju trenutnog stanja borbe:

- Stanje igrača: klasa i specijalizacija, trenutni resursi, lista aktivnih efekata s preostalim trajanjem, dostupnost svake sposobnosti (vreme punjenja)
- Stanje protivnika: procenat preostalog zdravlja, aktivni efekti na protivniku
- Kontekst borbe: proteklo vreme od početka borbe

#### Izlazi iz sistema

- **Primarna preporuka** — naziv sposobnosti koju treba upotrebiti i nivo hitnosti
- **Lanac zaključivanja** — lista pravila koja su dovela do preporuke (objašnjivost)
- **Naredne preporučene sposobnosti** — kratkoročno planiranje (2–3 poteza unapred)
- **Upozorenja** — vremenski kritične situacije (npr. ključni efekat uskoro ističe)
- **Status burst prozora** — da li je trenutak pogodan za upotrebu glavnih sposobnosti s dugim punjenjem

#### Baza znanja

Baza znanja obuhvata:

1. **Šablone (templates)** — parametrizovane definicije sposobnosti, efekata i specijalizacija koje omogućavaju ponovnu upotrebu pravila za sve tri specijalizacije
2. **Pravila derivacije (1. nivo)** — izvode apstraktne činjenice iz sirovog stanja: npr. *„resurs je kritično visok"*, *„protivnik je na niskom zdravlju — faza egzekucije"*, *„aktivan je vremenski ograničen efekat koji pojačava štetu"*
3. **Pravila procene (2. nivo)** — kombinuju izvedene činjenice u taktičke procene: npr. *„burst prozor je dostupan"*, *„urgentno trošenje resursa"*, *„neophodno je obnoviti vremenski efekat na protivniku"*
4. **Pravila preporuke (3. nivo)** — mapiraju taktičke procene na konkretne sposobnosti, uređene po prioritetu pomoću `salience` mehanizma
5. **CEP pravila (Drools Fusion)** — detektuju vremenske obrasce u toku borbe: istovremenu aktivaciju više pojačavajućih efekata, predugo zadržavanje resursa na maksimumu, ili simultano isticanje više vremenskih efekata; emituju složene događaje koji menjaju prioritete preporuka
6. **Backward chaining upiti** — proveravaju dostupnost sposobnosti i kratkoročno predviđaju burst prozore

Baza znanja popunjava se na osnovu teorycrafting resursa: SimulationCraft APL-ova za Dragonflight 10.1–10.2 i vodiča iz zajednice igrača, ručno konvertovanih u DRL pravila.

Sistem podržava tri specijalizacije s različitim resursnim paradigmama i mehanikama:

| Specijalizacija | Resursi | Karakteristična mehanika |
|---|---|---|
| Fury Warrior | Rage (0–100) | Proc mehanike, faza egzekucije, burst efekti |
| Subtlety Rogue | Energy (0–100) + Kombo poeni (0–7) | Burst prozori u stealth modu, upravljanje kombo poenima |
| Havoc Demon Hunter | Fury (0–100+) | Transformacija (Metamorphosis), Essence Break amplifikacija, dvostepeni burst prozor |

#### Šabloni (templates)

Analizom pravila identifikovana su četiri ponavljajuća strukturna obrasca koja se šablonizuju Drools `template` konstruktima, čime se smanjuje ukupan broj eksplicitnih pravila u bazi znanja.

| Šablon | Parametri | Zamenjuje | Instancijacija |
|---|---|---|---|
| **CooldownReady** | `abilityName`, `spec` | R8, R9, R14, R15, R17, R21, R23, R24, R25 | 9 |
| **ResourceOvercapRisk** | `spec`, `resourceField`, `threshold` | R5, R22 | 2 |
| **EffectExpiring** | `effectName`, `threshold` | R4, R12, R13 | 3 |
| **FinisherForRefresh** | `refreshFact`, `priorityFact` | T6, T7 | 2 |

**CooldownReady** — detektuje da sposobnost nije na punjenju i umeće odgovarajući fakt. Svih 9 pravila ima identičnu strukturu: `AbilityState(name == X, cooldownRemaining == 0.0) → insert(XReady)`. Šablon parametrizuje naziv sposobnosti i specijalizaciju.

Instancijacije: `(Recklessness, FW)`, `(OdynsFury, FW)`, `(Flagellation, SR)`, `(SymbolsOfDeath, SR)`, `(SecretTechnique, SR)`, `(EssenceBreak, DH)`, `(EyeBeam, DH)`, `(BladeDance, DH)`, `(Felblade, DH)`.

**ResourceOvercapRisk** — detektuje da je primarni resurs blizu maksimuma i umeće fakt rizika. Struktura: `PlayerState(resource >= threshold) → insert(XHighRisk)`. Parametri određuju koji resurs se prati i prag.

Instancijacije: `(FURY_WARRIOR, rage, 80, RageHighRisk)`, `(HAVOC_DH, fury, 80, FuryHighRisk)`.

**EffectExpiring** — detektuje da vremenski efekat uskoro ističe ili nije aktivan i umeće fakt za obnavljanje. Struktura: `EffectState(name == X, remainingSeconds < threshold) → insert(XRefreshNeeded)`.

Instancijacije: `(Enrage, 1.5, EnrageExpiring)`, `(Rupture, 7.0, RuptureRefreshNeeded)`, `(SliceAndDice, 2.0, SndRefreshNeeded)`.

**FinisherForRefresh** — kombinuje nivo-1 fakt obnavljanja efekta i prisustvo `FinisherReady` u taktički prioritet (nivo 2). Struktura: `XRefreshNeeded + FinisherReady → insert(PrioritizeX)`.

Instancijacije: `(SndRefreshNeeded, PrioritizeSnd)`, `(RuptureRefreshNeeded, PrioritizeRupture)`.

#### Lista pravila forward chaininga

**1. nivo — Derivacija stanja** (izvođenje apstraktnih činjenica iz sirovog stanja igre)

| Oznaka | Uslov | Izvedena činjenica | Specijalizacija |
|---|---|---|---|
| R1 | Protivnik ima manje od 20% zdravlja | `ExecutePhase` — aktivirana faza egzekucije | Fury Warrior |
| R2 | Protivnik ima 20–35% zdravlja (Massacre talent) | `MassacreRange` — Execute dostupan pre standardnog praga | Fury Warrior |
| R3 | Buff Enrage je aktivan | `EnragedState` — pojačavajući efekat koji ubrzava napade | Fury Warrior |
| R4 | Buff Enrage ima manje od 1,5s preostalog trajanja | `EnrageExpiring` — hitno treba obnoviti Enrage | Fury Warrior |
| R5 | Bes (Rage) ≥ 80 od 100 | `RageHighRisk` — resurs blizu maksimuma, overcap rizik | Fury Warrior |
| R6 | Proc Sudden Death je aktivan | `SDProcActive` — besplatna upotreba Execute | Fury Warrior |
| R7 | Buff Recklessness je aktivan | `RecklessnessActive` — burst buff pojačava Execute i generisanje besa | Fury Warrior |
| R8 | Sposobnost Recklessness nije na punjenju | `RecklessnessReady` — burst cooldown spreman za aktivaciju | Fury Warrior |
| R9 | Sposobnost Odyn's Fury nije na punjenju | `OdynsFuryReady` — sekundarni burst cooldown spreman | Fury Warrior |
| R10 | Buff Shadow Dance je aktivan | `StealthWindowActive` — stealth burst prozor otvoren | Subtlety Rogue |
| R11 | Kombo poeni ≥ 5 | `FinisherReady` — prag za upotrebu finalnog udarca dostignut | Subtlety Rogue |
| R12 | Efekat Rupture na protivniku ima < 7s preostalog trajanja ili nije aktivan | `RuptureRefreshNeeded` — bleed efekat treba obnoviti | Subtlety Rogue |
| R13 | Efekat Slice and Dice ima < 2s preostalog trajanja ili nije aktivan | `SndRefreshNeeded` — maintenance buff mora biti aktivan | Subtlety Rogue |
| R14 | Sposobnost Flagellation nije na punjenju | `FlagellationReady` — primarni burst cooldown spreman | Subtlety Rogue |
| R15 | Sposobnost Symbols of Death nije na punjenju | `SymbolsReady` — cooldown spreman za aktivaciju | Subtlety Rogue |
| R16 | Buff Symbols of Death je aktivan | `SymbolsActive` — bonus damage aktivan, burst prozor pojačan | Subtlety Rogue |
| R17 | Sposobnost Secret Technique nije na punjenju | `SecretTechReady` — sposobnost visokog prioriteta unutar Dance prozora | Subtlety Rogue |
| R18 | Shadow Dance ima jedno ili više punjenja dostupnih | `ShadowDanceAvailable` — stealth prozor može biti aktiviran | Subtlety Rogue |
| R19 | Buff Metamorphosis je aktivan | `MetaActive` — sve sposobnosti prelaze u pojačanu verziju | Havoc DH |
| R20 | Debuff Essence Break je aktivan na protivniku | `EssenceBreakActive` — sledeći udari amplifikovani za 40% | Havoc DH |
| R21 | Sposobnost Essence Break nije na punjenju | `EssenceBreakReady` — debuff spreman za primenu | Havoc DH |
| R22 | Fury ≥ 80 od 100 | `FuryHighRisk` — resurs blizu maksimuma, overcap rizik | Havoc DH |
| R23 | Sposobnost Eye Beam nije na punjenju | `EyeBeamReady` — primarni burst cooldown spreman | Havoc DH |
| R24 | Sposobnost Blade Dance / Death Sweep nije na punjenju | `BladeDanceReady` — prioritetna sposobnost spreman | Havoc DH |
| R25 | Sposobnost Felblade nije na punjenju | `FelbladeReady` — Fury generator i gap closer spreman | Havoc DH |
| R26 | Buff Unbound Chaos je aktivan | `UnboundChaosActive` — sledeći Fel Rush nosi pojačani udarac | Havoc DH |

**2. nivo — Taktička procena** (kombinovanje izvedenih činjenica u taktičke zaključke)

| Oznaka | Uslovi | Izvedena činjenica | Specijalizacija |
|---|---|---|---|
| T1 | `EnragedState` + (`RecklessnessReady` ILI `OdynsFuryReady`) | `BurstWindowOpportunity` — optimalni trenutak za aktivaciju burst cooldown-a | Fury Warrior |
| T2 | `ExecutePhase` ILI `MassacreRange` ILI `SDProcActive` | `ExecutePriorityActive` — Execute je dostupan ili prag dostignut | Fury Warrior |
| T3 | `EnrageExpiring` ILI `RageHighRisk` | `RampageUrgent` — Rampage treba odmah zbog Enrage ili overcap-a | Fury Warrior |
| T4 | `RampageUrgent` + `EnragedState` | `EmergencyRageDump` — hitno trošenje besa dok je Enrage aktivan | Fury Warrior |
| T5 | `FlagellationReady` | `FlagellationPriority` — aktivirati pre Symbols i Shadow Dance | Subtlety Rogue |
| T6 | `SndRefreshNeeded` + `FinisherReady` | `PrioritizeSnd` — Slice and Dice obnavljanje je hitan finalni udar | Subtlety Rogue |
| T7 | `RuptureRefreshNeeded` + `FinisherReady` | `PrioritizeRupture` — Rupture obnavljanje je prioritetni finalni udar | Subtlety Rogue |
| T8 | `SymbolsReady` + `StealthWindowActive` NIJE aktivan | `SymbolsBurstEntry` — aktivirati Symbols pre Dance prozora | Subtlety Rogue |
| T9 | `ShadowDanceAvailable` + `SymbolsActive` | `ActivateDance` — otvoriti Dance dok je Symbols buff aktivan | Subtlety Rogue |
| T10 | `StealthWindowActive` + `FinisherReady` | `SpendCPInDance` — trošiti kombo poene unutar Dance prozora | Subtlety Rogue |
| T11 | `SecretTechReady` + `StealthWindowActive` | `SecretTechPriority` — Secret Technique se troši unutar Dance | Subtlety Rogue |
| T12 | `EyeBeamReady` + `MetaActive` NIJE | `BurstEntryReady` — Eye Beam ulazi u burst; Demonic talent aktivira Metu | Havoc DH |
| T13 | `MetaActive` + `BladeDanceReady` | `DeathSweepAvailable` — Death Sweep (Meta verzija Blade Dance-a) dostupan | Havoc DH |
| T14 | `EssenceBreakReady` + `MetaActive` | `EssenceBreakSetup` — primeniti Essence Break dok je Meta aktivna | Havoc DH |
| T15 | `EssenceBreakActive` + `MetaActive` | `MaxBurstWindow` — Essence Break pojačava sve udarce tokom Mete | Havoc DH |
| T16 | `BladeDanceReady` + `MetaActive` NIJE | `BladeWindowOpen` — Blade Dance van Mete za generisanje Fury | Havoc DH |
| T17 | `FuryHighRisk` | `FuryDumpRequired` — hitno trošenje Fury resursa | Havoc DH |

**3. nivo — Preporuka akcije** (mapiranje taktičkih procena na konkretne sposobnosti, opadajući prioritet)

*Fury Warrior:*

| Prioritet | Uslov | Preporuka |
|---|---|---|
| 140 | `EnrageExpiring` | **Rampage** — hitna obnova Enrage-a |
| 130 | `RecklessnessActive` + `RageHighRisk` | **Rampage** — trošiti Rage tokom burst buffa |
| 120 | `SDProcActive` | **Execute** — besplatni Sudden Death proc |
| 110 | `BurstWindowOpportunity` + `RecklessnessReady` | **Recklessness** |
| 105 | `BurstWindowOpportunity` + `OdynsFuryReady` | **Odyn's Fury** |
| 90 | `RampageUrgent` | **Rampage** |
| 80 | `ExecutePriorityActive` | **Execute** |
| 60 | `EnragedState` NIJE aktivan | **Bloodthirst** — generiše Enrage proc |
| 50 | (podrazumevano) | **Raging Blow** |

*Subtlety Rogue:*

| Prioritet | Uslov | Preporuka |
|---|---|---|
| 130 | `PrioritizeSnd` | **Slice and Dice** — obnovi maintenance buff |
| 120 | `PrioritizeRupture` | **Rupture** |
| 110 | `FlagellationPriority` | **Flagellation** |
| 100 | `SymbolsBurstEntry` | **Symbols of Death** |
| 95 | `ActivateDance` | **Shadow Dance** — otvoriti stealth burst prozor |
| 90 | `SecretTechPriority` | **Secret Technique** |
| 85 | `SpendCPInDance` + `PrioritizeRupture` NIJE + `PrioritizeSnd` NIJE | **Eviscerate** |
| 80 | `StealthWindowActive` + `FinisherReady` NIJE | **Shadowstrike** |
| 70 | `FinisherReady` (van Dance prozora) | **Eviscerate** |
| 50 | (podrazumevano) | **Gloomblade** |

*Havoc Demon Hunter:*

| Prioritet | Uslov | Preporuka |
|---|---|---|
| 150 | `MaxBurstWindow` | **Death Sweep** |
| 140 | `EssenceBreakSetup` | **Essence Break** |
| 130 | `DeathSweepAvailable` | **Death Sweep** |
| 120 | `BurstEntryReady` | **Eye Beam** |
| 110 | `EssenceBreakActive` + `MetaActive` NIJE | **Chaos Strike** — konzumirati Essence Break van Mete |
| 100 | `BladeWindowOpen` | **Blade Dance** |
| 80 | `MetaActive` + `BladeDanceReady` NIJE | **Annihilation** — filler tokom Mete |
| 70 | `FuryDumpRequired` | **Chaos Strike** |
| 60 | `FelbladeReady` + `MetaActive` NIJE | **Felblade** |
| 40 | (podrazumevano, van Mete) | **Demon's Bite** |

#### Accumulate funkcije

| Oznaka | Uzorak | Izlaz | Upotreba |
|---|---|---|---|
| A1 | Svi aktivni `BuffState` fakti | `TotalBuffDuration` — zbir preostalog trajanja svih aktivnih efekata | Prikazan u reasoning chain-u; ulaz za procenu ukupnog burst stanja |
| A2 | `ProcEvent` fakti u poslednjih 3 sekunde (sliding window) | `RecentProcCount` — broj proc događaja u prozoru | Ulaz u CEP-1 Proc Storm pravilo; ako `RecentProcCount ≥ 3`, emituje se `ProcStormEvent` |
| A3 | Svi aktivni `DotState` fakti za datu specijalizaciju | `MinDoTRemaining` — minimalno preostalo trajanje od svih aktivnih DoT efekata | Detekcija simultanog istekanja više DoT-ova |
| A4 | Svi aktivni nivo-2 fakti | `ActiveTacticalCount` — broj aktivnih taktičkih procena | Ako `ActiveTacticalCount ≥ 4`, urgency u izlazu se podiže na `CRITICAL` |

#### CEP pravila (Drools Fusion)

Drools Fusion prati tok događaja iz borbe paralelno s forward chainingom. Svako CEP pravilo emituje složeni događaj koji se umeće u radnu memoriju i menja preporuke nivo-3 pravila — nije samo logovanje.

**CEP-1 — Proc Storm** (sve specijalizacije)

Sliding window od 3 sekunde; uslov: `RecentProcCount ≥ 3` (A2 accumulate).
Emituje: `ProcStormEvent` → umeće `ProcStormActive` fakt.
Efekat: sva nivo-3 filler pravila (prioritet ≤ 50) zamenjuju se hitnim proc-spend pravilima — Execute za FW, Shadowstrike za SR, Chaos Strike za DH.

Primer: Fury Warrior dobija Sudden Death proc, Enrage refresh i trinket proc u roku od 2 sekunde — sistem detektuje gužvu i forsira neodložno konzumiranje.

**CEP-2 — Shadow Dance Convergence** (Subtlety Rogue)

Sliding window od 6 sekundi; uslov: `ShadowDanceActivatedEvent` + `SymbolsOfDeathActiveEvent` u istom prozoru.
Emituje: `DanceSymbolsSynergyEvent` → umeće `MaxBurstActive` fakt.
Efekat: blokira CP trošenje dok CP < 5; forsira Shadowstrike spam radi brzog punjenja CP pre Eviscerate.

Ovo modeluje ključnu tehniku: sinhronizovana aktivacija Shadow Dance i Symbols of Death maksimizuje štetu Eviscerate unutar Dance prozora.

**CEP-3 — Essence Break Meta Sync** (Havoc Demon Hunter)

Sliding window od 2 sekunde; uslov: `EssenceBreakAppliedEvent` + `MetaActiveEvent` u istom prozoru.
Emituje: `EBMetaSynergyEvent` → umeće `EBMetaActive` fakt.
Efekat: forsira isključivo Death Sweep rotaciju — `MaxBurstWindow` pravilo (prioritet 150) dobija prednost nad svim ostalim dok su oba efekta aktivna.

Simultana aktivacija Essence Break i Metamorphosis je optimalni burst prozor za Havoc DH koji sistem automatski detektuje.

**CEP-4 — Resource Bleed** (sve specijalizacije)

Uslov: `ResourceHighEvent` (resurs > 85%) ostaje aktivan bez pada duže od 4 sekunde.
Emituje: `ResourceBleedEvent` → umeće `EmergencySpend` fakt.
Efekat: nivo-3 pravila za CD management (prioriteti 90–110) privremeno se preskačemo; prioritet dobijaju pravila za hitno trošenje resursa (70–80).

Modeluje situaciju overcap-a u kojoj igrač propušta da troši resurs, što rezultuje gubitkom potencijalne štete.

#### Backward chaining upiti

Backward chaining je organizovan u tri nivoa, pri čemu svaki viši nivo poziva niži:

**Nivo 3 (list) — Provera resursa** (`resourceSufficient(spec, abilityName)`)  
Najdublji nivo proverava da li igrač ima dovoljno resursa *odgovarajućeg tipa* za datu sposobnost. Svaka specijalizacija ima drugačiji resursni sistem: Fury Warrior koristi Bes, Subtlety Rogue Energiju i Kombo poene, Havoc Demon Hunter Fury. Primer: za Rampage je potrebno Bes ≥ 80, za Eviscerate Kombo poeni ≥ 5, za Chaos Strike Fury ≥ 20.

**Nivo 2 (srednji) — Provera izvršivosti sposobnosti** (`abilityExecutable(abilityName)`)  
Poziva `resourceSufficient` da proveri resurse, a zatim proverava i dodatne uslove: sposobnost nije na punjenju (cooldown = 0) i zadovoljeni su posebni preduslovi (npr. aktivan stealth za Shadowstrike). Koristi se za generisanje dela lanca zaključivanja u izlazu.

**Nivo 1 (koren) — Ostvarivost strateškog cilja** (`rotationGoalAchievable(goal)`)  
Poziva `abilityExecutable` za sposobnosti koje su potrebne za dati strateški cilj:
- `BURST_WINDOW` — proverava da li je glavna cooldown sposobnost specijalizacije izvršiva (Recklessness / Eye Beam)
- `EXECUTE_PHASE_READY` — proverava `abilityExecutable("Execute")` i prisustvo `ExecutePhase` fakta (Fury Warrior)
- `FINISHER_READY` — proverava `abilityExecutable("Eviscerate")` i prisustvo `FinisherReady` fakta (Subtlety Rogue)
- `META_BURST_WINDOW` — proverava `abilityExecutable("EyeBeam")` i `abilityExecutable("Metamorphosis")` i odsustvo `MetaActive` fakta (Havoc DH)

Primer lanca za pitanje „Da li je burst window ostvariv?" (Fury Warrior):
```
rotationGoalAchievable("BURST_WINDOW")
  → abilityExecutable("Recklessness")
      → resourceSufficient("FURY_WARRIOR", "Recklessness")  ✓
      → cooldown == 0  ✓
  → true
```
Koristi se za generisanje upozorenja tipa „Recklessness dostupan za 1 potez — odloži Rampage" i za objašnjenje zašto određeni cilj trenutno nije ostvariv.

### Konkretan primer zaključivanja

**Situacija**: Subtlety Rogue, početak borbe (0,5 sekundi od pusha). Rogue je izašao iz predborbe u stealth-u i otvorio sa jednim Shadowstrike-om koji je probudio borbu.

Trenutno stanje:
- Energija: 82, Kombo poeni: 1 (od uvodnog Shadowstrike-a)
- Aktivni efekti: Slice and Dice (28s — primenjen u predborbi)
- Shadow Dance: dostupan (2 punjenja), još nije aktiviran
- Svi cooldowni dostupni: Flagellation (0s), Symbols of Death (0s)
- Rupture: ne tika na protivniku (borba tek počela)
- Protivnik: 100% zdravlja

*Napomena za čitaoca*: Slice and Dice je trajni efekat pojačanja koji mora uvek biti aktivan — ako istekne, rotacija se gasi. Flagellation je sposobnost koja aplicira debuff na protivnika i pojačava finalne udare; u praksi se koristi kao „okidač" koji usklađuje ostale glavne sposobnosti (Symbols of Death, Shadow Dance). Shadow Dance privremeno aktivira stealth mod i omogućava korišćenje Shadowstrike-a umesto slabijeg Backstab-a. Kombo poeni se troše finalnim udarima — optimalno je trošiti pri 5–7 poena.

---

**Korak 1 — Učitavanje stanja**  
Fakti `PlayerState`, `AbilityState`, `BuffState`, `DotState` i `Target` unose se u radnu memoriju Drools sesije.

**Korak 2 — Derivacija (1. nivo)**  
- Shadow Dance nije aktivan → `StealthWindowActive` se **ne** umeće  
- Kombo poeni (1) < prag za finalni udar (5) → `FinisherReady` se **ne** umeće  
- Slice and Dice trajanje (28s) > pandemic prag (2s) → `SndRefreshNeeded` se **ne** umeće  
- Rupture ne tika (0s) < pandemic prag (7s) → umeće se `RuptureRefreshNeeded` *(ali T7 neće pucati jer `FinisherReady` nije prisutan)*  
- Flagellation dostupna (cooldown = 0) → umeće se `FlagellationReady`  
- Symbols of Death dostupan (cooldown = 0) → umeće se `SymbolsReady`

**Korak 3 — CEP detekcija (Drools Fusion)**  
Drools Fusion proverava event stream: u poslednjih 6 sekundi nije detektovana kombinacija `FlagellationActivatedEvent` + `SymbolsOfDeathActivatedEvent` — borba tek počinje, nijedan event još nije emitovan. CEP ne puca.

**Korak 4 — Procena (2. nivo)**  
- T5: `FlagellationReady` je prisutan → umeće se `FlagellationPriority`  
- T6: `SndRefreshNeeded` nije prisutan → `PrioritizeSnd` se **ne** umeće  
- T7: `RuptureRefreshNeeded` je prisutan, ali `FinisherReady` nije → `PrioritizeRupture` se **ne** umeće  
- T8: `StealthWindowActive` nije prisutan → `SpendCPInDance` se **ne** umeće  
- T9: `SymbolsReady` je prisutan + `StealthWindowActive` nije → umeće se `PrepareBurstEntry`

**Korak 5 — Preporuka (3. nivo)**  
- Prioritet 120 (`PrioritizeSnd`): nije aktivan → preskočeno  
- Prioritet 110 (`PrioritizeRupture`): nije aktivan → preskočeno  
- Prioritet 100 (`FlagellationPriority`): **aktivan** → **preporučuje Flagellation**  
- *(Prioritet 95 `PrepareBurstEntry` bi preporučio Symbols of Death, ali 100 > 95)*

**Korak 6 — Backward chaining**  
Upit `rotationGoalAchievable("BURST_WINDOW")` razmišlja unazad:  
→ `abilityExecutable("SymbolsOfDeath")`  
&nbsp;&nbsp;&nbsp;&nbsp;→ `resourceSufficient("SUBTLETY_ROGUE", "SymbolsOfDeath")`: energija 82 ≥ 0 troška ✓  
&nbsp;&nbsp;&nbsp;&nbsp;→ cooldown = 0 ✓  
→ `true` — burst window ostvariv odmah nakon Flagellation

**Korak 7 — Accumulate**  
`accumulate` sabira preostalo trajanje svih aktivnih efekata: Slice and Dice (28s) → `TotalBuffDuration = 28s`. Proc događaji u poslednjih 3s: 0.

**Izlaz sistema**:
```json
{
  "primaryRecommendation": {
    "ability": "Flagellation",
    "urgency": "HIGH",
    "reasoning": [
      "FlagellationReady: cooldown = 0 — primarni cooldown dostupan",
      "Flagellation usklađuje Symbols of Death i Shadow Dance",
      "Slice and Dice aktivan (28s) — maintenance buff obezbeđen",
      "Burst window ostvariv odmah nakon Flagellation"
    ]
  },
  "nextAbilities": ["Symbols of Death", "Shadow Dance", "Shadowstrike"],
  "warnings": [],
  "activeEvents": []
}
```
