package com.faks.sbnz.wow_rotation_advisor.config;

import org.drools.template.ObjectDataCompiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class TemplateRulesGenerator {

    public String generateCooldownReady() throws IOException {
        List<Map<String, Object>> data = List.of(
            Map.of("abilityName", "Avatar",           "readyFactClass", "AvatarReady"),
            Map.of("abilityName", "Recklessness",     "readyFactClass", "RecklessnessReady"),
            Map.of("abilityName", "Odyn's Fury",      "readyFactClass", "OdynsFuryReady"),
            Map.of("abilityName", "Shadow Blades",    "readyFactClass", "ShadowBladesReady"),
            Map.of("abilityName", "Symbols of Death", "readyFactClass", "SymbolsReady"),
            Map.of("abilityName", "Flagellation",     "readyFactClass", "FlagellationReady"),
            Map.of("abilityName", "Secret Technique", "readyFactClass", "SecretTechniqueReady"),
            Map.of("abilityName", "Fel Devastation",  "readyFactClass", "FelDevastationReady"),
            Map.of("abilityName", "Metamorphosis",    "readyFactClass", "MetamorphosisReady"),
            Map.of("abilityName", "Eye Beam",         "readyFactClass", "EyeBeamReady"),
            Map.of("abilityName", "Blade Dance",      "readyFactClass", "BladeDanceReady"),
            Map.of("abilityName", "Essence Break",    "readyFactClass", "EssenceBreakReady")
        );
        return compile(data, "/templates/cooldown-ready.drl.template");
    }

    public String generateResourceOvercap() throws IOException {
        List<Map<String, Object>> data = List.of(
            Map.of("stateClass", "FuryWarriorState", "threshold", 80,  "riskFactClass", "RageHighRisk"),
            Map.of("stateClass", "HavocDHState",     "threshold", 80,  "riskFactClass", "FuryHighRisk"),
            Map.of("stateClass", "HavocDHState",     "threshold", 100, "riskFactClass", "FuryOvercapRisk")
        );
        return compile(data, "/templates/resource-overcap-risk.drl.template");
    }

    public String generateEffectExpiring() throws IOException {
        List<Map<String, Object>> data = List.of(
            Map.of("stateClass", "SubtletyRogueState", "activeField", "sliceAndDiceActive",
                   "remainingField", "sliceAndDiceRemainingSeconds", "threshold", 2.0, "factClass", "SliceAndDiceRefreshNeeded")
        );
        return compile(data, "/templates/effect-expiring.drl.template");
    }

    public String generateFinisherForRefresh() throws IOException {
        List<Map<String, Object>> data = List.of(
            Map.of("refreshNeededFactClass", "SliceAndDiceRefreshNeeded", "priorityFactClass", "PrioritizeSnd"),
            Map.of("refreshNeededFactClass", "RuptureRefreshNeeded",       "priorityFactClass", "PrioritizeRupture")
        );
        return compile(data, "/templates/finisher-for-refresh.drl.template");
    }

    private String compile(List<Map<String, Object>> data, String templatePath) throws IOException {
        try (InputStream tpl = getClass().getResourceAsStream(templatePath)) {
            if (tpl == null) throw new IOException("Template not found: " + templatePath);
            return new ObjectDataCompiler().compile(data, tpl);
        }
    }
}
