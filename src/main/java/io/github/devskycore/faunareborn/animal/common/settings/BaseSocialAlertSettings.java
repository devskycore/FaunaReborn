package io.github.devskycore.faunareborn.animal.common.settings;

public abstract class BaseSocialAlertSettings {

    private final boolean enabled;
    private final boolean onDamage;
    private final boolean onNearbyDeath;
    private final boolean responderAdultsOnly;
    private final double radius;
    private final int cooldownTicks;
    private final int joinCooldownTicks;
    private final int maxResponders;

    protected BaseSocialAlertSettings(
            boolean enabled,
            boolean onDamage,
            boolean onNearbyDeath,
            boolean responderAdultsOnly,
            double radius,
            int cooldownTicks,
            int joinCooldownTicks,
            int maxResponders
    ) {
        this.enabled = enabled;
        this.onDamage = onDamage;
        this.onNearbyDeath = onNearbyDeath;
        this.responderAdultsOnly = responderAdultsOnly;
        this.radius = radius;
        this.cooldownTicks = cooldownTicks;
        this.joinCooldownTicks = joinCooldownTicks;
        this.maxResponders = maxResponders;
    }

    public final boolean enabled() {
        return enabled;
    }

    public final boolean onDamage() {
        return onDamage;
    }

    public final boolean onNearbyDeath() {
        return onNearbyDeath;
    }

    public final boolean responderAdultsOnly() {
        return responderAdultsOnly;
    }

    public final double radius() {
        return radius;
    }

    public final int cooldownTicks() {
        return cooldownTicks;
    }

    public final int joinCooldownTicks() {
        return joinCooldownTicks;
    }

    public final int maxResponders() {
        return maxResponders;
    }
}
