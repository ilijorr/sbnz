package com.faks.sbnz.wow_rotation_advisor.facts;

public class Recommendation {

    private String abilityName;
    private int priority;
    private String urgency;
    private String reason;

    public Recommendation(String abilityName, int priority, String urgency, String reason) {
        this.abilityName = abilityName;
        this.priority = priority;
        this.urgency = urgency;
        this.reason = reason;
    }

    public String getAbilityName() { return abilityName; }
    public int getPriority() { return priority; }
    public String getUrgency() { return urgency; }
    public String getReason() { return reason; }
}
