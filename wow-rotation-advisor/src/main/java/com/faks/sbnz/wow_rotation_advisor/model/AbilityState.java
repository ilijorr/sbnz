package com.faks.sbnz.wow_rotation_advisor.model;

public class AbilityState {

    private String name;
    private double cooldownRemaining;
    private int chargesAvailable;
    private double maxCooldown;
    private int maxCharges;

    public AbilityState(String name, double maxCooldown, int maxCharges) {
        this.name = name;
        this.maxCooldown = maxCooldown;
        this.maxCharges = maxCharges;
        this.cooldownRemaining = 0;
        this.chargesAvailable = maxCharges;
    }

    public void useCharge() {
        if (chargesAvailable > 0) {
            chargesAvailable--;
            if (cooldownRemaining == 0) {
                cooldownRemaining = maxCooldown;
            }
        }
    }

    public void tick(double seconds) {
        if (cooldownRemaining > 0) {
            cooldownRemaining = Math.max(0, cooldownRemaining - seconds);
            if (cooldownRemaining == 0 && chargesAvailable < maxCharges) {
                chargesAvailable++;
                if (chargesAvailable < maxCharges) {
                    cooldownRemaining = maxCooldown;
                }
            }
        }
    }

    public boolean isReady() {
        return cooldownRemaining == 0 && (maxCharges == 1 || chargesAvailable > 0);
    }

    public String getName() { return name; }
    public double getCooldownRemaining() { return cooldownRemaining; }
    public void setCooldownRemaining(double cooldownRemaining) { this.cooldownRemaining = cooldownRemaining; }
    public int getChargesAvailable() { return chargesAvailable; }
    public void setChargesAvailable(int chargesAvailable) { this.chargesAvailable = chargesAvailable; }
    public double getMaxCooldown() { return maxCooldown; }
    public int getMaxCharges() { return maxCharges; }
}
