package io.github.devskycore.faunareborn.animal.cow.hostility;

import io.github.devskycore.faunareborn.animal.cow.CowSettings;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

final class CowMilkAggressionController {

    private static final double ATTACK_RANGE = 2.1D;
    private static final double ATTACK_RANGE_SQ = ATTACK_RANGE * ATTACK_RANGE;
    private static final double FORWARD_PUSH = 0.23D;
    private static final double MAX_VERTICAL_GAP = 5.0D;

    private final CowSettings.MilkProvocationSettings settings;
    private final CowSettings.GlobalHostilitySettings global;
    private final Int2ObjectOpenHashMap<CowMilkAggressionBrain> brains = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<Cow> trackedCows = new Int2ObjectOpenHashMap<>();
    private final Map<PlayerCowPair, Long> milkTriggerCooldownUntil = new java.util.HashMap<>();
    private final Map<UUID, Double> originalMovementSpeedByCow = new java.util.HashMap<>();
    private final Map<UUID, Integer> activeCowsByWorld = new java.util.HashMap<>();
    private final Map<WorldChunkKey, Integer> activeCowsByChunk = new java.util.HashMap<>();
    private final Vector scratch = new Vector();
    private long currentTick;
    private long activeCacheTick = Long.MIN_VALUE;

    CowMilkAggressionController(CowSettings.MilkProvocationSettings settings, CowSettings.GlobalHostilitySettings global) {
        this.settings = settings;
        this.global = global;
    }

    void tick() {
        currentTick++;
        cleanupMilkCooldownMap();
        if (brains.isEmpty()) {
            return;
        }
        rebuildActiveCowCaches();

        int processed = 0;
        for (var iterator = brains.int2ObjectEntrySet().fastIterator(); iterator.hasNext() && processed < global.maxProcessedPerTick(); ) {
            var entry = iterator.next();
            processed++;
            int cowId = entry.getIntKey();
            CowMilkAggressionBrain brain = entry.getValue();
            Cow cow = trackedCows.get(cowId);
            if (cow == null || !cow.isValid() || cow.isDead()) {
                trackedCows.remove(cowId);
                iterator.remove();
                continue;
            }

            if (isWorldDisallowed(cow.getWorld()) || cow.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
                calmDown(cow, brain);
                trackedCows.remove(cowId);
                iterator.remove();
                continue;
            }

            Player target = resolveTarget(brain.targetUuid);
            if (target == null || target.getWorld() != cow.getWorld()) {
                calmDown(cow, brain);
                trackedCows.remove(cowId);
                iterator.remove();
                continue;
            }

            if (currentTick > brain.aggressionUntilTick) {
                calmDown(cow, brain);
                trackedCows.remove(cowId);
                iterator.remove();
                continue;
            }

            applyVisualEffects(cow);
            if (brain.state == CowMilkAggressionState.WARNING) {
                handleWarning(cow, target, brain);
                continue;
            }

            pursue(cow, target, brain);
            tryAttack(cow, target, brain);
        }
    }

