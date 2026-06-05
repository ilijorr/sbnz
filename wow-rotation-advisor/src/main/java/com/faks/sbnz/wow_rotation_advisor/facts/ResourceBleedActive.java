package com.faks.sbnz.wow_rotation_advisor.facts;

public class ResourceBleedActive {

    private final String spec;
    private final String message;

    public ResourceBleedActive(String spec, String message) {
        this.spec = spec;
        this.message = message;
    }

    public String getSpec() { return spec; }
    public String getMessage() { return message; }
}
