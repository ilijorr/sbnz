package com.faks.sbnz.wow_rotation_advisor;

import com.faks.sbnz.wow_rotation_advisor.config.TemplateRulesGenerator;
import com.faks.sbnz.wow_rotation_advisor.facts.*;
import com.faks.sbnz.wow_rotation_advisor.model.AbilityState;
import com.faks.sbnz.wow_rotation_advisor.model.FuryWarriorState;
import com.faks.sbnz.wow_rotation_advisor.model.SubtletyRogueState;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DroolsRulesTest {

    private final TemplateRulesGenerator gen = new TemplateRulesGenerator();

    @Test
    void cooldownReadyTemplate_generatesValidDrl() throws IOException {
        String drl = gen.generateCooldownReady();
        assertThat(drl).isNotBlank();

        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write("src/main/resources/rules/tpl-cooldown-ready.drl",
            ks.getResources().newByteArrayResource(drl.getBytes(StandardCharsets.UTF_8))
                .setResourceType(ResourceType.DRL));
        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();
        assertThat(kb.getResults().hasMessages(Message.Level.ERROR))
            .as("Template CooldownReady treba da se kompajlira bez gresaka")
            .isFalse();
    }

    @Test
    void recklessnessReady_whenCooldownZero() throws IOException {
        KieSession session = buildSessionFromTemplate(gen.generateCooldownReady());

        AbilityState reck = new AbilityState("Recklessness", 90.0, 1);
        session.insert(new FuryWarriorState());
        session.insert(reck);
        session.fireAllRules();

        assertThat(session.getObjects(o -> o instanceof RecklessnessReady))
            .as("RecklessnessReady treba biti insertan kad je cooldown 0")
            .hasSize(1);
        session.dispose();
    }

    @Test
    void rageHighRisk_whenResourceAbove80() throws IOException {
        KieSession session = buildSessionFromTemplate(gen.generateResourceOvercap());

        FuryWarriorState state = new FuryWarriorState();
        state.setPrimaryResource(85);
        session.insert(state);
        session.fireAllRules();

        assertThat(session.getObjects(o -> o instanceof RageHighRisk))
            .as("RageHighRisk treba biti insertan kad je rage >= 80")
            .hasSize(1);
        session.dispose();
    }

    @Test
    void prioritizeRupture_whenNeededAndCPAtMax() throws IOException {
        KieSession session = buildSessionFromTemplate(gen.generateFinisherForRefresh());

        session.insert(new RuptureRefreshNeeded());
        session.insert(new FinisherReady());
        session.fireAllRules();

        assertThat(session.getObjects(o -> o instanceof PrioritizeRupture))
            .as("PrioritizeRupture treba biti insertan kad je Rupture potreban i ima 5+ CP")
            .hasSize(1);
        session.dispose();
    }

    @Test
    void backwardChaining_prereqChainTransitive() throws IOException {
        byte[] bytes = DroolsRulesTest.class.getClassLoader()
            .getResourceAsStream("rules/backward-chaining.drl").readAllBytes();
        KieSession session = buildSessionFromTemplate(new String(bytes, StandardCharsets.UTF_8));

        session.insert(new Prerequisite("A", "B"));
        session.insert(new Prerequisite("B", "C"));
        session.fireAllRules();

        long chainCount = session.getObjects(o -> o instanceof PrereqChain).stream().count();
        assertThat(chainCount)
            .as("Treba biti 3 PrereqChain fakta: A→B, B→C, A→C (transitivno)")
            .isEqualTo(3);

        boolean transitivniLanac = session.getObjects(o -> o instanceof PrereqChain).stream()
            .map(o -> (PrereqChain) o)
            .anyMatch(c -> "A".equals(c.getFromAbility()) && "C".equals(c.getToAbility()) && c.getDepth() == 2);
        assertThat(transitivniLanac)
            .as("Transitivni lanac A→C sa dubinom 2 treba postojati")
            .isTrue();
        session.dispose();
    }

    private KieSession buildSessionFromTemplate(String drl) {
        KieHelper helper = new KieHelper();
        helper.addContent(drl, ResourceType.DRL);
        return helper.build().newKieSession();
    }
}