    void provokeCowFromMilking(Cow cow, Player aggressor, boolean naturalCow) {
        if (!settings.enabled() || !cow.isAdult()) {
            return;
        }
        if (isWorldDisallowed(cow.getWorld()) || cow.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        if (global.ignoreNamed() && cow.customName() != null) {
            return;
        }
        if (global.onlyNatural() && !naturalCow) {
            return;
        }
        if (global.activationChance() < 1.0D && ThreadLocalRandom.current().nextDouble() > global.activationChance()) {
            return;
        }
        ensureActiveCowCachesFresh();
        if (countActiveForChunk(cow) >= global.maxActiveHostilePerChunk()) {
            return;
        }
        if (countActiveForWorld(cow.getWorld()) >= global.maxActiveHostilePerWorld()) {
            return;
        }

        PlayerCowPair pair = new PlayerCowPair(aggressor.getUniqueId(), cow.getUniqueId());
        Long cooldownUntil = milkTriggerCooldownUntil.get(pair);
        if (cooldownUntil != null && currentTick < cooldownUntil) {
            return;
        }
        if (settings.milkingTriggerCooldownTicks() > 0) {
            milkTriggerCooldownUntil.put(pair, currentTick + settings.milkingTriggerCooldownTicks());
        }

        int cowId = cow.getEntityId();
        trackedCows.put(cowId, cow);
        CowMilkAggressionBrain brain = brains.computeIfAbsent(cowId, ignored -> new CowMilkAggressionBrain());
        brain.targetUuid = aggressor.getUniqueId();
        brain.aggressionUntilTick = Math.max(brain.aggressionUntilTick, currentTick + settings.aggressionDurationTicks());
        brain.forgetTargetAtTick = Math.max(brain.forgetTargetAtTick, currentTick + settings.forgetTargetAfterTicks());
        brain.warningUntilTick = currentTick + settings.warningDurationTicks();
        brain.state = settings.warningDurationTicks() > 0 ? CowMilkAggressionState.WARNING : CowMilkAggressionState.CHASE;
        brain.lastAttackTick = Long.MIN_VALUE;
        brain.nextChargeTick = currentTick + randomChargeDelay();

        cow.setTarget(aggressor);
        cow.setAggressive(true);
        faceTarget(cow, aggressor);
        playWarningAudio(cow, aggressor);
    }

    void removeCow(int cowId) {
        Cow cow = trackedCows.get(cowId);
        if (cow != null) {
            cow.setGlowing(false);
            restoreMovementBase(cow);
        }
        brains.remove(cowId);
        trackedCows.remove(cowId);
    }

    void removeTarget(UUID targetId) {
        if (targetId == null) {
            return;
        }
        for (var iterator = brains.int2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            CowMilkAggressionBrain brain = entry.getValue();
            if (!targetId.equals(brain.targetUuid)) {
                continue;
            }
            Cow cow = trackedCows.get(entry.getIntKey());
            if (cow != null) {
                calmDown(cow, brain);
            }
            trackedCows.remove(entry.getIntKey());
            iterator.remove();
        }
    }

    void clearAll() {
        for (var entry : brains.int2ObjectEntrySet()) {
            Cow cow = trackedCows.get(entry.getIntKey());
            if (cow != null) {
                calmDown(cow, entry.getValue());
            }
        }
        brains.clear();
        trackedCows.clear();
        milkTriggerCooldownUntil.clear();
        originalMovementSpeedByCow.clear();
    }

    private void handleWarning(Cow cow, Player target, CowMilkAggressionBrain brain) {
        faceTarget(cow, target);
        setAggressiveMovement(cow, 0.0D);
        if (currentTick < brain.warningUntilTick) {
            return;
        }
        brain.state = CowMilkAggressionState.CHASE;
        playChargeSound(cow, target);
    }

    private void pursue(Cow cow, Player target, CowMilkAggressionBrain brain) {
        if (settings.requireLineOfSight() && !cow.hasLineOfSight(target)) {
            if (currentTick > brain.forgetTargetAtTick) {
                calmDown(cow, brain);
            }
            return;
        }

        double distanceSq = distanceSq(cow, target);
        if (distanceSq > settings.detectionRangeSq() || Math.abs(cow.getY() - target.getY()) > MAX_VERTICAL_GAP) {
            if (currentTick > brain.forgetTargetAtTick) {
                calmDown(cow, brain);
            }
            return;
        }

        cow.setTarget(target);
        cow.setAggressive(true);
        setAggressiveMovement(cow, normalizedIntensity(brain));

        scratch.setX(target.getX() - cow.getX());
        scratch.setY(0.0D);
        scratch.setZ(target.getZ() - cow.getZ());
        double length = scratch.length();
        if (length <= 0.001D) {
            return;
        }

        scratch.multiply(1.0D / length);
        double push = FORWARD_PUSH;
        if (settings.chargeEnabled() && currentTick >= brain.nextChargeTick) {
            push += settings.chargeExtraPush();
            brain.nextChargeTick = currentTick + randomChargeDelay();
            playStompSound(cow);
        }
        scratch.multiply(push);

        Vector currentVelocity = cow.getVelocity();
        currentVelocity.setX(currentVelocity.getX() * 0.65D + scratch.getX());
        currentVelocity.setZ(currentVelocity.getZ() * 0.65D + scratch.getZ());
        cow.setVelocity(currentVelocity);
    }

    private void tryAttack(Cow cow, Player target, CowMilkAggressionBrain brain) {
        if (distanceSq(cow, target) > ATTACK_RANGE_SQ) {
            return;
        }
        if (currentTick - brain.lastAttackTick < settings.attackCooldownTicks()) {
            return;
        }
        brain.lastAttackTick = currentTick;

        target.damage(settings.attackDamage() * resolveDamageMultiplier(cow.getWorld()), cow);
        Vector knockbackDirection = target.getLocation().toVector().subtract(cow.getLocation().toVector());
        knockbackDirection.setY(0.0D);
        if (knockbackDirection.lengthSquared() > 0.0001D) {
            knockbackDirection.normalize().multiply(settings.knockbackStrength());
            target.setVelocity(target.getVelocity().add(knockbackDirection));
        }
        playAttackSound(cow, target);
    }

    private Player resolveTarget(UUID targetUuid) {
        if (targetUuid == null) {
            return null;
        }
        Player player = org.bukkit.Bukkit.getPlayer(targetUuid);
        if (player == null || !player.isOnline() || player.isDead()) {
            return null;
        }
        GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
            return null;
        }
        return player;
    }

