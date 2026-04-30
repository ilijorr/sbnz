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
| Shadow Priest | Madness (0–100) | Upravljanje višestrukim vremenskim efektima na protivniku, proc mehanike |

### Konkretan primer zaključivanja

**Situacija**: Subtlety Rogue, 45 Energije, 6 Kombo poena od maksimalnih 7.  
Aktivni efekti: Shadow Dance (preostalo 3,2 sekunde) i Symbols of Death (preostalo 2,1 sekunde).  
Efekat krvarenja (Rupture) na protivniku: 14,2 sekunde preostalog trajanja.

*Napomena*: Shadow Dance privremeno aktivira stealth mod i omogućava korišćenje snažnijeg napada (Shadowstrike) umesto standardnog. Kombo poeni se troše finalnim udarima (Eviscerate) — optimalno trošenje je pri 5–7 poena. Symbols of Death pojačava finalne udare. Rupture je vremenski efekat koji se mora obnavljati pre isteka — obnavljanje je neophodno tek kad preostane manje od ~30% trajanja (tzv. pandemic prag).

---

**Korak 1 — Učitavanje stanja**  
Fakti o stanju igrača, dostupnosti sposobnosti, aktivnim efektima i stanju protivnika unose se u radnu memoriju Drools sesije.

**Korak 2 — Derivacija (1. nivo)**  
- Shadow Dance je aktivan → umeće se `StealthWindowActive`  
- Kombo poeni (6) ≥ prag za finalni udar (5) → umeće se `FinisherReady`  
- Rupture trajanje (14,2s) > pandemic prag (7,0s) → `RuptureRefreshNeeded` se **ne** umeće  
- Symbols of Death nije dostupan (cooldown 9,8s > 0) → `SymbolsReady` se **ne** umeće

**Korak 3 — CEP detekcija (Drools Fusion)**  
Drools Fusion detektuje da su `ShadowDanceActivatedEvent` i `SymbolsOfDeathActivatedEvent` nastali u istom klizećem prozoru od 6 sekundi → emituje `DanceSymbolsSynergyEvent` → umeće se `MaxBurstActive` u radnu memoriju

**Korak 4 — Procena (2. nivo)**  
- `StealthWindowActive` + `FinisherReady` → umeće se `SpendCPInDance`  
- `RuptureRefreshNeeded` nije prisutan → `PrioritizeRupture` se **ne** umeće

**Korak 5 — Preporuka (3. nivo)**  
- Pravilo s prioritetom 110 (`PrioritizeRupture`): nije aktivno → preskočeno  
- Pravilo s prioritetom 90 (`SpendCPInDance` aktivan, `PrioritizeRupture` nije): **preporučuje Eviscerate**

**Korak 6 — Backward chaining**  
Upit `isAbilityUsable("Eviscerate")` potvrđuje uslove unazad: kombo poeni ≥ prag ∧ sposobnost nije na punjenju → `true`

**Korak 7 — Accumulate**  
`accumulate` sabira preostalo trajanje svih aktivnih efekata: 3,2 + 2,1 = 5,3s → `TotalBuffDuration`; prebrojava proc događaje u poslednjih 3s → 0, Proc Storm se ne aktivira.

**Izlaz sistema**:
```json
{
  "primaryRecommendation": {
    "ability": "Eviscerate",
    "urgency": "HIGH",
    "reasoning": [
      "Shadow Dance aktivan (3.2s preostalo) — stealth prozor otvoren",
      "Symbols of Death aktivan — pojačava finalne udare",
      "Kombo poeni: 6/7 — prag finalnog udara dostignut",
      "Rupture ne zahteva obnavljanje (14.2s > pandemic prag)"
    ]
  },
  "warnings": [
    "Shadow Dance ističe za 3.2s",
    "Symbols of Death ističe za 2.1s — iskoristi burst prozor odmah"
  ],
  "activeEvents": ["DanceSymbolsSynergyEvent"]
}
```
