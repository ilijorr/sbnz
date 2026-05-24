package com.faks.sbnz.wow_rotation_advisor.facts;

public class EnragedState {
    private double remainingSeconds;

    public EnragedState(double remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public double getRemainingSeconds() { return remainingSeconds; }
}
