package com.faks.sbnz.wow_rotation_advisor.service;

import com.faks.sbnz.wow_rotation_advisor.events.*;
import com.faks.sbnz.wow_rotation_advisor.facts.*;
import com.faks.sbnz.wow_rotation_advisor.model.*;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.ObjectFilter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameStateService {

    private final KieContainer kieContainer;
    private KieSession droolsSession;

    private static final Set<Class<?>> EVENT_CLASSES = Set.of(
        ProcEvent.class,
        FinisherReadyEvent.class,
        ShadowDanceActivatedEvent.class,
        SymbolsActivatedEvent.class,
        EssenceBreakAppliedEvent.class,
        MetaActivatedEvent.class,
        ResourceHighEvent.class,
        AbilityUsedEvent.class,
        FinisherUsedEvent.class
    );

    private String currentSpec = "FURY_WARRIOR";

    private FuryWarriorState fwState;
    private Map<String, AbilityState> fwAbilities;

    private SubtletyRogueState srState;
    private Map<String, AbilityState> srAbilities;
    private Map<String, DotState> srDots;

    private HavocDHState dhState;
    private Map<String, AbilityState> dhAbilities;

    private TargetState targetState;
    private Recommendation currentRecommendation;
    private String warningMessage = "";
    private List<String> currentPrereqChain = new ArrayList<>();
    private List<String> cepAlerts = new ArrayList<>();

    private static final String[][] PREREQ_DATA = {
        {"Shadowstrike",     "Shadow Dance"     },
        {"Secret Technique", "Shadow Dance"     },
        {"Shadow Dance",     "Symbols of Death" },
        {"Shadow Blades",    "Symbols of Death" },
        {"Eviscerate",       "Backstab"         },
        {"Rupture",          "Backstab"         },
        {"Slice and Dice",   "Backstab"         },
        {"Flagellation",     "Symbols of Death" },
        {"Rampage",          "Bloodthirst"      },
        {"Recklessness",     "Bloodthirst"      },
        {"Avatar",           "Recklessness"     },
        {"Execute",          "Recklessness"     },
        {"Blade Dance",      "Essence Break"    },
        {"Chaos Strike",     "Essence Break"    },
        {"Essence Break",    "Metamorphosis"    },
        {"Eye Beam",         "Metamorphosis"    },
        {"Fel Devastation",  "Metamorphosis"    },
    };

    private static final int BASE_RAGE_PER_SECOND   = 4;
    private static final int BASE_ENERGY_PER_SECOND = 10;
    private static final int BASE_FURY_PER_SECOND   = 4;

    public GameStateService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
        reset();
    }

    @PreDestroy
    public void cleanup() {
        if (droolsSession != null) droolsSession.dispose();
    }

    public synchronized void reset() {
        if (droolsSession != null) droolsSession.dispose();
        droolsSession = kieContainer.newKieSession("rotationSession");

        fwState = new FuryWarriorState();
        fwAbilities = new LinkedHashMap<>();
        fwAbilities.put("Bloodthirst",  new AbilityState("Bloodthirst",  4.5,   1));
        fwAbilities.put("Raging Blow",  new AbilityState("Raging Blow",  8.0,   2));
        fwAbilities.put("Rampage",      new AbilityState("Rampage",      0.0,   1));
        fwAbilities.put("Execute",      new AbilityState("Execute",      6.0,   1));
        fwAbilities.put("Recklessness", new AbilityState("Recklessness", 90.0,  1));
        fwAbilities.put("Avatar",       new AbilityState("Avatar",       90.0,  1));
        fwAbilities.put("Odyn's Fury",  new AbilityState("Odyn's Fury",  60.0,  1));

        srState = new SubtletyRogueState();
        srAbilities = new LinkedHashMap<>();
        srAbilities.put("Shadowstrike",     new AbilityState("Shadowstrike",     0.0,   1));
        srAbilities.put("Backstab",         new AbilityState("Backstab",         0.0,   1));
        srAbilities.put("Eviscerate",       new AbilityState("Eviscerate",       0.0,   1));
        srAbilities.put("Rupture",          new AbilityState("Rupture",          0.0,   1));
        srAbilities.put("Slice and Dice",   new AbilityState("Slice and Dice",   0.0,   1));
        srAbilities.put("Shadow Dance",     new AbilityState("Shadow Dance",     60.0,  2));
        srAbilities.put("Symbols of Death", new AbilityState("Symbols of Death", 38.0,  1));
        srAbilities.put("Flagellation",     new AbilityState("Flagellation",     30.0,  1));
        srAbilities.put("Secret Technique", new AbilityState("Secret Technique", 45.0,  1));
        srAbilities.put("Shadow Blades",    new AbilityState("Shadow Blades",    180.0, 1));
        srDots = new LinkedHashMap<>();
        srDots.put("Rupture", new DotState("Rupture", 4.2));

        dhState = new HavocDHState();
        dhAbilities = new LinkedHashMap<>();
        dhAbilities.put("Demon's Bite",    new AbilityState("Demon's Bite",    0.0,   1));
        dhAbilities.put("Chaos Strike",    new AbilityState("Chaos Strike",    0.0,   1));
        dhAbilities.put("Blade Dance",     new AbilityState("Blade Dance",     9.0,   1));
        dhAbilities.put("Eye Beam",        new AbilityState("Eye Beam",        30.0,  1));
        dhAbilities.put("Essence Break",   new AbilityState("Essence Break",   15.0,  1));
        dhAbilities.put("Metamorphosis",   new AbilityState("Metamorphosis",   180.0, 1));
        dhAbilities.put("Immolation Aura", new AbilityState("Immolation Aura", 30.0,  1));
        dhAbilities.put("Felblade",        new AbilityState("Felblade",        30.0,  2));
        dhAbilities.put("Fel Devastation", new AbilityState("Fel Devastation", 30.0,  1));

        targetState = new TargetState(100.0);
        warningMessage = "";
        cepAlerts = new ArrayList<>();
        runDrools();
    }

    @Scheduled(fixedDelay = 1000)
    public synchronized void tick() {
        switch (currentSpec) {
            case "FURY_WARRIOR"   -> tickFW();
            case "SUBTLETY_ROGUE" -> tickSR();
            case "HAVOC_DH"       -> tickDH();
        }
        runDrools();
    }

    private void tickFW() {
        int rageGen = fwState.isRecklessnessActive()
                ? (int) (BASE_RAGE_PER_SECOND * 1.5) : BASE_RAGE_PER_SECOND;
        fwState.setPrimaryResource(fwState.getPrimaryResource() + rageGen);

        if (fwState.isEnrageActive()) {
            double rem = fwState.getEnrageRemainingSeconds() - 1.0;
            if (rem <= 0) { fwState.setEnrageActive(false); fwState.setEnrageRemainingSeconds(0); }
            else fwState.setEnrageRemainingSeconds(rem);
        }
        if (fwState.isRecklessnessActive()) {
            double rem = fwState.getRecklessnessRemainingSeconds() - 1.0;
            if (rem <= 0) { fwState.setRecklessnessActive(false); fwState.setRecklessnessRemainingSeconds(0); }
            else fwState.setRecklessnessRemainingSeconds(rem);
        }
        if (fwState.isAvatarActive()) {
            double rem = fwState.getAvatarRemainingSeconds() - 1.0;
            if (rem <= 0) { fwState.setAvatarActive(false); fwState.setAvatarRemainingSeconds(0); }
            else fwState.setAvatarRemainingSeconds(rem);
        }
        fwAbilities.values().forEach(a -> a.tick(1.0));

        if (!fwState.isSuddenDeathProcActive() && Math.random() < 0.03) {
            fwState.setSuddenDeathProcActive(true);
            droolsSession.insert(new ProcEvent("FURY_WARRIOR", "SuddenDeath"));
        }
        if (fwState.getPrimaryResource() >= 80) {
            droolsSession.insert(new ResourceHighEvent("FURY_WARRIOR"));
        }
    }

    private void tickSR() {
        srState.setPrimaryResource(srState.getPrimaryResource() + BASE_ENERGY_PER_SECOND);

        if (srState.isShadowDanceActive()) {
            double rem = srState.getShadowDanceRemainingSeconds() - 1.0;
            if (rem <= 0) { srState.setShadowDanceActive(false); srState.setShadowDanceRemainingSeconds(0); }
            else srState.setShadowDanceRemainingSeconds(rem);
        }
        if (srState.isSymbolsOfDeathActive()) {
            double rem = srState.getSymbolsOfDeathRemainingSeconds() - 1.0;
            if (rem <= 0) { srState.setSymbolsOfDeathActive(false); srState.setSymbolsOfDeathRemainingSeconds(0); }
            else srState.setSymbolsOfDeathRemainingSeconds(rem);
        }
        if (srState.isSliceAndDiceActive()) {
            double rem = srState.getSliceAndDiceRemainingSeconds() - 1.0;
            if (rem <= 0) { srState.setSliceAndDiceActive(false); srState.setSliceAndDiceRemainingSeconds(0); }
            else srState.setSliceAndDiceRemainingSeconds(rem);
        }
        if (srState.isFlagellationActive()) {
            double rem = srState.getFlagellationRemainingSeconds() - 1.0;
            if (rem <= 0) { srState.setFlagellationActive(false); srState.setFlagellationRemainingSeconds(0); }
            else srState.setFlagellationRemainingSeconds(rem);
        }
        if (srState.isShadowBladesActive()) {
            double rem = srState.getShadowBladesRemainingSeconds() - 1.0;
            if (rem <= 0) { srState.setShadowBladesActive(false); srState.setShadowBladesRemainingSeconds(0); }
            else srState.setShadowBladesRemainingSeconds(rem);
        }
        srAbilities.values().forEach(a -> a.tick(1.0));
        srDots.values().forEach(d -> d.tick(1.0));
    }

    private void tickDH() {
        int furyGen = BASE_FURY_PER_SECOND;
        if (dhState.isImmolationAuraActive()) furyGen += 8;
        dhState.setPrimaryResource(dhState.getPrimaryResource() + furyGen);

        if (dhState.isMetamorphosisActive()) {
            double rem = dhState.getMetamorphosisRemainingSeconds() - 1.0;
            if (rem <= 0) { dhState.setMetamorphosisActive(false); dhState.setMetamorphosisRemainingSeconds(0); }
            else dhState.setMetamorphosisRemainingSeconds(rem);
        }
        if (dhState.isDemonicActive()) {
            double rem = dhState.getDemonicRemainingSeconds() - 1.0;
            if (rem <= 0) { dhState.setDemonicActive(false); dhState.setDemonicRemainingSeconds(0); }
            else dhState.setDemonicRemainingSeconds(rem);
        }
        if (dhState.isImmolationAuraActive()) {
            double rem = dhState.getImmolationAuraRemainingSeconds() - 1.0;
            if (rem <= 0) { dhState.setImmolationAuraActive(false); dhState.setImmolationAuraRemainingSeconds(0); }
            else dhState.setImmolationAuraRemainingSeconds(rem);
        }
        if (dhState.isEssenceBreakActive()) {
            double rem = dhState.getEssenceBreakRemainingSeconds() - 1.0;
            if (rem <= 0) { dhState.setEssenceBreakActive(false); dhState.setEssenceBreakRemainingSeconds(0); }
            else dhState.setEssenceBreakRemainingSeconds(rem);
        }
        dhAbilities.values().forEach(a -> a.tick(1.0));

        if (dhState.getPrimaryResource() >= 100) {
            droolsSession.insert(new ResourceHighEvent("HAVOC_DH"));
        }
    }

    public synchronized String useAbility(String abilityName) {
        String result = switch (currentSpec) {
            case "FURY_WARRIOR"   -> useFWAbility(abilityName);
            case "SUBTLETY_ROGUE" -> useSRAbility(abilityName);
            case "HAVOC_DH"       -> useDHAbility(abilityName);
            default -> "Nepoznati spec";
        };
        if ("OK".equals(result)) {
            droolsSession.insert(new AbilityUsedEvent(currentSpec, abilityName));
            if (currentSpec.equals("SUBTLETY_ROGUE") &&
                    (abilityName.equals("Eviscerate") || abilityName.equals("Rupture") || abilityName.equals("Slice and Dice"))) {
                droolsSession.insert(new FinisherUsedEvent());
            }
            runDrools();
        }
        return result;
    }

    private String useFWAbility(String name) {
        AbilityState a = fwAbilities.get(name);
        if (a == null) return "Nepoznata sposobnost";
        switch (name) {
            case "Bloodthirst" -> {
                if (!a.isReady()) return "Bloodthirst je na punjenju";
                fwState.setPrimaryResource(fwState.getPrimaryResource() + 8);
                a.setCooldownRemaining(4.5);
                if (Math.random() < 0.30) {
                    applyFWEnrage();
                    droolsSession.insert(new ProcEvent("FURY_WARRIOR", "EnrageProc"));
                }
            }
            case "Raging Blow" -> {
                if (a.getChargesAvailable() == 0) return "Raging Blow nema punjenja";
                fwState.setPrimaryResource(fwState.getPrimaryResource() + 12);
                a.useCharge();
            }
            case "Rampage" -> {
                if (fwState.getPrimaryResource() < 80) return "Nedovoljno Rage-a za Rampage (treba 80)";
                fwState.setPrimaryResource(fwState.getPrimaryResource() - 80);
                applyFWEnrage();
            }
            case "Execute" -> {
                if (!a.isReady()) return "Execute je na punjenju";
                boolean sd  = fwState.isSuddenDeathProcActive();
                boolean low = targetState.getHpPercent() < 20;
                if (!sd && !low) return "Execute nije dostupan (HP protivnika > 20%)";
                int cost = sd ? 0 : 25;
                if (fwState.getPrimaryResource() < cost) return "Nedovoljno Rage-a za Execute";
                fwState.setPrimaryResource(fwState.getPrimaryResource() - cost + 40);
                fwState.setSuddenDeathProcActive(false);
                a.setCooldownRemaining(6.0);
            }
            case "Recklessness" -> {
                if (!a.isReady()) return "Recklessness je na punjenju";
                fwState.setRecklessnessActive(true);
                fwState.setRecklessnessRemainingSeconds(12.0);
                a.setCooldownRemaining(90.0);
            }
            case "Avatar" -> {
                if (!a.isReady()) return "Avatar je na punjenju";
                fwState.setAvatarActive(true);
                fwState.setAvatarRemainingSeconds(20.0);
                a.setCooldownRemaining(90.0);
            }
            case "Odyn's Fury" -> {
                if (!a.isReady()) return "Odyn's Fury je na punjenju";
                a.setCooldownRemaining(60.0);
            }
            default -> { return "Nepoznata sposobnost"; }
        }
        return "OK";
    }

    private void applyFWEnrage() {
        fwState.setEnrageActive(true);
        fwState.setEnrageRemainingSeconds(4.0);
    }

    private String useSRAbility(String name) {
        AbilityState a = srAbilities.get(name);
        if (a == null) return "Nepoznata sposobnost";
        switch (name) {
            case "Shadowstrike" -> {
                boolean hasStealth = srState.isShadowDanceActive() || srState.isShadowBladesActive();
                if (!hasStealth) return "Shadowstrike zahteva Shadow Dance ili Shadow Blades";
                if (srState.getPrimaryResource() < 40) return "Nedovoljno Energy-ja za Shadowstrike (treba 40)";
                srState.setPrimaryResource(srState.getPrimaryResource() - 40);
                int cpBefore = srState.getComboPoints();
                srState.setComboPoints(cpBefore + 2);
                if (cpBefore < 5 && srState.getComboPoints() >= 5) {
                    droolsSession.insert(new FinisherReadyEvent());
                }
            }
            case "Backstab" -> {
                if (srState.getPrimaryResource() < 35) return "Nedovoljno Energy-ja za Backstab (treba 35)";
                srState.setPrimaryResource(srState.getPrimaryResource() - 35);
                int cpBefore = srState.getComboPoints();
                srState.setComboPoints(cpBefore + 2);
                if (cpBefore < 5 && srState.getComboPoints() >= 5) {
                    droolsSession.insert(new FinisherReadyEvent());
                }
            }
            case "Eviscerate" -> {
                if (srState.getComboPoints() < 1) return "Nema Combo Points za Eviscerate";
                if (srState.getPrimaryResource() < 35) return "Nedovoljno Energy-ja za Eviscerate (treba 35)";
                srState.setPrimaryResource(srState.getPrimaryResource() - 35);
                srState.setComboPoints(0);
                if (srState.isFlagellationActive())
                    srState.setFlagellationStacks(srState.getFlagellationStacks() + 1);
            }
            case "Rupture" -> {
                if (srState.getComboPoints() < 5) return "Rupture zahteva 5+ Combo Points";
                if (srState.getPrimaryResource() < 25) return "Nedovoljno Energy-ja za Rupture (treba 25)";
                int cp = srState.getComboPoints();
                srState.setPrimaryResource(srState.getPrimaryResource() - 25);
                srState.setComboPoints(0);
                srDots.get("Rupture").apply(8.0 + cp * 2.0);
                if (srState.isFlagellationActive())
                    srState.setFlagellationStacks(srState.getFlagellationStacks() + 1);
            }
            case "Slice and Dice" -> {
                if (srState.getComboPoints() < 2) return "Slice and Dice zahteva najmanje 2 Combo Poena";
                if (srState.getPrimaryResource() < 25) return "Nedovoljno Energy-ja za Slice and Dice (treba 25)";
                int cp = srState.getComboPoints();
                srState.setPrimaryResource(srState.getPrimaryResource() - 25);
                double duration = 6.0 + cp * 2.0;
                if (duration > srState.getSliceAndDiceRemainingSeconds()) {
                    srState.setSliceAndDiceActive(true);
                    srState.setSliceAndDiceRemainingSeconds(duration);
                }
                srState.setComboPoints(0);
                if (srState.isFlagellationActive())
                    srState.setFlagellationStacks(srState.getFlagellationStacks() + 1);
            }
            case "Flagellation" -> {
                if (!a.isReady()) return "Flagellation je na punjenju";
                a.setCooldownRemaining(30.0);
                srState.setFlagellationActive(true);
                srState.setFlagellationRemainingSeconds(12.0);
                srState.setFlagellationStacks(0);
            }
            case "Secret Technique" -> {
                if (!a.isReady()) return "Secret Technique je na punjenju";
                if (!srState.isShadowDanceActive()) return "Secret Technique zahteva Shadow Dance (stealth)";
                a.setCooldownRemaining(45.0);
            }
            case "Shadow Dance" -> {
                if (a.getChargesAvailable() == 0) return "Shadow Dance nema punjenja";
                a.useCharge();
                srState.setShadowDanceActive(true);
                srState.setShadowDanceRemainingSeconds(8.0);
                droolsSession.insert(new ShadowDanceActivatedEvent());
            }
            case "Symbols of Death" -> {
                if (!a.isReady()) return "Symbols of Death je na punjenju";
                a.setCooldownRemaining(38.0);
                srState.setSymbolsOfDeathActive(true);
                srState.setSymbolsOfDeathRemainingSeconds(34.0);
                droolsSession.insert(new SymbolsActivatedEvent());
            }
            case "Shadow Blades" -> {
                if (!a.isReady()) return "Shadow Blades je na punjenju";
                a.setCooldownRemaining(180.0);
                srState.setShadowBladesActive(true);
                srState.setShadowBladesRemainingSeconds(20.0);
            }
            default -> { return "Nepoznata sposobnost"; }
        }
        return "OK";
    }

    private String useDHAbility(String name) {
        AbilityState a = dhAbilities.get(name);
        if (a == null) return "Nepoznata sposobnost";
        switch (name) {
            case "Demon's Bite" -> {
                dhState.setPrimaryResource(dhState.getPrimaryResource() + 20);
            }
            case "Chaos Strike" -> {
                if (dhState.getPrimaryResource() < 40) return "Nedovoljno Fury-ja za Chaos Strike (treba 40)";
                dhState.setPrimaryResource(dhState.getPrimaryResource() - 40);
                if (Math.random() < 0.20) {
                    dhState.setPrimaryResource(dhState.getPrimaryResource() + 20);
                    droolsSession.insert(new ProcEvent("HAVOC_DH", "ChaosStrikeRefund"));
                }
            }
            case "Blade Dance" -> {
                if (!a.isReady()) return "Blade Dance je na punjenju";
                if (dhState.getPrimaryResource() < 35) return "Nedovoljno Fury-ja za Blade Dance (treba 35)";
                dhState.setPrimaryResource(dhState.getPrimaryResource() - 35);
                a.setCooldownRemaining(9.0);
            }
            case "Eye Beam" -> {
                if (!a.isReady()) return "Eye Beam je na punjenju";
                if (dhState.getPrimaryResource() < 30) return "Nedovoljno Fury-ja za Eye Beam (treba 30)";
                dhState.setPrimaryResource(dhState.getPrimaryResource() - 30);
                a.setCooldownRemaining(30.0);
                dhState.setDemonicActive(true);
                dhState.setDemonicRemainingSeconds(6.0);
            }
            case "Metamorphosis" -> {
                if (!a.isReady()) return "Metamorphosis je na punjenju";
                a.setCooldownRemaining(180.0);
                dhState.setMetamorphosisActive(true);
                dhState.setMetamorphosisRemainingSeconds(30.0);
                droolsSession.insert(new MetaActivatedEvent());
            }
            case "Essence Break" -> {
                if (!a.isReady()) return "Essence Break je na punjenju";
                a.setCooldownRemaining(15.0);
                dhState.setEssenceBreakActive(true);
                dhState.setEssenceBreakRemainingSeconds(4.0);
                droolsSession.insert(new EssenceBreakAppliedEvent());
            }
            case "Immolation Aura" -> {
                if (!a.isReady()) return "Immolation Aura je na punjenju";
                a.setCooldownRemaining(30.0);
                dhState.setImmolationAuraActive(true);
                dhState.setImmolationAuraRemainingSeconds(6.0);
            }
            case "Felblade" -> {
                if (a.getChargesAvailable() == 0) return "Felblade nema punjenja";
                a.useCharge();
                dhState.setPrimaryResource(dhState.getPrimaryResource() + 20);
            }
            case "Fel Devastation" -> {
                if (!a.isReady()) return "Fel Devastation je na punjenju";
                if (dhState.getPrimaryResource() < 50) return "Nedovoljno Fury-ja za Fel Devastation (treba 50)";
                dhState.setPrimaryResource(dhState.getPrimaryResource() - 50);
                a.setCooldownRemaining(30.0);
            }
            default -> { return "Nepoznata sposobnost"; }
        }
        return "OK";
    }

    private void cleanupNonEvents() {
        List<Object> toRetract = new ArrayList<>();
        droolsSession.getObjects().forEach(o -> {
            if (!EVENT_CLASSES.contains(o.getClass())) {
                toRetract.add(o);
            }
        });
        toRetract.forEach(o -> droolsSession.delete(droolsSession.getFactHandle(o)));
    }

    private void runDrools() {
        cleanupNonEvents();

        switch (currentSpec) {
            case "FURY_WARRIOR" -> {
                droolsSession.insert(fwState);
                fwAbilities.values().forEach(droolsSession::insert);
            }
            case "SUBTLETY_ROGUE" -> {
                droolsSession.insert(srState);
                srAbilities.values().forEach(droolsSession::insert);
                srDots.values().forEach(droolsSession::insert);
            }
            case "HAVOC_DH" -> {
                droolsSession.insert(dhState);
                dhAbilities.values().forEach(droolsSession::insert);
            }
        }
        droolsSession.insert(targetState);
        for (String[] p : PREREQ_DATA)
            droolsSession.insert(new Prerequisite(p[0], p[1]));

        droolsSession.fireAllRules();

        Collection<?> recs = droolsSession.getObjects(
                (ObjectFilter) obj -> obj instanceof Recommendation);
        currentRecommendation = recs.stream()
                .map(r -> (Recommendation) r)
                .max(Comparator.comparingInt(Recommendation::getPriority))
                .orElse(defaultRecommendation());

        currentPrereqChain = buildPrereqChain(currentRecommendation.getAbilityName());
        warningMessage = buildWarning();

        List<String> alerts = new ArrayList<>();
        droolsSession.getObjects().forEach(o -> {
            if (o instanceof ProcStormActive p && p.getSpec().equals(currentSpec))
                alerts.add("Proc Storm — " + p.getMessage());
            else if (o instanceof MaxBurstActive m && currentSpec.equals("SUBTLETY_ROGUE"))
                alerts.add("Max Burst — " + m.getMessage());
            else if (o instanceof EBMetaSynergyActive e && currentSpec.equals("HAVOC_DH"))
                alerts.add("EB+Meta — " + e.getMessage());
            else if (o instanceof ResourceBleedActive r && r.getSpec().equals(currentSpec))
                alerts.add("Resource Bleed — " + r.getMessage());
            else if (o instanceof RecklessnessWastedAlert rwa && currentSpec.equals("FURY_WARRIOR"))
                alerts.add("Propusteno — " + rwa.getMessage());
            else if (o instanceof DanceWindowWastedAlert dwa && currentSpec.equals("SUBTLETY_ROGUE"))
                alerts.add("Propusteno — " + dwa.getMessage());
            else if (o instanceof TripleCDConvergenceActive tc && currentSpec.equals("SUBTLETY_ROGUE"))
                alerts.add("Triple CD — " + tc.getMessage());
            else if (o instanceof MetaOpenerMissedAlert moa && currentSpec.equals("HAVOC_DH"))
                alerts.add("Propusteno — " + moa.getMessage());
            else if (o instanceof ChaosStrikeFrenzyActive cfa && currentSpec.equals("HAVOC_DH"))
                alerts.add("CS Frenzy — " + cfa.getMessage());
            else if (o instanceof BurstSequenceComplete bsc && bsc.getSpec().equals(currentSpec))
                alerts.add("Burst Seq — " + bsc.getMessage());
        });
        cepAlerts = alerts;
    }

    private List<String> buildPrereqChain(String ability) {
        Collection<?> chains = droolsSession.getObjects(
                (ObjectFilter) obj -> obj instanceof PrereqChain
                        && ((PrereqChain) obj).getFromAbility().equals(ability));
        return chains.stream()
                .map(o -> (PrereqChain) o)
                .sorted(Comparator.comparingInt(PrereqChain::getDepth).reversed())
                .map(PrereqChain::getToAbility)
                .collect(Collectors.toList());
    }

    private Recommendation defaultRecommendation() {
        return switch (currentSpec) {
            case "SUBTLETY_ROGUE" -> new Recommendation("Backstab",     10, "LOW", "Filler");
            case "HAVOC_DH"       -> new Recommendation("Chaos Strike", 10, "LOW", "Filler");
            default               -> new Recommendation("Raging Blow",  10, "LOW", "Filler");
        };
    }

    private String buildWarning() {
        return switch (currentSpec) {
            case "FURY_WARRIOR"   -> buildFWWarning();
            case "SUBTLETY_ROGUE" -> buildSRWarning();
            case "HAVOC_DH"       -> buildDHWarning();
            default -> "";
        };
    }

    private String buildFWWarning() {
        if (fwState.getPrimaryResource() >= 95)
            return "KRITIČNO: Rage na maksimumu — odmah iskoristi Rampage!";
        if (fwState.isEnrageActive() && fwState.getEnrageRemainingSeconds() <= 1.5)
            return "UPOZORENJE: Enrage isce za " + String.format("%.0f", fwState.getEnrageRemainingSeconds()) + "s!";
        if (fwState.isRecklessnessActive() && fwState.isAvatarActive())
            return "AVATAR+RECKLESSNESS: Maks burst prozor aktivan — trosi Rampage i Odyn's Fury odmah!";
        if (fwState.isRecklessnessActive() && !fwState.isEnrageActive())
            return "UPOZORENJE: Recklessness aktivan ali Enrage nije — iskoristi Bloodthirst!";
        return "";
    }

    private String buildSRWarning() {
        if (srState.getPrimaryResource() >= 95)
            return "KRITIČNO: Energy na maksimumu — troši sposobnost odmah!";
        if (srState.isSliceAndDiceActive() && srState.getSliceAndDiceRemainingSeconds() <= 2.0 && srState.getComboPoints() >= 5)
            return "UPOZORENJE: Slice and Dice isce — osvezi odmah!";
        if (srState.isShadowDanceActive() && srState.getShadowDanceRemainingSeconds() <= 2.0)
            return "UPOZORENJE: Shadow Dance isce za " + String.format("%.0f", srState.getShadowDanceRemainingSeconds()) + "s!";
        DotState rupture = srDots.get("Rupture");
        if (rupture.isNeedsRefresh() && srState.getComboPoints() >= 5)
            return "UPOZORENJE: Rupture treba refresh i imas 5+ CP — iskoristi Rupture!";
        return "";
    }

    private String buildDHWarning() {
        if (dhState.getPrimaryResource() >= 110)
            return "KRITIČNO: Fury pri maksimumu — odmah iskoristi Chaos Strike!";
        if (dhState.isDemonicActive() && dhState.getDemonicRemainingSeconds() <= 2.0)
            return "UPOZORENJE: Demonic buff isce za " + String.format("%.0f", dhState.getDemonicRemainingSeconds()) + "s!";
        return "";
    }

    public synchronized void setSpec(String spec) {
        this.currentSpec = spec;
        runDrools();
    }

    public synchronized void setTargetHp(double hp) {
        targetState.setHpPercent(hp);
        runDrools();
    }

    public synchronized String getCurrentSpec()                    { return currentSpec; }

    public synchronized PlayerState getPlayerState() {
        return switch (currentSpec) {
            case "SUBTLETY_ROGUE" -> srState;
            case "HAVOC_DH"       -> dhState;
            default               -> fwState;
        };
    }

    public synchronized Map<String, AbilityState> getAbilities() {
        return switch (currentSpec) {
            case "SUBTLETY_ROGUE" -> srAbilities;
            case "HAVOC_DH"       -> dhAbilities;
            default               -> fwAbilities;
        };
    }

    public synchronized Map<String, DotState> getDots() {
        return "SUBTLETY_ROGUE".equals(currentSpec) ? srDots : Collections.emptyMap();
    }

    public synchronized TargetState getTargetState()              { return targetState; }
    public synchronized Recommendation getCurrentRecommendation() { return currentRecommendation; }
    public synchronized String getWarningMessage()                { return warningMessage; }
    public synchronized List<String> getCurrentPrereqChain()      { return currentPrereqChain; }
    public synchronized List<String> getCepAlerts()               { return cepAlerts; }
}
