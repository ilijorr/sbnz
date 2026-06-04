package com.faks.sbnz.wow_rotation_advisor.model;

public class DotState {

    private String name;
    private double remainingSeconds;
    private double pandemicThreshold;
    private boolean active;

    public DotState(String name, double pandemicThreshold) {
        this.name = name;
        this.pandemicThreshold = pandemicThreshold;
    }

    public void apply(double duration) {
        this.remainingSeconds = duration;
        this.active = true;
    }

    public void tick(double seconds) {
        if (active) {
            remainingSeconds = Math.max(0, remainingSeconds - seconds);
            if (remainingSeconds == 0) active = false;
        }
    }

    public boolean isNeedsRefresh() {
        return !active || remainingSeconds < pandemicThreshold;
    }

    public String getName() { return name; }
    public double getRemainingSeconds() { return remainingSeconds; }
    public double getPandemicThreshold() { return pandemicThreshold; }
    public boolean isActive() { return active; }
}
