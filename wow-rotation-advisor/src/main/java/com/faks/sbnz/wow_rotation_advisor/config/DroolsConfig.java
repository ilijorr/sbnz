package com.faks.sbnz.wow_rotation_advisor.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class DroolsConfig {

    @Bean
    public KieContainer kieContainer() throws IOException {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        KieModuleModel kmm = ks.newKieModuleModel();
        KieBaseModel kbm = kmm.newKieBaseModel("RotationAdvisorRules")
            .setDefault(true)
            .addPackage("rules")
            .addPackage("cep_rules")
            .setEventProcessingMode(EventProcessingOption.STREAM);
        kbm.newKieSessionModel("rotationSession")
            .setDefault(true)
            .setClockType(ClockTypeOption.get("realtime"));
        kfs.writeKModuleXML(kmm.toXML());

        for (String path : List.of(
                "rules/fury-warrior.drl",
                "rules/subtlety-rogue.drl",
                "rules/havoc-dh.drl",
                "rules/backward-chaining.drl",
                "cep_rules/cep-rules.drl")) {
            byte[] bytes = new ClassPathResource(path).getInputStream().readAllBytes();
            kfs.write("src/main/resources/" + path,
                ks.getResources().newByteArrayResource(bytes).setResourceType(ResourceType.DRL));
        }

        TemplateRulesGenerator gen = new TemplateRulesGenerator();
        addGenerated(kfs, ks, "rules/tpl-cooldown-ready.drl",   gen.generateCooldownReady());
        addGenerated(kfs, ks, "rules/tpl-resource-overcap.drl", gen.generateResourceOvercap());
        addGenerated(kfs, ks, "rules/tpl-effect-expiring.drl",  gen.generateEffectExpiring());
        addGenerated(kfs, ks, "rules/tpl-finisher-refresh.drl", gen.generateFinisherForRefresh());

        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();
        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Greška u kompajliranju Drools pravila:\n" + kb.getResults().getMessages());
        }
        return ks.newKieContainer(kb.getKieModule().getReleaseId());
    }

    private void addGenerated(KieFileSystem kfs, KieServices ks, String path, String drl) {
        kfs.write("src/main/resources/" + path,
            ks.getResources().newByteArrayResource(drl.getBytes(StandardCharsets.UTF_8))
                .setResourceType(ResourceType.DRL));
    }
}
