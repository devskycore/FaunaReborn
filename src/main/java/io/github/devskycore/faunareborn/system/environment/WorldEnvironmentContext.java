package io.github.devskycore.faunareborn.system.environment;

public record WorldEnvironmentContext(
        boolean night,
        boolean raining,
        boolean thundering,
        MoonPhase moonPhase,
        boolean fullMoon,
        EnvironmentAggressionModifiers modifiers
) {
}
