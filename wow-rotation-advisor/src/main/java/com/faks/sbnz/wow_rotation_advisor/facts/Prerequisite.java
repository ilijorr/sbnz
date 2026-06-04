package com.faks.sbnz.wow_rotation_advisor.facts;

public class Prerequisite {
    private final String abilityName;
    private final String prereqName;

    public Prerequisite(String abilityName, String prereqName) {
        this.abilityName = abilityName;
        this.prereqName  = prereqName;
    }

    public String getAbilityName() { return abilityName; }
    public String getPrereqName()  { return prereqName;  }
}
