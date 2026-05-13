package io.github.devskycore.faunareborn.animal.pig.hostility;

import io.github.devskycore.faunareborn.animal.pig.PigSettings;
import io.github.devskycore.faunareborn.combat.deathmessage.HostileSpecies;
import io.github.devskycore.faunareborn.combat.deathmessage.HostilityCause;
import io.github.devskycore.faunareborn.combat.deathmessage.HostilityContextTracker;
import io.github.devskycore.faunareborn.system.platform.RuntimePlatform;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapter;
import io.github.devskycore.faunareborn.targeting.TargetEligibilityService;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.bukkit.Difficulty;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import io.github.devskycore.faunareborn.system.environment.WorldEnvironmentContextCache;
import io.github.devskycore.faunareborn.system.environment.EnvironmentAggressionModifiers;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class PigAggressionController {

    private static final double ATTACK_RANGE = 2.6D;
    private static final double ATTACK_RANGE_SQ = ATTACK_RANGE * ATTACK_RANGE;
    private static final double ATTACK_VERTICAL_TOLERANCE = 2.5D;
    private static final double FORWARD_PUSH = 0.23D;
    private static final double MAX_VERTICAL_GAP = 5.0D;
    private static final long LINE_OF_SIGHT_CACHE_TICKS = 4L;
    private static final long TARGET_REFRESH_INTERVAL_TICKS = 10L;
    private static final long MOVEMENT_ATTRIBUTE_UPDATE_INTERVAL_TICKS = 5L;
    private static final double MOVEMENT_ATTRIBUTE_EPSILON = 0.0005D;
    private static final int TARGET_REACTIVATION_COOLDOWN_TICKS = 20;

    private final SchedulerAdapter scheduler;
    private final boolean folia;
    private final Object stateLock = new Object();
    private final PigSettings.RodProvocationSettings settings;
    private final PigSettings.GlobalHostilitySettings global;
    private final TargetEligibilityService targetEligibilityService;
    private final PigActivationPolicy activationPolicy;
    private final WorldEnvironmentContextCache environmentCache;
    private final PigTargetingIndex targetingIndex = new PigTargetingIndex();
    private final Int2ObjectOpenHashMap<PigAggressionBrain> brains = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<Pig> trackedPigs = new Int2ObjectOpenHashMap<>();
    private final Map<UUID, Int2LongOpenHashMap> milkTriggerCooldownUntilByPlayer = new java.util.HashMap<>();
    private final Map<UUID, Int2LongOpenHashMap> resourceTriggerCooldownUntilByPlayer = new java.util.HashMap<>();
    private final Int2DoubleOpenHashMap originalMovementSpeedByPigId = new Int2DoubleOpenHashMap();
    private final Int2LongOpenHashMap socialAlertCooldownUntilByPigId = new Int2LongOpenHashMap();
    private final Object2LongOpenHashMap<UUID> targetReactivationCooldownUntil = new Object2LongOpenHashMap<>();
    private final Vector scratch = new Vector();
    private IntIterator processingCursor;
    private long currentTick;
    private long activeCacheTick = Long.MIN_VALUE;

    PigAggressionController(
            SchedulerAdapter scheduler,
            PigSettings.RodProvocationSettings settings,
            PigSettings.GlobalHostilitySettings global,
            WorldEnvironmentContextCache environmentCache
    ) {
        this.scheduler = scheduler;
        this.folia = RuntimePlatform.isFolia();
        this.settings = settings;
        this.global = global;
        this.targetEligibilityService = new TargetEligibilityService(global.targeting());
        this.activationPolicy = new PigActivationPolicy(settings, global, targetEligibilityService);
        this.environmentCache = environmentCache;
        this.socialAlertCooldownUntilByPigId.defaultReturnValue(Long.MIN_VALUE);
        this.originalMovementSpeedByPigId.defaultReturnValue(Double.NaN);
        this.targetReactivationCooldownUntil.defaultReturnValue(Long.MIN_VALUE);
    }

    void tick() {
        if (folia) {
            tickFolia();
            return;
        }
        tickLegacy();
    }

    private void tickLegacy() {
        currentTick++;
        cleanupMilkCooldownMap();
        cleanupResourceCooldownMap();
        cleanupTargetCooldownMap();
        if (brains.isEmpty()) {
            return;
        }

        int processed = 0;
        int scanned = 0;
        int scanBudgetTarget = Math.max(global.maxProcessedPerTick() * 3, 64);
        int scanBudget = Math.clamp(scanBudgetTarget, 0, brains.size());
        while (processed < global.maxProcessedPerTick() && scanned < scanBudget && !brains.isEmpty()) {
            int PigId = nextProcessingPigId();
            if (PigId == Integer.MIN_VALUE) {
                break;
            }
            scanned++;

            PigAggressionBrain brain = brains.get(PigId);
            if (brain == null) {
                continue;
            }
            Pig Pig = trackedPigs.get(PigId);
            if (Pig == null || !Pig.isValid() || Pig.isDead()) {
                removeAggressionState(PigId, Pig, brain, false);
                continue;
            }

            if (isWorldDisallowed(Pig.getWorld()) || Pig.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
                removeAggressionState(PigId, Pig, brain, true);
                continue;
            }

            Player target = resolveTarget(brain.targetUuid);
            if (target == null || target.getWorld() != Pig.getWorld()) {
                removeAggressionState(PigId, Pig, brain, true);
                continue;
            }

            if (currentTick > brain.aggressionUntilTick) {
                removeAggressionState(PigId, Pig, brain, true);
                continue;
            }

            processed++;
            applyVisualEffects(Pig, brain);
            processState(Pig, target, brain);

            if (brain.targetUuid == null) {
                removeAggressionState(PigId, Pig, brain, false);
            }
        }
    }

    private void tickFolia() {
        List<Integer> processIds = new ArrayList<>();
        long tickSnapshot;
        synchronized (stateLock) {
            currentTick++;
            tickSnapshot = currentTick;
            cleanupMilkCooldownMap();
            cleanupResourceCooldownMap();
            cleanupTargetCooldownMap();
            if (brains.isEmpty()) {
                return;
            }

            int processed = 0;
            int scanned = 0;
            int scanBudgetTarget = Math.max(global.maxProcessedPerTick() * 3, 64);
            int scanBudget = Math.clamp(scanBudgetTarget, 0, brains.size());
            while (processed < global.maxProcessedPerTick() && scanned < scanBudget && !brains.isEmpty()) {
                int PigId = nextProcessingPigId();
                if (PigId == Integer.MIN_VALUE) {
                    break;
                }
                scanned++;
                processIds.add(PigId);
                processed++;
            }
        }

        for (int PigId : processIds) {
            Pig Pig;
            synchronized (stateLock) {
                Pig = trackedPigs.get(PigId);
            }
            if (Pig == null) {
                continue;
            }
            scheduler.runForEntity(Pig, () -> processPigFolia(PigId, tickSnapshot));
        }
    }

    private void processPigFolia(int PigId, long tickSnapshot) {
        synchronized (stateLock) {
            if (currentTick != tickSnapshot) {
                return;
            }
            PigAggressionBrain brain = brains.get(PigId);
            if (brain == null) {
                return;
            }
            Pig Pig = trackedPigs.get(PigId);
            if (Pig == null || !Pig.isValid() || Pig.isDead()) {
                removeAggressionState(PigId, Pig, brain, false);
                return;
            }

            if (isWorldDisallowed(Pig.getWorld()) || Pig.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
                removeAggressionState(PigId, Pig, brain, true);
                return;
            }

            Player target = resolveTarget(brain.targetUuid);
            if (target == null || target.getWorld() != Pig.getWorld()) {
                removeAggressionState(PigId, Pig, brain, true);
                return;
            }

            if (currentTick > brain.aggressionUntilTick) {
                removeAggressionState(PigId, Pig, brain, true);
                return;
            }

            applyVisualEffects(Pig, brain);
            processState(Pig, target, brain);

            if (brain.targetUuid == null) {
                removeAggressionState(PigId, Pig, brain, false);
            }
        }
    }

    void provokePigFromRodProvocation(Pig Pig, Player aggressor, boolean naturalPig) {
        synchronized (stateLock) {
        if (isProvocationBlocked(Pig, naturalPig)) {
            return;
        }

        UUID playerId = aggressor.getUniqueId();
        int PigId = Pig.getEntityId();
        if (isCooldownActive(milkTriggerCooldownUntilByPlayer, playerId, PigId)) {
            return;
        }
        if (settings.rodTriggerCooldownTicks() > 0) {
            putCooldown(milkTriggerCooldownUntilByPlayer, playerId, PigId, currentTick + settings.rodTriggerCooldownTicks());
        }

        activatePigAggression(Pig, aggressor, naturalPig, settings.aggressionDurationTicks(), HostilityCause.ROD_PROVOCATION);
        }
    }

    boolean provokePigFromResources(
            Pig Pig,
            Player aggressor,
            boolean naturalPig,
            int triggerCooldownTicks,
            int aggressionDurationTicks
    ) {
        return provokePigFromResources(
                Pig,
                aggressor,
                naturalPig,
                triggerCooldownTicks,
                aggressionDurationTicks,
                HostilityCause.TERRITORIAL_PICKUP
        );
    }

    boolean provokePigFromResources(
            Pig Pig,
            Player aggressor,
            boolean naturalPig,
            int triggerCooldownTicks,
            int aggressionDurationTicks,
            HostilityCause hostilityCause
    ) {
        synchronized (stateLock) {
        if (isProvocationBlocked(Pig, naturalPig)) {
            return false;
        }
        UUID playerId = aggressor.getUniqueId();
        int PigId = Pig.getEntityId();
        if (isCooldownActive(resourceTriggerCooldownUntilByPlayer, playerId, PigId)) {
            return false;
        }
        if (triggerCooldownTicks > 0) {
            putCooldown(resourceTriggerCooldownUntilByPlayer, playerId, PigId, currentTick + triggerCooldownTicks);
        }
        return activatePigAggression(Pig, aggressor, naturalPig, aggressionDurationTicks, hostilityCause);
        }
    }

    void provokeNearbyPigsFromSocialAlert(
            Pig emitter,
            Player aggressor,
            java.util.List<org.bukkit.entity.Entity> nearbyEntities,
            PigSettings.SocialAlertSettings socialAlertSettings,
            java.util.function.Predicate<Pig> naturalPigPredicate,
            HostilityCause hostilityCause
    ) {
        synchronized (stateLock) {
        if (!socialAlertSettings.enabled() || socialAlertSettings.maxResponders() <= 0) {
            return;
        }
        if (emitter == null || aggressor == null || nearbyEntities == null || nearbyEntities.isEmpty()) {
            return;
        }
        if (!targetEligibilityService.isEligible(aggressor, global.worldFilter(), -1L)) {
            return;
        }

        long cooldownUntil = socialAlertCooldownUntilByPigId.get(emitter.getEntityId());
        if (currentTick < cooldownUntil) {
            return;
        }

        UUID aggressorId = aggressor.getUniqueId();
        int recruited = 0;
        ensureActivePigCachesFresh();
        for (org.bukkit.entity.Entity entity : nearbyEntities) {
            if (!(entity instanceof Pig ally) || ally.getEntityId() == emitter.getEntityId()) {
                continue;
            }
            if (targetingIndex.attackersForTarget(aggressorId) >= socialAlertSettings.maxResponders()) {
                break;
            }
            if (socialAlertSettings.responderAdultsOnly() && !ally.isAdult()) {
                continue;
            }
            if (isProvocationBlocked(ally, naturalPigPredicate.test(ally))) {
                continue;
            }
            PigAggressionBrain allyBrain = brains.get(ally.getEntityId());
            if (allyBrain != null && currentTick < allyBrain.socialAlertBlockedUntilTick) {
                continue;
            }
            if (!activatePigAggression(ally, aggressor, naturalPigPredicate.test(ally), settings.aggressionDurationTicks(), hostilityCause)) {
                continue;
            }
            if (socialAlertSettings.joinCooldownTicks() > 0) {
                PigAggressionBrain recruitedBrain = brains.get(ally.getEntityId());
                if (recruitedBrain != null) {
                    recruitedBrain.socialAlertBlockedUntilTick = currentTick + socialAlertSettings.joinCooldownTicks();
                }
            }
            recruited++;
            if (recruited >= socialAlertSettings.maxResponders()) {
                break;
            }
        }

        if (recruited > 0 && socialAlertSettings.cooldownTicks() > 0) {
            socialAlertCooldownUntilByPigId.put(emitter.getEntityId(), currentTick + socialAlertSettings.cooldownTicks());
        }
        }
    }

    private boolean isProvocationBlocked(Pig Pig, boolean naturalPig) {
        if (Pig == null) {
            return true;
        }
        if (isWorldDisallowed(Pig.getWorld()) || Pig.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return true;
        }
        ensureActivePigCachesFresh();
        int worldActives = targetingIndex.activeInWorld(Pig.getWorld().getUID());
        int chunkActives = targetingIndex.activeInChunk(Pig);
        PigAggressionBrain selfBrain = brains.get(Pig.getEntityId());
        if (selfBrain != null
                && selfBrain.state != PigAggressionState.IDLE
                && selfBrain.targetUuid != null) {
            worldActives = Math.max(0, worldActives - 1);
            chunkActives = Math.max(0, chunkActives - 1);
        }
        if (chunkActives >= global.maxActiveHostilePerChunk()) {
            return true;
        }
        return worldActives >= global.maxActiveHostilePerWorld();
    }

    private boolean activatePigAggression(
            Pig Pig,
            Player aggressor,
            boolean naturalPig,
            int aggressionDurationTicks,
            HostilityCause hostilityCause
    ) {
        int PigId = Pig.getEntityId();
        trackedPigs.put(PigId, Pig);
        PigAggressionBrain brain = brains.get(PigId);
        if (brain == null) {
            brain = new PigAggressionBrain();
            brains.put(PigId, brain);
            resetProcessingCursor();
        }
        UUID nextTarget = aggressor.getUniqueId();
        if (isTargetOnReactivationCooldown(nextTarget, brain)) {
            return false;
        }
        if (activationPolicy.isActivationBlocked(Pig, aggressor, naturalPig(Pig, naturalPig))) {
            return false;
        }
        if (isRetargetBlocked(brain, nextTarget)) {
            return false;
        }
        captureOriginalVisualState(Pig, brain);
        UUID previousTarget = brain.targetUuid;
        if (previousTarget != null && !previousTarget.equals(nextTarget) && settings.retargetGraceTicks() > 0) {
            brain.ignoreTargetUuid = previousTarget;
            brain.ignoreTargetUntilTick = currentTick + settings.retargetGraceTicks();
        }
        targetingIndex.replaceTarget(previousTarget, nextTarget);
        brain.targetUuid = nextTarget;
        brain.aggressionUntilTick = Math.max(brain.aggressionUntilTick, currentTick + Math.max(1, aggressionDurationTicks));
        int persistenceTicks = Math.max(1, (int) Math.round(settings.forgetTargetAfterTicks() * environmentCache.context(Pig.getWorld()).modifiers().targetPersistenceMultiplier()));
        brain.forgetTargetAtTick = Math.max(brain.forgetTargetAtTick, currentTick + persistenceTicks);
        brain.warningUntilTick = currentTick + settings.warningDurationTicks();
        transitionState(brain, settings.warningDurationTicks() > 0 ? PigAggressionState.ALERT : PigAggressionState.CHASE);
        brain.lastAttackTick = Long.MIN_VALUE;
        brain.lastAttackWallTimeMs = Long.MIN_VALUE;
        brain.nextChargeTick = currentTick + randomChargeDelay();
        brain.nextTargetRefreshTick = currentTick + TARGET_REFRESH_INTERVAL_TICKS;
        brain.lastLineOfSightCheckTick = Long.MIN_VALUE;
        brain.nextMovementUpdateTick = currentTick;
        brain.nextParticleTick = nextParticleTick(PigId);
        brain.hostilityCause = hostilityCause;

        Pig.setTarget(aggressor);
        Pig.setAggressive(true);
        faceTarget(Pig, aggressor);
        playWarningAudio(Pig, aggressor);
        invalidateActivePigCache();
        return true;
    }

    void removePig(int PigId) {
        synchronized (stateLock) {
        Pig Pig = trackedPigs.get(PigId);
        PigAggressionBrain removedBrain = brains.remove(PigId);
        if (Pig != null && Pig.isValid()) {
            restorePigRuntimeState(Pig, removedBrain);
        }
        if (removedBrain != null) {
            if (Pig != null && Pig.isValid()) {
                targetingIndex.unregisterActive(Pig, removedBrain.targetUuid);
            } else {
                targetingIndex.replaceTarget(removedBrain.targetUuid, null);
            }
        }
        trackedPigs.remove(PigId);
        socialAlertCooldownUntilByPigId.remove(PigId);
        originalMovementSpeedByPigId.remove(PigId);
        activationPolicy.forget(PigId);
        resetProcessingCursor();
        invalidateActivePigCache();
        }
    }

    void removeTarget(UUID targetId) {
        synchronized (stateLock) {
        if (targetId == null) {
            return;
        }
        for (var iterator = brains.int2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            PigAggressionBrain brain = entry.getValue();
            if (!targetId.equals(brain.targetUuid)) {
                continue;
            }
            Pig Pig = trackedPigs.get(entry.getIntKey());
            if (Pig != null && Pig.isValid()) {
                calmDown(Pig, brain);
            } else {
                targetingIndex.replaceTarget(brain.targetUuid, null);
            }
            trackedPigs.remove(entry.getIntKey());
            originalMovementSpeedByPigId.remove(entry.getIntKey());
            iterator.remove();
            resetProcessingCursor();
            invalidateActivePigCache();
        }
        }
    }

    void clearAll() {
        synchronized (stateLock) {
        for (var entry : brains.int2ObjectEntrySet()) {
            Pig Pig = trackedPigs.get(entry.getIntKey());
            if (Pig != null && Pig.isValid()) {
                calmDown(Pig, entry.getValue());
            }
        }
        brains.clear();
        trackedPigs.clear();
        targetingIndex.clear();
        milkTriggerCooldownUntilByPlayer.clear();
        resourceTriggerCooldownUntilByPlayer.clear();
        socialAlertCooldownUntilByPigId.clear();
        targetReactivationCooldownUntil.clear();
        originalMovementSpeedByPigId.clear();
        activationPolicy.clear();
        resetProcessingCursor();
        invalidateActivePigCache();
        }
    }

    long currentTick() {
        synchronized (stateLock) {
            return currentTick;
        }
    }

    private void processState(Pig Pig, Player target, PigAggressionBrain brain) {
        switch (brain.state) {
            case IDLE -> transitionState(brain, PigAggressionState.ALERT);
            case ALERT -> handleAlert(Pig, target, brain);
            case CHASE -> handleChase(Pig, target, brain);
            case ATTACK -> handleAttack(Pig, target, brain);
        }
    }

    private void handleAlert(Pig Pig, Player target, PigAggressionBrain brain) {
        faceTarget(Pig, target);
        setAggressiveMovement(Pig, brain, 0.0D);
        if (currentTick < brain.warningUntilTick) {
            return;
        }
        transitionState(brain, PigAggressionState.CHASE);
        playChargeSound(Pig, target);
    }

    private void handleChase(Pig Pig, Player target, PigAggressionBrain brain) {
        if (!pursue(Pig, target, brain)) {
            return;
        }
        if (isWithinAttackWindow(Pig, target)) {
            transitionState(brain, PigAggressionState.ATTACK);
        }
    }

    private void handleAttack(Pig Pig, Player target, PigAggressionBrain brain) {
        if (!isWithinAttackWindow(Pig, target)) {
            transitionState(brain, PigAggressionState.CHASE);
            return;
        }
        if (!tryAttack(Pig, target, brain)) {
            return;
        }
        transitionState(brain, PigAggressionState.CHASE);
    }

    private boolean pursue(Pig Pig, Player target, PigAggressionBrain brain) {
        if (settings.requireLineOfSight() && !hasLineOfSight(Pig, target, brain)) {
            if (currentTick > brain.forgetTargetAtTick) {
                calmDown(Pig, brain);
            }
            return false;
        }

        double distanceSq = distanceSq(Pig, target);
        EnvironmentAggressionModifiers env = environmentCache.context(Pig.getWorld()).modifiers();
        double effectiveDetectionRangeSq = effectiveDetectionRangeSq(settings.detectionRange(), env);
        if (distanceSq > effectiveDetectionRangeSq || Math.abs(Pig.getY() - target.getY()) > MAX_VERTICAL_GAP) {
            if (currentTick > brain.forgetTargetAtTick) {
                calmDown(Pig, brain);
            }
            return false;
        }

        if (currentTick >= brain.nextTargetRefreshTick) {
            Pig.setTarget(target);
            Pig.setAggressive(true);
            brain.nextTargetRefreshTick = currentTick + TARGET_REFRESH_INTERVAL_TICKS;
        }
        setAggressiveMovement(Pig, brain, normalizedIntensity(brain));

        scratch.setX(target.getX() - Pig.getX());
        scratch.setY(0.0D);
        scratch.setZ(target.getZ() - Pig.getZ());
        double length = scratch.length();
        if (length <= 0.001D) {
            return true;
        }

        scratch.multiply(1.0D / length);
        double push = FORWARD_PUSH;
        if (settings.chargeEnabled() && currentTick >= brain.nextChargeTick) {
            push += settings.chargeExtraPush();
            brain.nextChargeTick = currentTick + randomChargeDelay();
            playStompSound(Pig);
        }
        scratch.multiply(push);

        Vector currentVelocity = Pig.getVelocity();
        currentVelocity.setX(currentVelocity.getX() * 0.65D + scratch.getX());
        currentVelocity.setZ(currentVelocity.getZ() * 0.65D + scratch.getZ());
        Pig.setVelocity(currentVelocity);
        return true;
    }

    private boolean tryAttack(Pig Pig, Player target, PigAggressionBrain brain) {
        if (!isWithinAttackWindow(Pig, target)) {
            return false;
        }
        long nowMs = System.currentTimeMillis();
        long effectiveCooldownTicks = Math.max(1L, Math.round(settings.attackCooldownTicks() * environmentCache.context(Pig.getWorld()).modifiers().attackCooldownMultiplier()));
        long cooldownMs = Math.max(50L, effectiveCooldownTicks * 50L);
        if (brain.lastAttackWallTimeMs != Long.MIN_VALUE && nowMs - brain.lastAttackWallTimeMs < cooldownMs) {
            return false;
        }
        double envDamageMultiplier = environmentCache.context(Pig.getWorld()).modifiers().attackDamageMultiplier();
        double finalDamage = settings.attackDamage() * resolveDamageMultiplier(Pig.getWorld()) * envDamageMultiplier;
        if (finalDamage <= 0.0D) {
            return false;
        }

        brain.lastAttackTick = currentTick;
        brain.lastAttackWallTimeMs = nowMs;

        HostilityContextTracker.record(target.getUniqueId(), HostileSpecies.PIG, brain.hostilityCause);
        target.damage(finalDamage, Pig);
        double dx = target.getX() - Pig.getX();
        double dz = target.getZ() - Pig.getZ();
        double lengthSq = dx * dx + dz * dz;
        if (lengthSq > 0.0001D) {
            double scale = settings.knockbackStrength() / Math.sqrt(lengthSq);
            Vector velocity = target.getVelocity();
            velocity.setX(velocity.getX() + (dx * scale));
            velocity.setZ(velocity.getZ() + (dz * scale));
            target.setVelocity(velocity);
        }
        playAttackSound(Pig, target);
        return true;
    }

    private boolean isWithinAttackWindow(Pig Pig, Player target) {
        double dy = Math.abs(Pig.getY() - target.getY());
        if (dy > ATTACK_VERTICAL_TOLERANCE) {
            return false;
        }
        double dx = Pig.getX() - target.getX();
        double dz = Pig.getZ() - target.getZ();
        return (dx * dx + dz * dz) <= ATTACK_RANGE_SQ;
    }

    private Player resolveTarget(UUID targetUuid) {
        if (targetUuid == null) {
            return null;
        }
        Player player = org.bukkit.Bukkit.getPlayer(targetUuid);
        if (!targetEligibilityService.isEligible(player, global.worldFilter(), currentTick)) {
            return null;
        }
        return player;
    }

    private void setAggressiveMovement(Pig Pig, PigAggressionBrain brain, double intensity) {
        if (currentTick < brain.nextMovementUpdateTick) {
            return;
        }
        AttributeInstance movement = Pig.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }
        int PigId = Pig.getEntityId();
        double originalBase = originalMovementSpeedByPigId.get(PigId);
        if (Double.isNaN(originalBase)) {
            originalBase = movement.getBaseValue();
            originalMovementSpeedByPigId.put(PigId, originalBase);
        }
        double envSpeedMultiplier = environmentCache.context(Pig.getWorld()).modifiers().movementSpeedMultiplier();
        double desired = originalBase * (1.0D + (((settings.speedMultiplier() * envSpeedMultiplier) - 1.0D) * Math.max(0.0D, intensity)));
        desired = Math.clamp(desired, 0.05D, 0.8D);
        if (Double.isNaN(brain.lastMovementBaseValue)
                || Math.abs(brain.lastMovementBaseValue - desired) >= MOVEMENT_ATTRIBUTE_EPSILON) {
            movement.setBaseValue(desired);
            brain.lastMovementBaseValue = desired;
        }
        brain.nextMovementUpdateTick = currentTick + MOVEMENT_ATTRIBUTE_UPDATE_INTERVAL_TICKS;
    }

    private void calmDown(Pig Pig, PigAggressionBrain brain) {
        UUID cooledTarget = brain.targetUuid;
        targetingIndex.replaceTarget(brain.targetUuid, null);
        Pig.setTarget(null);
        Pig.setAggressive(false);
        restorePigRuntimeState(Pig, brain);
        brain.targetUuid = null;
        transitionState(brain, PigAggressionState.IDLE);
        applyTargetReactivationCooldown(cooledTarget);
        invalidateActivePigCache();
    }

    private void transitionState(PigAggressionBrain brain, PigAggressionState nextState) {
        if (brain.state == nextState) {
            return;
        }
        brain.state = nextState;
        brain.stateStartedTick = currentTick;
    }

    private boolean hasLineOfSight(Pig Pig, Player target, PigAggressionBrain brain) {
        if (brain.lastLineOfSightCheckTick == Long.MIN_VALUE
                || currentTick - brain.lastLineOfSightCheckTick >= LINE_OF_SIGHT_CACHE_TICKS) {
            brain.lastLineOfSightResult = Pig.hasLineOfSight(target);
            brain.lastLineOfSightCheckTick = currentTick;
        }
        return brain.lastLineOfSightResult;
    }

    private boolean isRetargetBlocked(PigAggressionBrain brain, UUID candidateTargetUuid) {
        if (brain.ignoreTargetUuid == null) {
            return false;
        }
        if (currentTick >= brain.ignoreTargetUntilTick) {
            brain.ignoreTargetUuid = null;
            brain.ignoreTargetUntilTick = Long.MIN_VALUE;
            return false;
        }
        return brain.ignoreTargetUuid.equals(candidateTargetUuid);
    }

    private void faceTarget(Pig Pig, Player target) {
        double dx = target.getX() - Pig.getX();
        double dz = target.getZ() - Pig.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        Pig.setRotation(yaw, Pig.getPitch());
    }

    private double normalizedIntensity(PigAggressionBrain brain) {
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
        if (milkTriggerCooldownUntilByPlayer.isEmpty() || currentTick % 40L != 0L) {
            return;
        }
        cleanupCooldownMap(milkTriggerCooldownUntilByPlayer);
    }

    private void cleanupResourceCooldownMap() {
        if (resourceTriggerCooldownUntilByPlayer.isEmpty() || currentTick % 40L != 0L) {
            return;
        }
        cleanupCooldownMap(resourceTriggerCooldownUntilByPlayer);
    }

    private void cleanupTargetCooldownMap() {
        if (targetReactivationCooldownUntil.isEmpty()) {
            return;
        }
        targetReactivationCooldownUntil.object2LongEntrySet().removeIf(entry -> currentTick >= entry.getLongValue());
    }

    private boolean isCooldownActive(Map<UUID, Int2LongOpenHashMap> cooldowns, UUID playerId, int PigId) {
        Int2LongOpenHashMap cooldownByPigId = cooldowns.get(playerId);
        return cooldownByPigId != null && currentTick < cooldownByPigId.get(PigId);
    }

    private void putCooldown(Map<UUID, Int2LongOpenHashMap> cooldowns, UUID playerId, int PigId, long untilTick) {
        Int2LongOpenHashMap cooldownByPigId = cooldowns.get(playerId);
        if (cooldownByPigId == null) {
            cooldownByPigId = new Int2LongOpenHashMap();
            cooldownByPigId.defaultReturnValue(Long.MIN_VALUE);
            cooldowns.put(playerId, cooldownByPigId);
        }
        cooldownByPigId.put(PigId, untilTick);
    }

    private void cleanupCooldownMap(Map<UUID, Int2LongOpenHashMap> cooldowns) {
        for (var iterator = cooldowns.entrySet().iterator(); iterator.hasNext(); ) {
            Int2LongOpenHashMap cooldownByPigId = iterator.next().getValue();
            cooldownByPigId.int2LongEntrySet().removeIf(entry -> currentTick >= entry.getLongValue());
            if (cooldownByPigId.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private boolean isTargetOnReactivationCooldown(UUID targetUuid, PigAggressionBrain brain) {
        if (targetUuid == null) {
            return false;
        }
        if (brain != null && targetUuid.equals(brain.targetUuid)) {
            return false;
        }
        return currentTick < targetReactivationCooldownUntil.getLong(targetUuid);
    }

    private void applyTargetReactivationCooldown(UUID targetUuid) {
        if (targetUuid == null || TARGET_REACTIVATION_COOLDOWN_TICKS <= 0) {
            return;
        }
        targetReactivationCooldownUntil.put(targetUuid, currentTick + TARGET_REACTIVATION_COOLDOWN_TICKS);
    }

    private boolean naturalPig(Pig pig, boolean providedNatural) {
        if (!global.onlyNatural()) {
            return true;
        }
        return pig != null && providedNatural;
    }

    private void playWarningAudio(Pig Pig, Player target) {
        if (!global.visualEffects().soundEnabled()) {
            return;
        }
        if (settings.playWarningSound()) {
            Pig.getWorld().playSound(Pig, Sound.ENTITY_PIG_AMBIENT, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.75F);
        }
        if (settings.playStompSound()) {
            Pig.getWorld().playSound(Pig, Sound.ENTITY_PIG_STEP, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.6F);
        }
        if (settings.playAggressiveSounds()) {
            target.playSound(target, Sound.ENTITY_PIG_HURT, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.85F);
        }
    }

    private void playChargeSound(Pig Pig, Player target) {
        if (!settings.playAggressiveSounds() || !global.visualEffects().soundEnabled()) {
            return;
        }
        Pig.getWorld().playSound(Pig, Sound.ENTITY_PIG_HURT, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.65F);
        target.playSound(target, Sound.ENTITY_PIG_HURT, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.75F);
    }

    private void playStompSound(Pig Pig) {
        if (!settings.playStompSound() || !global.visualEffects().soundEnabled()) {
            return;
        }
        Pig.getWorld().playSound(Pig, Sound.ENTITY_PIG_STEP, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.55F);
    }

    private void playAttackSound(Pig Pig, Player target) {
        if (!settings.playAggressiveSounds() || !global.visualEffects().soundEnabled()) {
            return;
        }
        Pig.getWorld().playSound(Pig, Sound.ENTITY_PIG_HURT, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.6F);
        target.playSound(target, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.9F);
    }

    private double distanceSq(Pig Pig, Player player) {
        double dx = Pig.getX() - player.getX();
        double dy = Pig.getY() - player.getY();
        double dz = Pig.getZ() - player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean isWorldDisallowed(World world) {
        return world == null || global.worldFilter().isWorldDisallowed(world.getName());
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

    private void applyVisualEffects(Pig Pig, PigAggressionBrain brain) {
        PigSettings.VisualEffectsSettings visual = global.visualEffects();
        if (visual.glowEnabled() && !Pig.isGlowing()) {
            Pig.setGlowing(true);
        }

        if (visual.particlesEnabled() && visual.particlesIntervalTicks() > 0 && currentTick >= brain.nextParticleTick) {
            int amount = Math.max(1, (int) Math.round(2.0D * visual.particlesIntensity()));
            Pig.getWorld().spawnParticle(
                    Particle.ANGRY_VILLAGER,
                    Pig.getX(),
                    Pig.getY() + 1.2D,
                    Pig.getZ(),
                    amount,
                    0.25D,
                    0.25D,
                    0.25D,
                    0.0D
            );
            brain.nextParticleTick = currentTick + visual.particlesIntervalTicks();
        }
    }

    private void restorePigRuntimeState(Pig Pig, PigAggressionBrain brain) {
        if (brain != null && brain.originalGlowCaptured && Pig.isGlowing() != brain.originallyGlowing) {
            Pig.setGlowing(brain.originallyGlowing);
        }
        restoreMovementBase(Pig, brain);
    }

    private void restoreMovementBase(Pig Pig, PigAggressionBrain brain) {
        AttributeInstance movement = Pig.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }
        int PigId = Pig.getEntityId();
        if (originalMovementSpeedByPigId.containsKey(PigId)) {
            double original = originalMovementSpeedByPigId.remove(PigId);
            movement.setBaseValue(original);
        }
        if (brain != null) {
            brain.lastMovementBaseValue = Double.NaN;
            brain.nextMovementUpdateTick = currentTick;
        }
    }

    private int nextProcessingPigId() {
        if (brains.isEmpty()) {
            processingCursor = null;
            return Integer.MIN_VALUE;
        }
        if (processingCursor == null || !processingCursor.hasNext()) {
            processingCursor = brains.keySet().iterator();
        }
        if (!processingCursor.hasNext()) {
            return Integer.MIN_VALUE;
        }
        return processingCursor.nextInt();
    }

    private void removeAggressionState(int PigId, Pig Pig, PigAggressionBrain brain, boolean calm) {
        if (calm && Pig != null && brain != null) {
            calmDown(Pig, brain);
        } else if (brain != null) {
            targetingIndex.replaceTarget(brain.targetUuid, null);
        }
        if (Pig != null && Pig.isValid() && brain != null && !calm) {
            restorePigRuntimeState(Pig, brain);
        }
        brains.remove(PigId);
        trackedPigs.remove(PigId);
        socialAlertCooldownUntilByPigId.remove(PigId);
        originalMovementSpeedByPigId.remove(PigId);
        activationPolicy.forget(PigId);
        resetProcessingCursor();
        invalidateActivePigCache();
    }

    private void captureOriginalVisualState(Pig Pig, PigAggressionBrain brain) {
        if (brain.originalGlowCaptured) {
            return;
        }
        brain.originallyGlowing = Pig.isGlowing();
        brain.originalGlowCaptured = true;
    }

    private long nextParticleTick(int PigId) {
        PigSettings.VisualEffectsSettings visual = global.visualEffects();
        if (!visual.particlesEnabled() || visual.particlesIntervalTicks() <= 0) {
            return Long.MAX_VALUE;
        }
        return currentTick + Math.floorMod(PigId, visual.particlesIntervalTicks());
    }

    private void resetProcessingCursor() {
        processingCursor = null;
    }

    private void invalidateActivePigCache() {
        activeCacheTick = Long.MIN_VALUE;
    }

    private void ensureActivePigCachesFresh() {
        if (activeCacheTick == currentTick) {
            return;
        }
        rebuildActivePigCaches();
    }

    private static double effectiveDetectionRangeSq(double baseDetectionRange, EnvironmentAggressionModifiers env) {
        double effectiveRange = Math.max(1.0D, (baseDetectionRange * env.detectionRadiusMultiplier()) + env.detectionRadiusBonus());
        return effectiveRange * effectiveRange;
    }
    private void rebuildActivePigCaches() {
        targetingIndex.clear();
        for (Pig Pig : trackedPigs.values()) {
            if (Pig == null || !Pig.isValid() || Pig.isDead()) {
                continue;
            }
            PigAggressionBrain brain = brains.get(Pig.getEntityId());
            targetingIndex.registerActive(Pig, brain == null ? null : brain.targetUuid);
        }
        activeCacheTick = currentTick;
    }

}





