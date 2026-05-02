package io.github.devskycore.faunareborn.config.combat;

import io.github.devskycore.faunareborn.config.core.ConfigNumbers;
import io.github.devskycore.faunareborn.config.core.PluginConfigDefaults;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class CombatConfigReader {

    private final ConfigNumbers numbers;

    public CombatConfigReader(FaunaRebornPlugin plugin) {
        this.numbers = new ConfigNumbers(plugin);
    }

    public CombatConfigValues read(FileConfiguration config) {
        double attackDamage = numbers.finiteRange(
                config.getDouble("chicken-hostility.attack-damage", PluginConfigDefaults.ATTACK_DAMAGE),
                0.0D,
                PluginConfigDefaults.MAX_ATTACK_DAMAGE,
                PluginConfigDefaults.ATTACK_DAMAGE,
                "Invalid chicken-hostility.attack-damage in config.yml. Falling back to 2.0",
                "chicken-hostility.attack-damage is too high. Clamped to 100.0"
        );
        int maxSimultaneousAttackers = numbers.intRange(
                config.getInt(
                        "chicken-hostility.max-simultaneous-attackers-per-player",
                        PluginConfigDefaults.MAX_SIMULTANEOUS_ATTACKERS
                ),
                1,
                PluginConfigDefaults.MAX_SIMULTANEOUS_ATTACKERS_LIMIT,
                PluginConfigDefaults.MAX_SIMULTANEOUS_ATTACKERS,
                "Invalid chicken-hostility.max-simultaneous-attackers-per-player in config.yml. Falling back to 3",
                "chicken-hostility.max-simultaneous-attackers-per-player is too high. Clamped to 64"
        );
        double detectionRadius = numbers.finiteRange(
                config.getDouble("chicken-hostility.detection-radius", PluginConfigDefaults.DETECTION_RADIUS),
                0.01D,
                PluginConfigDefaults.MAX_DETECTION_RADIUS,
                PluginConfigDefaults.DETECTION_RADIUS,
                "Invalid chicken-hostility.detection-radius in config.yml. Falling back to 8.0",
                "chicken-hostility.detection-radius is too high. Clamped to 64.0"
        );
        double attackRange = numbers.finiteRange(
                config.getDouble("chicken-hostility.attack-range", PluginConfigDefaults.ATTACK_RANGE),
                0.01D,
                PluginConfigDefaults.MAX_ATTACK_RANGE,
                PluginConfigDefaults.ATTACK_RANGE,
                "Invalid chicken-hostility.attack-range in config.yml. Falling back to 1.5",
                "chicken-hostility.attack-range is too high. Clamped to 8.0"
        );

        return new CombatConfigValues(attackDamage, maxSimultaneousAttackers, detectionRadius, attackRange);
    }
}

