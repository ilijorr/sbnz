package com.faks.sbnz.wow_rotation_advisor.model;

public abstract class PlayerState {

    private int primaryResource;

    public int getPrimaryResource() { return primaryResource; }

    public void setPrimaryResource(int v) {
        this.primaryResource = Math.max(0, Math.min(getMaxPrimaryResource(), v));
    }

    public abstract int getMaxPrimaryResource();
    public abstract String getSpec();
}
