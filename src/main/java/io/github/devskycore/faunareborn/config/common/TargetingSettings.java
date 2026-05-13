package io.github.devskycore.faunareborn.config.common;

public record TargetingSettings(
        Ignore ignore,
        Scoring scoring
) {
    public record Ignore(
            boolean adventure,
            boolean invisiblePotion,
            boolean vanished,
            boolean godMode
    ) {
    }

    public record Scoring(
            boolean enabled,
            double healthWeight,
            double distanceWeight,
            double currentThreatWeight,
            double lineOfSightBonus,
            int retargetCooldownTicks,
            boolean requireMultipleCandidates
    ) {
    }
}
