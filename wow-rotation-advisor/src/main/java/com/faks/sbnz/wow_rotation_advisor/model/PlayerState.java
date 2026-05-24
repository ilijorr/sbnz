package com.faks.sbnz.wow_rotation_advisor.model;

public class PlayerState {

    private int rage;
    private boolean enrageActive;
    private double enrageRemainingSeconds;
    private boolean suddenDeathProcActive;
    private boolean recklessnessActive;
    private double recklessnessRemainingSeconds;

    public PlayerState() {
        this.rage = 0;
        this.enrageActive = false;
        this.enrageRemainingSeconds = 0;
        this.suddenDeathProcActive = false;
        this.recklessnessActive = false;
        this.recklessnessRemainingSeconds = 0;
    }

    public int getRage() { return rage; }
    public void setRage(int rage) { this.rage = Math.max(0, Math.min(100, rage)); }

    public boolean isEnrageActive() { return enrageActive; }
    public void setEnrageActive(boolean enrageActive) { this.enrageActive = enrageActive; }

    public double getEnrageRemainingSeconds() { return enrageRemainingSeconds; }
    public void setEnrageRemainingSeconds(double enrageRemainingSeconds) {
        this.enrageRemainingSeconds = enrageRemainingSeconds;
    }

    public boolean isSuddenDeathProcActive() { return suddenDeathProcActive; }
    public void setSuddenDeathProcActive(boolean suddenDeathProcActive) {
        this.suddenDeathProcActive = suddenDeathProcActive;
    }

    public boolean isRecklessnessActive() { return recklessnessActive; }
    public void setRecklessnessActive(boolean recklessnessActive) {
        this.recklessnessActive = recklessnessActive;
    }

    public double getRecklessnessRemainingSeconds() { return recklessnessRemainingSeconds; }
    public void setRecklessnessRemainingSeconds(double recklessnessRemainingSeconds) {
        this.recklessnessRemainingSeconds = recklessnessRemainingSeconds;
    }
}
