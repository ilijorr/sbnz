package com.faks.sbnz.wow_rotation_advisor.service;

import com.faks.sbnz.wow_rotation_advisor.facts.Recommendation;
import com.faks.sbnz.wow_rotation_advisor.model.AbilityState;
import com.faks.sbnz.wow_rotation_advisor.model.PlayerState;
import com.faks.sbnz.wow_rotation_advisor.model.TargetState;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.ObjectFilter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GameStateService {

    private final KieContainer kieContainer;

    private PlayerState playerState;
    private Map<String, AbilityState> abilities;
    private TargetState targetState;
    private Recommendation currentRecommendation;
    private String warningMessage;

    private static final int BASE_RAGE_PER_SECOND = 4;

    public GameStateService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
        reset();
    }

    public synchronized void reset() {
        playerState = new PlayerState();
        targetState = new TargetState(100.0);
        abilities = new LinkedHashMap<>();
        abilities.put("Bloodthirst",  new AbilityState("Bloodthirst",  4.5,  1));
        abilities.put("Raging Blow",  new AbilityState("Raging Blow",  8.0,  2));
        abilities.put("Rampage",      new AbilityState("Rampage",      0.0,  1));
        abilities.put("Execute",      new AbilityState("Execute",      6.0,  1));
        abilities.put("Recklessness", new AbilityState("Recklessness", 90.0, 1));
        warningMessage = "";
        runDrools();
    }

    @Scheduled(fixedDelay = 1000)
    public synchronized void tick() {
        int rageGen = playerState.isRecklessnessActive()
                ? (int) (BASE_RAGE_PER_SECOND * 1.5)
                : BASE_RAGE_PER_SECOND;
        playerState.setRage(playerState.getRage() + rageGen);

        if (playerState.isEnrageActive()) {
            double newEnrage = playerState.getEnrageRemainingSeconds() - 1.0;
            if (newEnrage <= 0) {
                playerState.setEnrageActive(false);
                playerState.setEnrageRemainingSeconds(0);
            } else {
                playerState.setEnrageRemainingSeconds(newEnrage);
            }
        }

        if (playerState.isRecklessnessActive()) {
            double newReck = playerState.getRecklessnessRemainingSeconds() - 1.0;
            if (newReck <= 0) {
                playerState.setRecklessnessActive(false);
                playerState.setRecklessnessRemainingSeconds(0);
            } else {
                playerState.setRecklessnessRemainingSeconds(newReck);
            }
        }

        abilities.values().forEach(a -> a.tick(1.0));

        if (!playerState.isSuddenDeathProcActive() && Math.random() < 0.03) {
            playerState.setSuddenDeathProcActive(true);
        }

        runDrools();
    }

    public synchronized String useAbility(String abilityName) {
        AbilityState ability = abilities.get(abilityName);
        if (ability == null) return "Nepoznata sposobnost";

        switch (abilityName) {
            case "Bloodthirst" -> {
                if (!ability.isReady()) return "Bloodthirst je na punjenju";
                playerState.setRage(playerState.getRage() + 8);
                ability.setCooldownRemaining(4.5);
                if (Math.random() < 0.30) applyEnrage();
            }
            case "Raging Blow" -> {
                if (ability.getChargesAvailable() == 0) return "Raging Blow nema punjenja";
                playerState.setRage(playerState.getRage() + 12);
                ability.useCharge();
                if (Math.random() < 0.25) {
                    ability.setChargesAvailable(ability.getChargesAvailable() + 1);
                }
            }
            case "Rampage" -> {
                if (playerState.getRage() < 80) return "Nedovoljno Rage-a za Rampage (treba 80)";
                playerState.setRage(playerState.getRage() - 80);
                applyEnrage();
            }
            case "Execute" -> {
                if (!ability.isReady()) return "Execute je na punjenju";
                boolean suddenDeath = playerState.isSuddenDeathProcActive();
                boolean lowHp = targetState.getHpPercent() < 20;
                if (!suddenDeath && !lowHp) return "Execute nije dostupan (HP protivnika > 20%)";
                int cost = suddenDeath ? 0 : 25;
                if (playerState.getRage() < cost) return "Nedovoljno Rage-a za Execute";
                playerState.setRage(playerState.getRage() - cost + 40);
                playerState.setSuddenDeathProcActive(false);
                ability.setCooldownRemaining(6.0);
            }
            case "Recklessness" -> {
                if (!ability.isReady()) return "Recklessness je na punjenju";
                playerState.setRecklessnessActive(true);
                playerState.setRecklessnessRemainingSeconds(12.0);
                ability.setCooldownRemaining(90.0);
            }
            default -> { return "Nepoznata sposobnost"; }
        }

        runDrools();
        return "OK";
    }

    private void applyEnrage() {
        playerState.setEnrageActive(true);
        playerState.setEnrageRemainingSeconds(4.0);
    }

    private void runDrools() {
        KieSession kieSession = kieContainer.newKieSession("furyWarriorSession");
        try {
            kieSession.insert(playerState);
            abilities.values().forEach(kieSession::insert);
            kieSession.insert(targetState);
            kieSession.fireAllRules();

            Collection<?> recs = kieSession.getObjects(
                    (ObjectFilter) obj -> obj instanceof Recommendation);

            currentRecommendation = recs.stream()
                    .map(r -> (Recommendation) r)
                    .max(Comparator.comparingInt(Recommendation::getPriority))
                    .orElse(new Recommendation("Raging Blow", 10, "LOW", "Filler"));

            warningMessage = buildWarning();
        } finally {
            kieSession.dispose();
        }
    }

    private String buildWarning() {
        if (playerState.getRage() >= 95) return "KRITIČNO: Rage na maksimumu — odmah iskoristi Rampage!";
        if (playerState.isEnrageActive() && playerState.getEnrageRemainingSeconds() <= 1.5)
            return "UPOZORENJE: Enrage ističe za " + String.format("%.0f", playerState.getEnrageRemainingSeconds()) + "s!";
        if (playerState.isRecklessnessActive() && !playerState.isEnrageActive())
            return "UPOZORENJE: Recklessness aktivan ali Enrage nije — iskoristi Bloodthirst!";
        return "";
    }

    public synchronized PlayerState getPlayerState() { return playerState; }
    public synchronized Map<String, AbilityState> getAbilities() { return abilities; }
    public synchronized TargetState getTargetState() { return targetState; }
    public synchronized Recommendation getCurrentRecommendation() { return currentRecommendation; }
    public synchronized String getWarningMessage() { return warningMessage; }

    public synchronized void setTargetHp(double hp) {
        targetState.setHpPercent(hp);
        runDrools();
    }
}
