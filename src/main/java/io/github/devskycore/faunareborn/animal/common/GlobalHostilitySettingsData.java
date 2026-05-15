package io.github.devskycore.faunareborn.animal.common;

import io.github.devskycore.faunareborn.config.common.TargetingSettings;
import io.github.devskycore.faunareborn.config.common.WorldFilter;

import java.util.Map;

public record GlobalHostilitySettingsData(
        double activationChance,
        boolean onlyNatural,
        boolean ignoreNamed,
        WorldFilter worldFilter,
        int maxActivePerChunk,
        int maxActivePerWorld,
        int maxProcessedPerTick,
        double peacefulDamageMultiplier,
        double easyDamageMultiplier,
        double normalDamageMultiplier,
        double hardDamageMultiplier,
        Map<String, Double> worldDamageMultipliers,
        boolean nightDamageEnabled,
        double nightDamageMultiplier,
        VisualEffectsSettingsData visualEffectsSettings,
        TargetingSettings targeting
) {
    public record VisualEffectsSettingsData(
            boolean glowEnabled,
            boolean particlesEnabled,
            int particlesIntervalTicks,
            double particlesIntensity,
            boolean soundEnabled,
            int soundIntervalTicks,
            double soundVolume
    ) {
    }
}
