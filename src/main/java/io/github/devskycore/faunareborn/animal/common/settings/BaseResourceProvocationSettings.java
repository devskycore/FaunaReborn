package io.github.devskycore.faunareborn.animal.common.settings;

public abstract class BaseResourceProvocationSettings {

    private final boolean enabled;
    private final int thresholdOne;
    private final int thresholdTwo;
    private final int thresholdThree;
    private final double detectionRadius;
    private final double detectionRadiusSq;
    private final int timeWindowTicks;
    private final int maxItemAgeTicks;
    private final boolean nightModifierEnabled;
    private final double nightThresholdMultiplier;
    private final boolean socialPropagationEnabled;
    private final int maxResponders;
    private final int triggerCooldownTicks;
    private final int aggressionDurationTicks;

    protected BaseResourceProvocationSettings(
            boolean enabled,
            int thresholdOne,
            int thresholdTwo,
            int thresholdThree,
            double detectionRadius,
            double detectionRadiusSq,
            int timeWindowTicks,
            int maxItemAgeTicks,
            boolean nightModifierEnabled,
            double nightThresholdMultiplier,
            boolean socialPropagationEnabled,
            int maxResponders,
            int triggerCooldownTicks,
            int aggressionDurationTicks
    ) {
        this.enabled = enabled;
        this.thresholdOne = thresholdOne;
        this.thresholdTwo = thresholdTwo;
        this.thresholdThree = thresholdThree;
        this.detectionRadius = detectionRadius;
        this.detectionRadiusSq = detectionRadiusSq;
        this.timeWindowTicks = timeWindowTicks;
        this.maxItemAgeTicks = maxItemAgeTicks;
        this.nightModifierEnabled = nightModifierEnabled;
        this.nightThresholdMultiplier = nightThresholdMultiplier;
        this.socialPropagationEnabled = socialPropagationEnabled;
        this.maxResponders = maxResponders;
        this.triggerCooldownTicks = triggerCooldownTicks;
        this.aggressionDurationTicks = aggressionDurationTicks;
    }

    protected final int thresholdOne() {
        return thresholdOne;
    }

    protected final int thresholdTwo() {
        return thresholdTwo;
    }

    protected final int thresholdThree() {
        return thresholdThree;
    }

    public final boolean enabled() {
        return enabled;
    }

    public final double detectionRadius() {
        return detectionRadius;
    }

    public final double detectionRadiusSq() {
        return detectionRadiusSq;
    }

    public final int timeWindowTicks() {
        return timeWindowTicks;
    }

    public final int maxItemAgeTicks() {
        return maxItemAgeTicks;
    }

    public final boolean nightModifierEnabled() {
        return nightModifierEnabled;
    }

    public final double nightThresholdMultiplier() {
        return nightThresholdMultiplier;
    }

    public final boolean socialPropagationEnabled() {
        return socialPropagationEnabled;
    }

    public final int maxResponders() {
        return maxResponders;
    }

    public final int triggerCooldownTicks() {
        return triggerCooldownTicks;
    }

    public final int aggressionDurationTicks() {
        return aggressionDurationTicks;
    }
}
