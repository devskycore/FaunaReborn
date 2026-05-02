package io.github.devskycore.faunareborn.config.core;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;

public final class ConfigNumbers {

    private final FaunaRebornPlugin plugin;

    public ConfigNumbers(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
    }

    public double finiteAndMin(double value, double minInclusive, double fallback, String invalidMessage) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < minInclusive) {
            plugin.getLogger().warning(invalidMessage);
            return fallback;
        }
        return value;
    }

    public double finiteAndPositive(double value, double fallback, String invalidMessage) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0D) {
            plugin.getLogger().warning(invalidMessage);
            return fallback;
        }
        return value;
    }

    public double finiteRange(
            double value,
            double minInclusive,
            double maxInclusive,
            double fallback,
            String invalidMessage,
            String tooHighMessage
    ) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < minInclusive) {
            plugin.getLogger().warning(invalidMessage);
            return fallback;
        }
        if (value > maxInclusive) {
            plugin.getLogger().warning(tooHighMessage);
            return maxInclusive;
        }
        return value;
    }

    public int intRange(
            int value,
            int minInclusive,
            int maxInclusive,
            int fallback,
            String invalidMessage,
            String tooHighMessage
    ) {
        if (value < minInclusive) {
            plugin.getLogger().warning(invalidMessage);
            return fallback;
        }
        if (value > maxInclusive) {
            plugin.getLogger().warning(tooHighMessage);
            return maxInclusive;
        }
        return value;
    }

    public int toTicks(double seconds) {
        return toTicks(seconds, true);
    }

    public int toNonNegativeTicks(double seconds) {
        return toTicks(seconds, false);
    }

    private int toTicks(double seconds, boolean atLeastOne) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds)) {
            return atLeastOne ? 1 : 0;
        }

        double boundedSeconds = Math.min(seconds, PluginConfigDefaults.MAX_TIMER_SECONDS);
        long ticks = Math.round(boundedSeconds * 20.0D);
        if (ticks > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        int minimum = atLeastOne ? 1 : 0;
        return Math.max(minimum, (int) ticks);
    }
}

