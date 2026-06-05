package com.faks.sbnz.wow_rotation_advisor.events;

import org.kie.api.definition.type.Expires;
import org.kie.api.definition.type.Role;

@Role(Role.Type.EVENT)
@Expires("8s")
public class ProcEvent {

    private final String spec;
    private final String procName;

    public ProcEvent(String spec, String procName) {
        this.spec = spec;
        this.procName = procName;
    }

    public String getSpec() { return spec; }
    public String getProcName() { return procName; }
}