    private void setAggressiveMovement(Cow cow, double intensity) {
        AttributeInstance movement = cow.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }
        UUID cowId = cow.getUniqueId();
        double originalBase = originalMovementSpeedByCow.computeIfAbsent(cowId, ignored -> movement.getBaseValue());
        double desired = originalBase * (1.0D + ((settings.speedMultiplier() - 1.0D) * Math.max(0.0D, intensity)));
        movement.setBaseValue(Math.clamp(desired, 0.05D, 0.8D));
    }

    private void calmDown(Cow cow, CowMilkAggressionBrain brain) {
        cow.setTarget(null);
        cow.setAggressive(false);
        cow.setGlowing(false);
        restoreMovementBase(cow);
        brain.targetUuid = null;
    }

    private void faceTarget(Cow cow, Player target) {
        double dx = target.getX() - cow.getX();
        double dz = target.getZ() - cow.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        cow.setRotation(yaw, cow.getPitch());
    }

    private double normalizedIntensity(CowMilkAggressionBrain brain) {
        long remaining = Math.max(0L, brain.aggressionUntilTick - currentTick);
        double ratio = (double) remaining / (double) settings.aggressionDurationTicks();
        return Math.clamp(ratio, 0.35D, 1.0D);
    }

    private long randomChargeDelay() {
        int min = settings.chargeMinIntervalTicks();
        int max = settings.chargeMaxIntervalTicks();
        return ThreadLocalRandom.current().nextLong(min, (long) max + 1L);
    }

    private void cleanupMilkCooldownMap() {
        if (milkTriggerCooldownUntil.isEmpty() || currentTick % 40L != 0L) {
            return;
        }
        milkTriggerCooldownUntil.entrySet().removeIf(entry -> currentTick >= entry.getValue());
    }

    private void playWarningAudio(Cow cow, Player target) {
        if (!global.visualEffects().soundEnabled()) {
            return;
        }
        if (settings.playWarningSound()) {
            cow.getWorld().playSound(cow.getLocation(), Sound.ENTITY_COW_AMBIENT, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.75F);
        }
        if (settings.playStompSound()) {
            cow.getWorld().playSound(cow.getLocation(), Sound.ENTITY_COW_STEP, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.6F);
        }
        if (settings.playAggressiveSounds()) {
            target.playSound(target.getLocation(), Sound.ENTITY_COW_HURT, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.85F);
        }
    }

    private void playChargeSound(Cow cow, Player target) {
        if (!settings.playAggressiveSounds() || !global.visualEffects().soundEnabled()) {
            return;
        }
        cow.getWorld().playSound(cow.getLocation(), Sound.ENTITY_COW_HURT, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.65F);
        target.playSound(target.getLocation(), Sound.ENTITY_COW_HURT, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.75F);
    }

    private void playStompSound(Cow cow) {
        if (!settings.playStompSound() || !global.visualEffects().soundEnabled()) {
            return;
        }
        cow.getWorld().playSound(cow.getLocation(), Sound.ENTITY_COW_STEP, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.55F);
    }

    private void playAttackSound(Cow cow, Player target) {
        if (!settings.playAggressiveSounds() || !global.visualEffects().soundEnabled()) {
            return;
        }
        cow.getWorld().playSound(cow.getLocation(), Sound.ENTITY_COW_HURT, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.6F);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.9F);
    }

    private double distanceSq(Cow cow, Player player) {
        double dx = cow.getX() - player.getX();
        double dy = cow.getY() - player.getY();
        double dz = cow.getZ() - player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean isWorldDisallowed(World world) {
        return world == null || !global.worldFilter().isWorldAllowed(world.getName());
    }

    private int countActiveForChunk(Cow probe) {
        return activeCowsByChunk.getOrDefault(
                new WorldChunkKey(probe.getWorld().getUID(), probe.getChunk().getX(), probe.getChunk().getZ()),
                0
        );
    }

    private int countActiveForWorld(World world) {
        return activeCowsByWorld.getOrDefault(world.getUID(), 0);
    }

    private double resolveDamageMultiplier(World world) {
        if (world == null) {
            return 1.0D;
        }
        double difficultyMultiplier = switch (world.getDifficulty()) {
            case PEACEFUL -> global.peacefulDamageMultiplier();
            case EASY -> global.easyDamageMultiplier();
            case NORMAL -> global.normalDamageMultiplier();
            case HARD -> global.hardDamageMultiplier();
        };
        double worldMultiplier = global.worldDamageMultipliers().getOrDefault(world.getName().toLowerCase(Locale.ROOT), 1.0D);
        double nightMultiplier = isNight(world) && global.nightDamageEnabled() ? global.nightDamageMultiplier() : 1.0D;
        return Math.max(0.0D, difficultyMultiplier * worldMultiplier * nightMultiplier);
    }

    private boolean isNight(World world) {
        long time = world.getTime();
        return time >= 13000L && time <= 23000L;
    }

    private void applyVisualEffects(Cow cow) {
        CowSettings.VisualEffectsSettings visual = global.visualEffects();
        cow.setGlowing(visual.glowEnabled());

        if (visual.particlesEnabled() && visual.particlesIntervalTicks() > 0 && currentTick % visual.particlesIntervalTicks() == 0L) {
            int amount = Math.max(1, (int) Math.round(2.0D * visual.particlesIntensity()));
            cow.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, cow.getLocation().add(0.0D, 1.2D, 0.0D), amount, 0.25D, 0.25D, 0.25D, 0.0D);
        }
    }

    private void restoreMovementBase(Cow cow) {
        AttributeInstance movement = cow.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }
        Double original = originalMovementSpeedByCow.remove(cow.getUniqueId());
        if (original != null) {
            movement.setBaseValue(original);
        }
    }

    private void ensureActiveCowCachesFresh() {
        if (activeCacheTick == currentTick) {
            return;
        }
        rebuildActiveCowCaches();
    }

    private void rebuildActiveCowCaches() {
        activeCowsByWorld.clear();
        activeCowsByChunk.clear();
        for (Cow cow : trackedCows.values()) {
            if (cow == null || !cow.isValid() || cow.isDead()) {
                continue;
            }
            UUID worldId = cow.getWorld().getUID();
            activeCowsByWorld.merge(worldId, 1, Integer::sum);
            activeCowsByChunk.merge(
                    new WorldChunkKey(worldId, cow.getChunk().getX(), cow.getChunk().getZ()),
                    1,
                    Integer::sum
            );
        }
        activeCacheTick = currentTick;
    }

    private record PlayerCowPair(UUID playerUuid, UUID cowUuid) {
    }

    private record WorldChunkKey(UUID worldId, int chunkX, int chunkZ) {
    }
}
