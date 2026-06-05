package com.faks.sbnz.wow_rotation_advisor.facts;

public class ProcStormActive {

    private final String spec;
    private final String message;

    public ProcStormActive(String spec, String message) {
        this.spec = spec;
        this.message = message;
    }

    public String getSpec() { return spec; }
    public String getMessage() { return message; }
}
