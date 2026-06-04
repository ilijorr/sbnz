package com.faks.sbnz.wow_rotation_advisor.model;

public class HavocDHState extends PlayerState {

    private boolean metamorphosisActive;
    private double metamorphosisRemainingSeconds;
    private boolean demonicActive;
    private double demonicRemainingSeconds;
    private boolean immolationAuraActive;
    private double immolationAuraRemainingSeconds;
    private boolean essenceBreakActive;
    private double essenceBreakRemainingSeconds;

    public HavocDHState() {}

    @Override public int getMaxPrimaryResource() { return 120; }
    @Override public String getSpec() { return "HAVOC_DH"; }

    public boolean isMetamorphosisActive() { return metamorphosisActive; }
    public void setMetamorphosisActive(boolean v) { this.metamorphosisActive = v; }

    public double getMetamorphosisRemainingSeconds() { return metamorphosisRemainingSeconds; }
    public void setMetamorphosisRemainingSeconds(double v) { this.metamorphosisRemainingSeconds = v; }

    public boolean isDemonicActive() { return demonicActive; }
    public void setDemonicActive(boolean v) { this.demonicActive = v; }

    public double getDemonicRemainingSeconds() { return demonicRemainingSeconds; }
    public void setDemonicRemainingSeconds(double v) { this.demonicRemainingSeconds = v; }

    public boolean isImmolationAuraActive() { return immolationAuraActive; }
    public void setImmolationAuraActive(boolean v) { this.immolationAuraActive = v; }

    public double getImmolationAuraRemainingSeconds() { return immolationAuraRemainingSeconds; }
    public void setImmolationAuraRemainingSeconds(double v) { this.immolationAuraRemainingSeconds = v; }

    public boolean isEssenceBreakActive() { return essenceBreakActive; }
    public void setEssenceBreakActive(boolean v) { this.essenceBreakActive = v; }

    public double getEssenceBreakRemainingSeconds() { return essenceBreakRemainingSeconds; }
    public void setEssenceBreakRemainingSeconds(double v) { this.essenceBreakRemainingSeconds = v; }
}
