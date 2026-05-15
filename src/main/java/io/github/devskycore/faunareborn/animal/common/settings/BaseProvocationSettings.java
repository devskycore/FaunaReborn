package io.github.devskycore.faunareborn.animal.common.settings;

public abstract class BaseProvocationSettings {

    private final boolean enabled;
    private final int aggressionDurationTicks;
    private final int forgetTargetAfterTicks;
    private final int triggerCooldownTicks;
    private final double detectionRange;
    private final double detectionRangeSq;
    private final boolean requireLineOfSight;
    private final int warningDurationTicks;
    private final double attackDamage;
    private final int attackCooldownTicks;
    private final double knockbackStrength;
    private final double speedMultiplier;
    private final int retargetGraceTicks;
    private final boolean playAggressiveSounds;
    private final boolean playWarningSound;
    private final boolean playStompSound;
    private final boolean chargeEnabled;
    private final int chargeMinIntervalTicks;
    private final int chargeMaxIntervalTicks;
    private final double chargeExtraPush;

    protected BaseProvocationSettings(
            boolean enabled,
            int aggressionDurationTicks,
            int forgetTargetAfterTicks,
            int triggerCooldownTicks,
            double detectionRange,
            double detectionRangeSq,
            boolean requireLineOfSight,
            int warningDurationTicks,
            double attackDamage,
            int attackCooldownTicks,
            double knockbackStrength,
            double speedMultiplier,
            int retargetGraceTicks,
            boolean playAggressiveSounds,
            boolean playWarningSound,
            boolean playStompSound,
            boolean chargeEnabled,
            int chargeMinIntervalTicks,
            int chargeMaxIntervalTicks,
            double chargeExtraPush
    ) {
        this.enabled = enabled;
        this.aggressionDurationTicks = aggressionDurationTicks;
        this.forgetTargetAfterTicks = forgetTargetAfterTicks;
        this.triggerCooldownTicks = triggerCooldownTicks;
        this.detectionRange = detectionRange;
        this.detectionRangeSq = detectionRangeSq;
        this.requireLineOfSight = requireLineOfSight;
        this.warningDurationTicks = warningDurationTicks;
        this.attackDamage = attackDamage;
        this.attackCooldownTicks = attackCooldownTicks;
        this.knockbackStrength = knockbackStrength;
        this.speedMultiplier = speedMultiplier;
        this.retargetGraceTicks = retargetGraceTicks;
        this.playAggressiveSounds = playAggressiveSounds;
        this.playWarningSound = playWarningSound;
        this.playStompSound = playStompSound;
        this.chargeEnabled = chargeEnabled;
        this.chargeMinIntervalTicks = chargeMinIntervalTicks;
        this.chargeMaxIntervalTicks = chargeMaxIntervalTicks;
        this.chargeExtraPush = chargeExtraPush;
    }

    protected final int triggerCooldownTicks() {
        return triggerCooldownTicks;
    }

    public final boolean enabled() {
        return enabled;
    }

    public final int aggressionDurationTicks() {
        return aggressionDurationTicks;
    }

    public final int forgetTargetAfterTicks() {
        return forgetTargetAfterTicks;
    }

    public final double detectionRange() {
        return detectionRange;
    }

    public final double detectionRangeSq() {
        return detectionRangeSq;
    }

    public final boolean requireLineOfSight() {
        return requireLineOfSight;
    }

    public final int warningDurationTicks() {
        return warningDurationTicks;
    }

    public final double attackDamage() {
        return attackDamage;
    }

    public final int attackCooldownTicks() {
        return attackCooldownTicks;
    }

    public final double knockbackStrength() {
        return knockbackStrength;
    }

    public final double speedMultiplier() {
        return speedMultiplier;
    }

    public final int retargetGraceTicks() {
        return retargetGraceTicks;
    }

    public final boolean playAggressiveSounds() {
        return playAggressiveSounds;
    }

    public final boolean playWarningSound() {
        return playWarningSound;
    }

    public final boolean playStompSound() {
        return playStompSound;
    }

    public final boolean chargeEnabled() {
        return chargeEnabled;
    }

    public final int chargeMinIntervalTicks() {
        return chargeMinIntervalTicks;
    }

    public final int chargeMaxIntervalTicks() {
        return chargeMaxIntervalTicks;
    }

    public final double chargeExtraPush() {
        return chargeExtraPush;
    }
}
