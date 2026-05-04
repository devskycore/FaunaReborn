package io.github.devskycore.faunareborn.animal.chicken.hostility;

import io.github.devskycore.faunareborn.animal.chicken.config.ChickenHostilitySettings;
import org.bukkit.World;

import java.util.Locale;
import java.util.Map;

final class ChickenDamageScaler {

    private final double attackDamage;
    private final double peacefulDamageMultiplier;
    private final double easyDamageMultiplier;
    private final double normalDamageMultiplier;
    private final double hardDamageMultiplier;
    private final Map<String, Double> worldDamageMultipliers;
    private final boolean nightDamageEnabled;
    private final double nightDamageMultiplier;
    private final WorldNightStateCache worldNightStateCache;

    ChickenDamageScaler(
            ChickenHostilitySettings.Combat combat,
            ChickenHostilitySettings.DamageScaling damageScaling,
            WorldNightStateCache worldNightStateCache
    ) {
        this.attackDamage = combat.attackDamage();
        this.peacefulDamageMultiplier = damageScaling.peacefulDamageMultiplier();
        this.easyDamageMultiplier = damageScaling.easyDamageMultiplier();
        this.normalDamageMultiplier = damageScaling.normalDamageMultiplier();
        this.hardDamageMultiplier = damageScaling.hardDamageMultiplier();
        this.worldDamageMultipliers = damageScaling.worldDamageMultipliers();
        this.nightDamageEnabled = damageScaling.nightDamageEnabled();
        this.nightDamageMultiplier = damageScaling.nightDamageMultiplier();
        this.worldNightStateCache = worldNightStateCache;
    }

    double resolveScaledDamage(World world) {
        double difficultyMultiplier = switch (world.getDifficulty()) {
            case PEACEFUL -> peacefulDamageMultiplier;
            case EASY -> easyDamageMultiplier;
            case NORMAL -> normalDamageMultiplier;
            case HARD -> hardDamageMultiplier;
        };
        double worldMultiplier = worldDamageMultipliers.getOrDefault(world.getName().toLowerCase(Locale.ROOT), 1.0D);
        double damage = attackDamage * difficultyMultiplier * worldMultiplier;
        if (nightDamageEnabled && worldNightStateCache.isNight(world)) {
            damage *= nightDamageMultiplier;
        }
        return Math.max(0.0D, damage);
    }
}
