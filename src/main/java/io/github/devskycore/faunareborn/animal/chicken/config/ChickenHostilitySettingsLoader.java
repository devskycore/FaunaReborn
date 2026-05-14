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
import io.github.devskycore.faunareborn.config.common.TargetingSettings;
import io.github.devskycore.faunareborn.config.common.TargetingSettingsReader;
import io.github.devskycore.faunareborn.config.common.LodSettingsReader;
import org.bukkit.Difficulty;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import io.github.devskycore.faunareborn.system.environment.EnvironmentAggressionSettings;
import io.github.devskycore.faunareborn.system.lod.LodSettings;

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
    private final TargetingSettingsReader targetingSettingsReader;
    private final LodSettingsReader lodSettingsReader;

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
        this.targetingSettingsReader = new TargetingSettingsReader(plugin);
        this.lodSettingsReader = new LodSettingsReader(plugin);
    }

    public ChickenHostilitySettings load(FileConfiguration globalConfig, FileConfiguration entityConfig) {
        CombatConfigValues combat = combatReader.read(entityConfig);
        ThreatAndCooldownConfigValues threatAndCooldown = threatAndCooldownReader.read(entityConfig);
        ProcessingLimitsConfigValues processingLimits = processingLimitsReader.read(entityConfig);
        ActivationConfig activation = activationReader.read(entityConfig);
        MovementConfigValues movement = movementReader.read(entityConfig);
        WorldFilter worldFilter = worldFilterReader.readWorldFilter(entityConfig);
        NightBehaviorConfigValues nightBehavior = nightBehaviorReader.read(entityConfig);
        ItemPickupTerritorialityConfig itemPickupTerritoriality = itemPickupTerritorialityReader.read(entityConfig);
        TargetingSettings targeting = targetingSettingsReader.read(globalConfig);
        EnvironmentAggressionSettings environmentAggressionSettings = EnvironmentAggressionSettings.fromConfig(entityConfig, "");
        LodSettings lodSettings = lodSettingsReader.read(globalConfig, "lod");
        ConfigNumbers numbers = new ConfigNumbers(plugin);
        String visualRoot = "visual-effects";

        double peacefulDamageMultiplier = damageScalingReader.readDifficultyDamageMultiplier(entityConfig, Difficulty.PEACEFUL, 0.0D);
        double easyDamageMultiplier = damageScalingReader.readDifficultyDamageMultiplier(entityConfig, Difficulty.EASY, 1.0D);
        double normalDamageMultiplier = damageScalingReader.readDifficultyDamageMultiplier(entityConfig, Difficulty.NORMAL, 1.0D);
        double hardDamageMultiplier = damageScalingReader.readDifficultyDamageMultiplier(entityConfig, Difficulty.HARD, 1.2D);
        Map<String, Double> worldDamageMultipliers = damageScalingReader.readWorldDamageMultipliers(entityConfig);

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
                entityConfig.getBoolean("chicken-hostility.enabled", true),
                combatSettings,
                limits,
                socialAlert,
                visuals,
                movementSettings,
                damageScaling,
                activation,
                worldFilter,
                targeting,
                itemPickupTerritoriality,
                environmentAggressionSettings,
                lodSettings
        );
    }

}

