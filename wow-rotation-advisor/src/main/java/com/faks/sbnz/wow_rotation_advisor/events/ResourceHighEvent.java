package com.faks.sbnz.wow_rotation_advisor.events;

import org.kie.api.definition.type.Expires;
import org.kie.api.definition.type.Role;

@Role(Role.Type.EVENT)
@Expires("6s")
public class ResourceHighEvent {

    private final String spec;

    public ResourceHighEvent(String spec) {
        this.spec = spec;
    }

    public String getSpec() { return spec; }
}
