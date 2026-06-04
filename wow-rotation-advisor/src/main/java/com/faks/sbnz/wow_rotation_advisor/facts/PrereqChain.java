package com.faks.sbnz.wow_rotation_advisor.facts;

public class PrereqChain {
    private final String fromAbility;
    private final String toAbility;
    private final int depth;

    public PrereqChain(String fromAbility, String toAbility, int depth) {
        this.fromAbility = fromAbility;
        this.toAbility   = toAbility;
        this.depth       = depth;
    }

    public String getFromAbility() { return fromAbility; }
    public String getToAbility()   { return toAbility;   }
    public int    getDepth()       { return depth;       }
}
