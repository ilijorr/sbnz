package com.faks.sbnz.wow_rotation_advisor.events;

import org.kie.api.definition.type.Expires;
import org.kie.api.definition.type.Role;

@Role(Role.Type.EVENT)
@Expires("20s")
public class AbilityUsedEvent {

    private final String spec;
    private final String abilityName;

    public AbilityUsedEvent(String spec, String abilityName) {
        this.spec = spec;
        this.abilityName = abilityName;
    }

    public String getSpec() { return spec; }
    public String getAbilityName() { return abilityName; }
}
