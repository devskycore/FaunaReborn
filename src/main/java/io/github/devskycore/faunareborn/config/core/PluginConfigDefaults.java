package io.github.devskycore.faunareborn.config.core;

public final class PluginConfigDefaults {

    public static final double ATTACK_DAMAGE = 2.0D;
    public static final double MAX_ATTACK_DAMAGE = 100.0D;
    public static final double GLOBAL_TARGET_COOLDOWN_SECONDS = 1.5D;
    public static final double MAX_TIMER_SECONDS = 86_400.0D;
    public static final int MAX_SIMULTANEOUS_ATTACKERS = 3;
    public static final int MAX_SIMULTANEOUS_ATTACKERS_LIMIT = 64;
    public static final int MAX_ACTIVE_HOSTILE_CHICKENS_PER_CHUNK = 8;
    public static final int MAX_ACTIVE_HOSTILE_CHICKENS_PER_CHUNK_LIMIT = 128;
    public static final int MAX_ACTIVE_HOSTILE_CHICKENS_PER_WORLD = 250;
    public static final int MAX_ACTIVE_HOSTILE_CHICKENS_PER_WORLD_LIMIT = 5_000;
    public static final int MAX_PROCESSED_CHICKENS_PER_TICK = 300;
    public static final int MAX_PROCESSED_CHICKENS_PER_TICK_LIMIT = 1_000;
    public static final double ATTACK_COOLDOWN_SECONDS = 0.9D;
    public static final double THREAT_TIMEOUT_SECONDS = 8.0D;
    public static final double RETARGET_GRACE_SECONDS = 3.0D;
    public static final int NO_LINE_OF_SIGHT_RESET_TICKS = 40;
    public static final boolean SOCIAL_ALERT_ENABLED = true;
    public static final boolean SOCIAL_ALERT_ON_DAMAGE = true;
    public static final boolean SOCIAL_ALERT_ON_NEARBY_DEATH = true;
    public static final double SOCIAL_ALERT_RADIUS = 10.0D;
    public static final double MAX_SOCIAL_ALERT_RADIUS = 32.0D;
    public static final double SOCIAL_ALERT_COOLDOWN_SECONDS = 1.0D;
    public static final double SOCIAL_ALERT_JOIN_COOLDOWN_SECONDS = 2.0D;
    public static final int SOCIAL_ALERT_MAX_RESPONDERS = 4;
    public static final int SOCIAL_ALERT_MAX_RESPONDERS_LIMIT = 32;
    public static final boolean SOCIAL_ALERT_RESPONDER_ADULTS_ONLY = true;
    public static final boolean VISUAL_GLOW_ENABLED = true;
    public static final boolean VISUAL_PARTICLES_ENABLED = true;
    public static final int VISUAL_PARTICLES_INTERVAL_TICKS = 8;
    public static final int VISUAL_PARTICLES_INTERVAL_TICKS_LIMIT = 200;
    public static final double VISUAL_PARTICLES_VOLUME = 1.0D;
    public static final double VISUAL_PARTICLES_VOLUME_LIMIT = 5.0D;
    public static final boolean VISUAL_SOUND_ENABLED = true;
    public static final int VISUAL_SOUND_INTERVAL_TICKS = 160;
    public static final int VISUAL_SOUND_INTERVAL_TICKS_LIMIT = 1200;
    public static final double VISUAL_SOUND_VOLUME = 0.18D;
    public static final double VISUAL_SOUND_VOLUME_LIMIT = 5.0D;
    public static final double ACTIVATION_CHANCE = 1.0D;
    public static final boolean ONLY_NATURAL_CHICKENS = true;
    public static final boolean IGNORE_NAMED = true;
    public static final double DETECTION_RADIUS = 8.0D;
    public static final double MAX_DETECTION_RADIUS = 64.0D;
    public static final double ATTACK_RANGE = 1.5D;
    public static final double MAX_ATTACK_RANGE = 8.0D;
    public static final double MOVEMENT_SPEED_MULTIPLIER = 1.0D;
    public static final double MIN_MOVEMENT_SPEED_MULTIPLIER = 0.1D;
    public static final double MAX_MOVEMENT_SPEED_MULTIPLIER = 4.0D;
    public static final double MOVEMENT_DISTANCE_BOOST_START_DISTANCE = 5.0D;
    public static final double MOVEMENT_DISTANCE_BOOST_EXTRA_SPEED_PER_BLOCK = 0.08D;
    public static final double MOVEMENT_DISTANCE_BOOST_MAX_MULTIPLIER = 1.35D;
    public static final boolean MOVEMENT_TERRAIN_JUMP_ENABLED = true;
    public static final double MOVEMENT_TERRAIN_JUMP_VERTICAL_BOOST = 0.42D;
    public static final int MOVEMENT_TERRAIN_JUMP_COOLDOWN_TICKS = 8;
    public static final double MOVEMENT_TERRAIN_JUMP_TRIGGER_HEIGHT_DELTA = 0.6D;
    public static final double NIGHT_DAMAGE_MULTIPLIER = 1.2D;
    public static final double MIN_NIGHT_DAMAGE_MULTIPLIER = 1.0D;
    public static final double MAX_NIGHT_DAMAGE_MULTIPLIER = 1.5D;
    public static final double MAX_DAMAGE_MULTIPLIER = 10.0D;

    private PluginConfigDefaults() {
    }
}

