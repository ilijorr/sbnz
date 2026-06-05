package com.faks.sbnz.wow_rotation_advisor.model;

public class FuryWarriorState extends PlayerState {

    private boolean enrageActive;
    private double enrageRemainingSeconds;
    private boolean suddenDeathProcActive;
    private boolean recklessnessActive;
    private double recklessnessRemainingSeconds;
    private boolean avatarActive;
    private double avatarRemainingSeconds;

    public FuryWarriorState() {}

    @Override public int getMaxPrimaryResource() { return 100; }
    @Override public String getSpec() { return "FURY_WARRIOR"; }

    public boolean isEnrageActive() { return enrageActive; }
    public void setEnrageActive(boolean v) { this.enrageActive = v; }

    public double getEnrageRemainingSeconds() { return enrageRemainingSeconds; }
    public void setEnrageRemainingSeconds(double v) { this.enrageRemainingSeconds = v; }

    public boolean isSuddenDeathProcActive() { return suddenDeathProcActive; }
    public void setSuddenDeathProcActive(boolean v) { this.suddenDeathProcActive = v; }

    public boolean isRecklessnessActive() { return recklessnessActive; }
    public void setRecklessnessActive(boolean v) { this.recklessnessActive = v; }

    public double getRecklessnessRemainingSeconds() { return recklessnessRemainingSeconds; }
    public void setRecklessnessRemainingSeconds(double v) { this.recklessnessRemainingSeconds = v; }

    public boolean isAvatarActive() { return avatarActive; }
    public void setAvatarActive(boolean v) { this.avatarActive = v; }

    public double getAvatarRemainingSeconds() { return avatarRemainingSeconds; }
    public void setAvatarRemainingSeconds(double v) { this.avatarRemainingSeconds = v; }
}
