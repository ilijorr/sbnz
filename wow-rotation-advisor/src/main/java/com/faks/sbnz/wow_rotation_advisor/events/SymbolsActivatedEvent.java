package com.faks.sbnz.wow_rotation_advisor.events;

import org.kie.api.definition.type.Expires;
import org.kie.api.definition.type.Role;

@Role(Role.Type.EVENT)
@Expires("40s")
public class SymbolsActivatedEvent {
}
