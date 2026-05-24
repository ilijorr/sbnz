package com.faks.sbnz.wow_rotation_advisor.model;

public class TargetState {

    private double hpPercent;

    public TargetState(double hpPercent) {
        this.hpPercent = hpPercent;
    }

    public double getHpPercent() { return hpPercent; }
    public void setHpPercent(double hpPercent) { this.hpPercent = hpPercent; }
}
