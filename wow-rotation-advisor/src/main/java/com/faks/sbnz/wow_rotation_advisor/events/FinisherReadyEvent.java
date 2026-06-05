package com.faks.sbnz.wow_rotation_advisor.events;

import org.kie.api.definition.type.Expires;
import org.kie.api.definition.type.Role;

@Role(Role.Type.EVENT)
@Expires("8s")
public class FinisherReadyEvent {
}
