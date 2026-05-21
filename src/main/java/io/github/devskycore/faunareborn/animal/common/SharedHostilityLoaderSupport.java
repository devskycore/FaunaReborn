package io.github.devskycore.faunareborn.animal.common;

import io.github.devskycore.faunareborn.animal.common.settings.CommonGlobalHostilitySettings;
import io.github.devskycore.faunareborn.animal.common.settings.SharedVisualEffectsSettings;

public final class SharedHostilityLoaderSupport {

    private SharedHostilityLoaderSupport() {
    }

    public static CommonGlobalHostilitySettings mapGlobalHostilitySettings(GlobalHostilitySettingsData data) {
        SharedVisualEffectsSettings visualEffectsSettings = visualEffectsSettings(data);
        return new CommonGlobalHostilitySettings(
                data.activationChance(),
                data.onlyNatural(),
                data.ignoreNamed(),
                data.worldFilter(),
                data.maxActivePerChunk(),
                data.maxActivePerWorld(),
                data.maxProcessedPerTick(),
                data.peacefulDamageMultiplier(),
                data.easyDamageMultiplier(),
                data.normalDamageMultiplier(),
                data.hardDamageMultiplier(),
                data.worldDamageMultipliers(),
                data.nightDamageEnabled(),
                data.nightDamageMultiplier(),
                visualEffectsSettings,
                data.targeting()
        );
    }

    public static int secondsToTicks(double seconds, boolean minimumOne) {
        long ticks = Math.round(seconds * 20.0D);
        if (ticks > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(minimumOne ? 1 : 0, (int) ticks);
    }

    public static double clampedDouble(double value, double min, double max, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < min) {
            return fallback;
        }
        return Math.min(value, max);
    }

    private static SharedVisualEffectsSettings visualEffectsSettings(GlobalHostilitySettingsData data) {
        GlobalHostilitySettingsData.VisualEffectsSettingsData visualData = data.visualEffectsSettings();
        return new SharedVisualEffectsSettings(
                visualData.glowEnabled(),
                visualData.particlesEnabled(),
                visualData.particlesIntervalTicks(),
                visualData.particlesIntensity(),
                visualData.soundEnabled(),
                visualData.soundIntervalTicks(),
                visualData.soundVolume()
        );
    }
}
