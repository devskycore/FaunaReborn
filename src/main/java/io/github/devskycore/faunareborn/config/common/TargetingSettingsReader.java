package io.github.devskycore.faunareborn.config.common;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class TargetingSettingsReader {

    private final ConfigNumbers numbers;

    public TargetingSettingsReader(FaunaRebornPlugin plugin) {
        this.numbers = new ConfigNumbers(plugin);
    }

    public TargetingSettings read(FileConfiguration config) {
        boolean adventure = config.getBoolean("targeting.ignore.adventure", true);
        boolean invisiblePotion = config.getBoolean("targeting.ignore.invisible-potion", true);
        boolean vanished = config.getBoolean("targeting.ignore.vanished", true);
        boolean godMode = config.getBoolean("targeting.ignore.god-mode", true);

        boolean enabled = config.getBoolean("targeting.scoring.enabled", true);
        double healthWeight = numbers.finiteRange(
                config.getDouble("targeting.scoring.health-weight", 1.25D),
                0.0D,
                10.0D,
                1.25D,
                "Invalid targeting.scoring.health-weight in config.yml. Falling back to 1.25",
                "targeting.scoring.health-weight is too high. Clamped to 10.0"
        );
        double distanceWeight = numbers.finiteRange(
                config.getDouble("targeting.scoring.distance-weight", 1.0D),
                0.0D,
                10.0D,
                1.0D,
                "Invalid targeting.scoring.distance-weight in config.yml. Falling back to 1.0",
                "targeting.scoring.distance-weight is too high. Clamped to 10.0"
        );
        double currentThreatWeight = numbers.finiteRange(
                config.getDouble("targeting.scoring.current-threat-weight", 0.75D),
                0.0D,
                10.0D,
                0.75D,
                "Invalid targeting.scoring.current-threat-weight in config.yml. Falling back to 0.75",
                "targeting.scoring.current-threat-weight is too high. Clamped to 10.0"
        );
        double lineOfSightBonus = numbers.finiteRange(
                config.getDouble("targeting.scoring.line-of-sight-bonus", 0.35D),
                0.0D,
                10.0D,
                0.35D,
                "Invalid targeting.scoring.line-of-sight-bonus in config.yml. Falling back to 0.35",
                "targeting.scoring.line-of-sight-bonus is too high. Clamped to 10.0"
        );
        int retargetCooldownTicks = numbers.toNonNegativeTicks(numbers.finiteRange(
                config.getDouble("targeting.scoring.retarget-cooldown-seconds", 2.5D),
                0.0D,
                3600.0D,
                2.5D,
                "Invalid targeting.scoring.retarget-cooldown-seconds in config.yml. Falling back to 2.5",
                "targeting.scoring.retarget-cooldown-seconds is too high. Clamped to 3600"
        ));
        boolean requireMultipleCandidates = config.getBoolean("targeting.scoring.require-multiple-candidates", true);

        return new TargetingSettings(
                new TargetingSettings.Ignore(adventure, invisiblePotion, vanished, godMode),
                new TargetingSettings.Scoring(
                        enabled,
                        healthWeight,
                        distanceWeight,
                        currentThreatWeight,
                        lineOfSightBonus,
                        retargetCooldownTicks,
                        requireMultipleCandidates
                )
        );
    }
}
