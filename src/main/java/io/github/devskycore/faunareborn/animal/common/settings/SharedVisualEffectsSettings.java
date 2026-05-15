package io.github.devskycore.faunareborn.animal.common.settings;

public record SharedVisualEffectsSettings(
        boolean glowEnabled,
        boolean particlesEnabled,
        int particlesIntervalTicks,
        double particlesIntensity,
        boolean soundEnabled,
        int soundIntervalTicks,
        double soundVolume
) {
}
