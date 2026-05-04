package io.github.devskycore.faunareborn.animal.chicken.config;

import io.github.devskycore.faunareborn.config.common.ConfigNumbers;
import io.github.devskycore.faunareborn.animal.chicken.config.combat.CombatConfigReader;
import io.github.devskycore.faunareborn.animal.chicken.config.combat.CombatConfigValues;
import io.github.devskycore.faunareborn.animal.chicken.config.combat.DamageScalingConfigReader;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.animal.chicken.config.movement.MovementConfigReader;
import io.github.devskycore.faunareborn.animal.chicken.config.movement.MovementConfigValues;
import io.github.devskycore.faunareborn.animal.chicken.config.night.NightBehaviorConfigReader;
import io.github.devskycore.faunareborn.animal.chicken.config.night.NightBehaviorConfigValues;
import io.github.devskycore.faunareborn.config.common.WorldFilter;
import io.github.devskycore.faunareborn.config.common.WorldFilterConfigReader;
import org.bukkit.Difficulty;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public final class ChickenHostilitySettingsLoader {

    private final FaunaRebornPlugin plugin;
    private final CombatConfigReader combatReader;
    private final ThreatAndCooldownConfigReader threatAndCooldownReader;
    private final ProcessingLimitsConfigReader processingLimitsReader;
    private final ActivationConfigReader activationReader;
    private final MovementConfigReader movementReader;
    private final WorldFilterConfigReader worldFilterReader;
    private final DamageScalingConfigReader damageScalingReader;
    private final NightBehaviorConfigReader nightBehaviorReader;
    private final ItemPickupTerritorialityConfigReader itemPickupTerritorialityReader;

    public ChickenHostilitySettingsLoader(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
        this.combatReader = new CombatConfigReader(plugin);
        this.threatAndCooldownReader = new ThreatAndCooldownConfigReader(plugin);
        this.processingLimitsReader = new ProcessingLimitsConfigReader(plugin);
        this.activationReader = new ActivationConfigReader(plugin);
        this.movementReader = new MovementConfigReader(plugin);
        this.worldFilterReader = new WorldFilterConfigReader(plugin);
        this.damageScalingReader = new DamageScalingConfigReader(plugin);
        this.nightBehaviorReader = new NightBehaviorConfigReader(plugin);
        this.itemPickupTerritorialityReader = new ItemPickupTerritorialityConfigReader(plugin);
    }

    public ChickenHostilitySettings load(FileConfiguration config) {
        CombatConfigValues combat = combatReader.read(config);
        ThreatAndCooldownConfigValues threatAndCooldown = threatAndCooldownReader.read(config);
        ProcessingLimitsConfigValues processingLimits = processingLimitsReader.read(config);
        ActivationConfig activation = activationReader.read(config);
        MovementConfigValues movement = movementReader.read(config);
        WorldFilter worldFilter = worldFilterReader.readWorldFilter(config);
        NightBehaviorConfigValues nightBehavior = nightBehaviorReader.read(config);
        ItemPickupTerritorialityConfig itemPickupTerritoriality = itemPickupTerritorialityReader.read(config);
        ConfigNumbers numbers = new ConfigNumbers(plugin);
        FileConfiguration globalConfig = plugin.getConfig();
        String visualRoot = "visual-effects";

        double peacefulDamageMultiplier = damageScalingReader.readDifficultyDamageMultiplier(config, Difficulty.PEACEFUL, 0.0D);
        double easyDamageMultiplier = damageScalingReader.readDifficultyDamageMultiplier(config, Difficulty.EASY, 1.0D);
        double normalDamageMultiplier = damageScalingReader.readDifficultyDamageMultiplier(config, Difficulty.NORMAL, 1.0D);
        double hardDamageMultiplier = damageScalingReader.readDifficultyDamageMultiplier(config, Difficulty.HARD, 1.2D);
        Map<String, Double> worldDamageMultipliers = damageScalingReader.readWorldDamageMultipliers(config);

        ChickenHostilitySettings.Combat combatSettings = new ChickenHostilitySettings.Combat(
                combat.attackDamage(),
                threatAndCooldown.globalTargetCooldownTicks(),
                combat.maxSimultaneousAttackers(),
                threatAndCooldown.attackCooldownTicks(),
                threatAndCooldown.threatTimeoutTicks(),
                threatAndCooldown.retargetGraceTicks(),
                threatAndCooldown.noLineOfSightResetTicks(),
                combat.detectionRadius(),
                combat.attackRange()
        );
        ChickenHostilitySettings.Limits limits = new ChickenHostilitySettings.Limits(
                processingLimits.maxActiveHostileChickensPerChunk(),
                processingLimits.maxActiveHostileChickensPerWorld(),
                processingLimits.maxProcessedChickensPerTick()
        );
        ChickenHostilitySettings.SocialAlert socialAlert = new ChickenHostilitySettings.SocialAlert(
                threatAndCooldown.socialAlertEnabled(),
                threatAndCooldown.socialAlertOnDamage(),
                threatAndCooldown.socialAlertOnNearbyDeath(),
                threatAndCooldown.socialAlertResponderAdultsOnly(),
                threatAndCooldown.socialAlertRadius(),
                threatAndCooldown.socialAlertCooldownTicks(),
                threatAndCooldown.socialAlertJoinCooldownTicks(),
                threatAndCooldown.socialAlertMaxResponders()
        );
        ChickenHostilitySettings.Visuals visuals = new ChickenHostilitySettings.Visuals(
                globalConfig.getBoolean(visualRoot + ".glow.enabled", PluginConfigDefaults.VISUAL_GLOW_ENABLED),
                globalConfig.getBoolean(visualRoot + ".particles.enabled", PluginConfigDefaults.VISUAL_PARTICLES_ENABLED),
                numbers.intRange(
                        globalConfig.getInt(
                                visualRoot + ".particles.interval-ticks",
                                PluginConfigDefaults.VISUAL_PARTICLES_INTERVAL_TICKS
                        ),
                        1,
                        PluginConfigDefaults.VISUAL_PARTICLES_INTERVAL_TICKS_LIMIT,
                        PluginConfigDefaults.VISUAL_PARTICLES_INTERVAL_TICKS,
                        "Invalid visual-effects.particles.interval-ticks in config.yml. Falling back to 8",
                        "visual-effects.particles.interval-ticks is too high. Clamped to 200"
                ),
                numbers.finiteRange(
                        globalConfig.getDouble(
                                visualRoot + ".particles.intensity",
                                PluginConfigDefaults.VISUAL_PARTICLES_VOLUME
                        ),
                        0.0D,
                        PluginConfigDefaults.VISUAL_PARTICLES_VOLUME_LIMIT,
                        PluginConfigDefaults.VISUAL_PARTICLES_VOLUME,
                        "Invalid visual-effects.particles.intensity in config.yml. Falling back to 1.0",
                        "visual-effects.particles.intensity is too high. Clamped to 5.0"
                ),
                globalConfig.getBoolean(visualRoot + ".sound.enabled", PluginConfigDefaults.VISUAL_SOUND_ENABLED),
                numbers.intRange(
                        globalConfig.getInt(
                                visualRoot + ".sound.interval-ticks",
                                PluginConfigDefaults.VISUAL_SOUND_INTERVAL_TICKS
                        ),
                        1,
                        PluginConfigDefaults.VISUAL_SOUND_INTERVAL_TICKS_LIMIT,
                        PluginConfigDefaults.VISUAL_SOUND_INTERVAL_TICKS,
                        "Invalid visual-effects.sound.interval-ticks in config.yml. Falling back to 160",
                        "visual-effects.sound.interval-ticks is too high. Clamped to 1200"
                ),
                numbers.finiteRange(
                        globalConfig.getDouble(
                                visualRoot + ".sound.volume",
                                PluginConfigDefaults.VISUAL_SOUND_VOLUME
                        ),
                        0.0D,
                        PluginConfigDefaults.VISUAL_SOUND_VOLUME_LIMIT,
                        PluginConfigDefaults.VISUAL_SOUND_VOLUME,
                        "Invalid visual-effects.sound.volume in config.yml. Falling back to 0.18",
                        "visual-effects.sound.volume is too high. Clamped to 5.0"
                )
        );
        ChickenHostilitySettings.Movement movementSettings = new ChickenHostilitySettings.Movement(
                movement.movementSpeedMultiplier(),
                movement.movementDistanceBoostStartDistance(),
                movement.movementDistanceBoostExtraSpeedPerBlock(),
                movement.movementDistanceBoostMaxMultiplier(),
                movement.movementTerrainJumpEnabled(),
                movement.movementTerrainJumpVerticalBoost(),
                movement.movementTerrainJumpCooldownTicks(),
                movement.movementTerrainJumpTriggerHeightDelta()
        );
        ChickenHostilitySettings.DamageScaling damageScaling = new ChickenHostilitySettings.DamageScaling(
                peacefulDamageMultiplier,
                easyDamageMultiplier,
                normalDamageMultiplier,
                hardDamageMultiplier,
                worldDamageMultipliers,
                nightBehavior.nightDamageEnabled(),
                nightBehavior.nightDamageMultiplier()
        );

        return new ChickenHostilitySettings(
                config.getBoolean("chicken-hostility.enabled", true),
                combatSettings,
                limits,
                socialAlert,
                visuals,
                movementSettings,
                damageScaling,
                activation,
                worldFilter,
                itemPickupTerritoriality
        );
    }

}
