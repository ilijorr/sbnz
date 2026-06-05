package com.faks.sbnz.wow_rotation_advisor.model;

public class SubtletyRogueState extends PlayerState {

    private int comboPoints;
    private boolean shadowDanceActive;
    private double shadowDanceRemainingSeconds;
    private boolean symbolsOfDeathActive;
    private double symbolsOfDeathRemainingSeconds;
    private boolean sliceAndDiceActive;
    private double sliceAndDiceRemainingSeconds;
    private boolean flagellationActive;
    private double flagellationRemainingSeconds;
    private int flagellationStacks;
    private boolean shadowBladesActive;
    private double shadowBladesRemainingSeconds;

    public SubtletyRogueState() {
        setPrimaryResource(60);
    }

    @Override public int getMaxPrimaryResource() { return 100; }
    @Override public String getSpec() { return "SUBTLETY_ROGUE"; }

    public int getComboPoints() { return comboPoints; }
    public void setComboPoints(int v) { this.comboPoints = Math.max(0, Math.min(7, v)); }

    public boolean isShadowDanceActive() { return shadowDanceActive; }
    public void setShadowDanceActive(boolean v) { this.shadowDanceActive = v; }

    public double getShadowDanceRemainingSeconds() { return shadowDanceRemainingSeconds; }
    public void setShadowDanceRemainingSeconds(double v) { this.shadowDanceRemainingSeconds = v; }

    public boolean isSymbolsOfDeathActive() { return symbolsOfDeathActive; }
    public void setSymbolsOfDeathActive(boolean v) { this.symbolsOfDeathActive = v; }

    public double getSymbolsOfDeathRemainingSeconds() { return symbolsOfDeathRemainingSeconds; }
    public void setSymbolsOfDeathRemainingSeconds(double v) { this.symbolsOfDeathRemainingSeconds = v; }

    public boolean isSliceAndDiceActive() { return sliceAndDiceActive; }
    public void setSliceAndDiceActive(boolean v) { this.sliceAndDiceActive = v; }

    public double getSliceAndDiceRemainingSeconds() { return sliceAndDiceRemainingSeconds; }
    public void setSliceAndDiceRemainingSeconds(double v) { this.sliceAndDiceRemainingSeconds = v; }

    public boolean isFlagellationActive() { return flagellationActive; }
    public void setFlagellationActive(boolean v) { this.flagellationActive = v; }

    public double getFlagellationRemainingSeconds() { return flagellationRemainingSeconds; }
    public void setFlagellationRemainingSeconds(double v) { this.flagellationRemainingSeconds = v; }

    public int getFlagellationStacks() { return flagellationStacks; }
    public void setFlagellationStacks(int v) { this.flagellationStacks = Math.max(0, Math.min(30, v)); }

    public boolean isShadowBladesActive() { return shadowBladesActive; }
    public void setShadowBladesActive(boolean v) { this.shadowBladesActive = v; }

    public double getShadowBladesRemainingSeconds() { return shadowBladesRemainingSeconds; }
    public void setShadowBladesRemainingSeconds(double v) { this.shadowBladesRemainingSeconds = v; }
}
