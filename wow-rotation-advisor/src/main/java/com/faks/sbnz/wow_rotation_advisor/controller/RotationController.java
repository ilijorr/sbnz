package com.faks.sbnz.wow_rotation_advisor.controller;

import com.faks.sbnz.wow_rotation_advisor.facts.Recommendation;
import com.faks.sbnz.wow_rotation_advisor.model.AbilityState;
import com.faks.sbnz.wow_rotation_advisor.model.DotState;
import com.faks.sbnz.wow_rotation_advisor.model.TargetState;
import com.faks.sbnz.wow_rotation_advisor.service.GameStateService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RotationController {

    private final GameStateService service;

    public RotationController(GameStateService service) {
        this.service = service;
    }

    @GetMapping("/state")
    public Map<String, Object> getState() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("spec", service.getCurrentSpec());
        response.put("player", service.getPlayerState());

        Map<String, Object> abilitiesMap = new LinkedHashMap<>();
        for (Map.Entry<String, AbilityState> e : service.getAbilities().entrySet()) {
            AbilityState a = e.getValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("cooldownRemaining", round(a.getCooldownRemaining()));
            m.put("chargesAvailable", a.getChargesAvailable());
            m.put("maxCharges", a.getMaxCharges());
            m.put("ready", a.isReady());
            abilitiesMap.put(e.getKey(), m);
        }
        response.put("abilities", abilitiesMap);

        Map<String, Object> dotsMap = new LinkedHashMap<>();
        for (Map.Entry<String, DotState> e : service.getDots().entrySet()) {
            DotState d = e.getValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("active", d.isActive());
            m.put("remainingSeconds", round(d.getRemainingSeconds()));
            m.put("pandemicThreshold", d.getPandemicThreshold());
            m.put("needsRefresh", d.isNeedsRefresh());
            dotsMap.put(e.getKey(), m);
        }
        response.put("dots", dotsMap);

        TargetState target = service.getTargetState();
        response.put("target", Map.of("hpPercent", target.getHpPercent()));

        Recommendation rec = service.getCurrentRecommendation();
        if (rec != null) {
            response.put("recommendation", Map.of(
                    "abilityName", rec.getAbilityName(),
                    "priority",    rec.getPriority(),
                    "urgency",     rec.getUrgency(),
                    "reason",      rec.getReason()
            ));
        }

        response.put("warning", service.getWarningMessage());
        response.put("prereqChain", service.getCurrentPrereqChain());
        response.put("cepAlerts", service.getCepAlerts());
        return response;
    }

    @PostMapping("/use-ability")
    public Map<String, String> useAbility(@RequestParam String name) {
        return Map.of("result", service.useAbility(name));
    }

    @PostMapping("/set-spec")
    public Map<String, String> setSpec(@RequestParam String spec) {
        service.setSpec(spec);
        return Map.of("result", "OK");
    }

    @PostMapping("/set-target-hp")
    public Map<String, String> setTargetHp(@RequestParam double hp) {
        service.setTargetHp(Math.max(0, Math.min(100, hp)));
        return Map.of("result", "OK");
    }

    @PostMapping("/reset")
    public Map<String, String> reset() {
        service.reset();
        return Map.of("result", "OK");
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
