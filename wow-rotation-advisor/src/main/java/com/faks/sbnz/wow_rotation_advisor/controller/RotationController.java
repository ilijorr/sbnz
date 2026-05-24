package com.faks.sbnz.wow_rotation_advisor.controller;

import com.faks.sbnz.wow_rotation_advisor.facts.Recommendation;
import com.faks.sbnz.wow_rotation_advisor.model.AbilityState;
import com.faks.sbnz.wow_rotation_advisor.model.PlayerState;
import com.faks.sbnz.wow_rotation_advisor.model.TargetState;
import com.faks.sbnz.wow_rotation_advisor.service.GameStateService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RotationController {

    private final GameStateService gameStateService;

    public RotationController(GameStateService gameStateService) {
        this.gameStateService = gameStateService;
    }

    @GetMapping("/state")
    public Map<String, Object> getState() {
        Map<String, Object> response = new HashMap<>();

        PlayerState player = gameStateService.getPlayerState();
        Map<String, Object> playerMap = new HashMap<>();
        playerMap.put("rage", player.getRage());
        playerMap.put("enrageActive", player.isEnrageActive());
        playerMap.put("enrageRemainingSeconds", round(player.getEnrageRemainingSeconds()));
        playerMap.put("suddenDeathProcActive", player.isSuddenDeathProcActive());
        playerMap.put("recklessnessActive", player.isRecklessnessActive());
        playerMap.put("recklessnessRemainingSeconds", round(player.getRecklessnessRemainingSeconds()));
        response.put("player", playerMap);

        Map<String, Object> abilitiesMap = new HashMap<>();
        for (Map.Entry<String, AbilityState> entry : gameStateService.getAbilities().entrySet()) {
            AbilityState a = entry.getValue();
            Map<String, Object> abilityMap = new HashMap<>();
            abilityMap.put("cooldownRemaining", round(a.getCooldownRemaining()));
            abilityMap.put("chargesAvailable", a.getChargesAvailable());
            abilityMap.put("maxCharges", a.getMaxCharges());
            abilityMap.put("ready", a.isReady());
            abilitiesMap.put(entry.getKey(), abilityMap);
        }
        response.put("abilities", abilitiesMap);

        TargetState target = gameStateService.getTargetState();
        response.put("target", Map.of("hpPercent", target.getHpPercent()));

        Recommendation rec = gameStateService.getCurrentRecommendation();
        if (rec != null) {
            response.put("recommendation", Map.of(
                    "abilityName", rec.getAbilityName(),
                    "priority", rec.getPriority(),
                    "urgency", rec.getUrgency(),
                    "reason", rec.getReason()
            ));
        }

        response.put("warning", gameStateService.getWarningMessage());
        return response;
    }

    @PostMapping("/use-ability")
    public Map<String, String> useAbility(@RequestParam String name) {
        String result = gameStateService.useAbility(name);
        return Map.of("result", result);
    }

    @PostMapping("/set-target-hp")
    public Map<String, String> setTargetHp(@RequestParam double hp) {
        gameStateService.setTargetHp(Math.max(0, Math.min(100, hp)));
        return Map.of("result", "OK");
    }

    @PostMapping("/reset")
    public Map<String, String> reset() {
        gameStateService.reset();
        return Map.of("result", "OK");
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
