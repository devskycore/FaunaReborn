package io.github.devskycore.faunareborn.animal.cow.hostility;
import io.github.devskycore.faunareborn.animal.common.settings.CommonGlobalHostilitySettings;
import io.github.devskycore.faunareborn.animal.common.settings.CommonSocialAlertSettings;

import io.github.devskycore.faunareborn.animal.cow.CowSettings;
import io.github.devskycore.faunareborn.animal.common.settings.SharedVisualEffectsSettings;
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
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import io.github.devskycore.faunareborn.system.environment.WorldEnvironmentContextCache;
import io.github.devskycore.faunareborn.system.environment.EnvironmentAggressionModifiers;
import io.github.devskycore.faunareborn.system.lod.LodResolver;
import io.github.devskycore.faunareborn.system.lod.LodSettings;
import io.github.devskycore.faunareborn.system.lod.LodTier;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class CowMilkAggressionController {

    private static final double ATTACK_RANGE = 2.1D;
    private static final double ATTACK_RANGE_SQ = ATTACK_RANGE * ATTACK_RANGE;
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
    private final CowSettings.MilkProvocationSettings settings;
    private final CommonGlobalHostilitySettings global;
    private final LodSettings lodSettings;
    private final TargetEligibilityService targetEligibilityService;
    private final CowActivationPolicy activationPolicy;
    private final WorldEnvironmentContextCache environmentCache;
    private final CowTargetingIndex targetingIndex = new CowTargetingIndex();
    private final Int2ObjectOpenHashMap<CowMilkAggressionBrain> brains = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<Cow> trackedCows = new Int2ObjectOpenHashMap<>();
    private final Map<UUID, Int2LongOpenHashMap> milkTriggerCooldownUntilByPlayer = new java.util.HashMap<>();
    private final Map<UUID, Int2LongOpenHashMap> resourceTriggerCooldownUntilByPlayer = new java.util.HashMap<>();
    private final Int2DoubleOpenHashMap originalMovementSpeedByCowId = new Int2DoubleOpenHashMap();
    private final Int2LongOpenHashMap socialAlertCooldownUntilByCowId = new Int2LongOpenHashMap();
    private final Object2LongOpenHashMap<UUID> targetReactivationCooldownUntil = new Object2LongOpenHashMap<>();
    private final Vector scratch = new Vector();
    private IntIterator processingCursor;
    private long currentTick;
    private long activeCacheTick = Long.MIN_VALUE;

    CowMilkAggressionController(
            SchedulerAdapter scheduler,
            CowSettings.MilkProvocationSettings settings,
            CommonGlobalHostilitySettings global,
            LodSettings lodSettings,
            WorldEnvironmentContextCache environmentCache
    ) {
        this.scheduler = scheduler;
        this.folia = RuntimePlatform.isFolia();
        this.settings = settings;
        this.global = global;
        this.lodSettings = lodSettings;
        this.targetEligibilityService = new TargetEligibilityService(global.targeting());
        this.activationPolicy = new CowActivationPolicy(settings, global, targetEligibilityService);
        this.environmentCache = environmentCache;
        this.socialAlertCooldownUntilByCowId.defaultReturnValue(Long.MIN_VALUE);
        this.originalMovementSpeedByCowId.defaultReturnValue(Double.NaN);
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
            int cowId = nextProcessingCowId();
            if (cowId == Integer.MIN_VALUE) {
                break;
            }
            scanned++;

            CowMilkAggressionBrain brain = brains.get(cowId);
            if (brain == null) {
                continue;
            }
            Cow cow = trackedCows.get(cowId);
            if (cow == null || !cow.isValid() || cow.isDead()) {
                removeAggressionState(cowId, cow, brain, false);
                continue;
            }

            if (isWorldDisallowed(cow.getWorld()) || cow.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
                removeAggressionState(cowId, cow, brain, true);
                continue;
            }

            Player target = resolveTarget(brain.targetUuid);
            if (target == null || target.getWorld() != cow.getWorld()) {
                removeAggressionState(cowId, cow, brain, true);
                continue;
            }

            if (currentTick > brain.aggressionUntilTick) {
                removeAggressionState(cowId, cow, brain, true);
                continue;
            }
            if (currentTick < brain.nextProcessTick) {
                continue;
            }

            processed++;
            updateLodState(cow, target, brain);
            applyVisualEffects(cow, brain);
            processState(cow, target, brain);
            brain.nextProcessTick = currentTick + lodSettings.intervalFor(brain.lodTier);

            if (brain.targetUuid == null) {
                removeAggressionState(cowId, cow, brain, false);
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
                int cowId = nextProcessingCowId();
                if (cowId == Integer.MIN_VALUE) {
                    break;
                }
                scanned++;
                processIds.add(cowId);
                processed++;
            }
        }

        for (int cowId : processIds) {
            Cow cow;
            synchronized (stateLock) {
                cow = trackedCows.get(cowId);
            }
            if (cow == null) {
                continue;
            }
            scheduler.runForEntity(cow, () -> processCowFolia(cowId, tickSnapshot));
        }
    }

    private void processCowFolia(int cowId, long tickSnapshot) {
        synchronized (stateLock) {
            if (currentTick < tickSnapshot) {
                return;
            }
            CowMilkAggressionBrain brain = brains.get(cowId);
            if (brain == null) {
                return;
            }
            Cow cow = trackedCows.get(cowId);
            if (cow == null || !cow.isValid() || cow.isDead()) {
                removeAggressionState(cowId, cow, brain, false);
                return;
            }

            if (isWorldDisallowed(cow.getWorld()) || cow.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
                removeAggressionState(cowId, cow, brain, true);
                return;
            }

            Player target = resolveTarget(brain.targetUuid);
            if (target == null || target.getWorld() != cow.getWorld()) {
                removeAggressionState(cowId, cow, brain, true);
                return;
            }

            if (currentTick > brain.aggressionUntilTick) {
                removeAggressionState(cowId, cow, brain, true);
                return;
            }
            if (currentTick < brain.nextProcessTick) {
                return;
            }

            updateLodState(cow, target, brain);
            applyVisualEffects(cow, brain);
            processState(cow, target, brain);
            brain.nextProcessTick = currentTick + lodSettings.intervalFor(brain.lodTier);

            if (brain.targetUuid == null) {
                removeAggressionState(cowId, cow, brain, false);
            }
        }
    }

    void provokeCowFromMilking(Cow cow, Player aggressor, boolean naturalCow) {
        synchronized (stateLock) {
        if (isProvocationBlocked(cow)) {
            return;
        }

        UUID playerId = aggressor.getUniqueId();
        int cowId = cow.getEntityId();
        if (isCooldownActive(milkTriggerCooldownUntilByPlayer, playerId, cowId)) {
            return;
        }
        if (settings.milkingTriggerCooldownTicks() > 0) {
            putCooldown(milkTriggerCooldownUntilByPlayer, playerId, cowId, currentTick + settings.milkingTriggerCooldownTicks());
        }

        activateCowAggression(cow, aggressor, naturalCow, settings.aggressionDurationTicks(), HostilityCause.MILKING_PROVOCATION);
        }
    }

    void provokeCowFromDamage(Cow cow, Player aggressor, boolean naturalCow) {
        synchronized (stateLock) {
        activateCowAggression(
                cow,
                aggressor,
                naturalCow,
                settings.aggressionDurationTicks(),
                HostilityCause.HERD_RETALIATION_DAMAGE
        );
        }
    }

    boolean provokeCowFromResources(
            Cow cow,
            Player aggressor,
            boolean naturalCow,
            int triggerCooldownTicks,
            int aggressionDurationTicks
    ) {
        return provokeCowFromResources(
                cow,
                aggressor,
                naturalCow,
                triggerCooldownTicks,
                aggressionDurationTicks,
                HostilityCause.TERRITORIAL_PICKUP
        );
    }

    boolean provokeCowFromResources(
            Cow cow,
            Player aggressor,
            boolean naturalCow,
            int triggerCooldownTicks,
            int aggressionDurationTicks,
            HostilityCause hostilityCause
    ) {
        synchronized (stateLock) {
        if (isProvocationBlocked(cow)) {
            return false;
        }
        UUID playerId = aggressor.getUniqueId();
        int cowId = cow.getEntityId();
        if (isCooldownActive(resourceTriggerCooldownUntilByPlayer, playerId, cowId)) {
            return false;
        }
        if (triggerCooldownTicks > 0) {
            putCooldown(resourceTriggerCooldownUntilByPlayer, playerId, cowId, currentTick + triggerCooldownTicks);
        }
        return activateCowAggression(cow, aggressor, naturalCow, aggressionDurationTicks, hostilityCause);
        }
    }

    void provokeNearbyCowsFromSocialAlert(
            Cow emitter,
            Player aggressor,
            List<Entity> nearbyEntities,
            CommonSocialAlertSettings socialAlertSettings,
            java.util.function.Predicate<Cow> naturalCowPredicate,
            HostilityCause hostilityCause
    ) {
        synchronized (stateLock) {
        if (!socialAlertSettings.enabled() || socialAlertSettings.maxResponders() <= 0) {
            return;
        }
        if (emitter == null || aggressor == null || nearbyEntities == null || nearbyEntities.isEmpty()) {
            return;
        }
        if (targetEligibilityService.isIneligible(aggressor, global.worldFilter(), -1L)) {
            return;
        }

        long cooldownUntil = socialAlertCooldownUntilByCowId.get(emitter.getEntityId());
        if (currentTick < cooldownUntil) {
            return;
        }

        UUID aggressorId = aggressor.getUniqueId();
        int recruited = 0;
        ensureActiveCowCachesFresh();
        for (org.bukkit.entity.Entity entity : nearbyEntities) {
            if (!(entity instanceof Cow ally) || ally.getEntityId() == emitter.getEntityId()) {
                continue;
            }
            if (targetingIndex.attackersForTarget(aggressorId) >= socialAlertSettings.maxResponders()) {
                break;
            }
            if (socialAlertSettings.responderAdultsOnly() && !ally.isAdult()) {
                continue;
            }
            if (isProvocationBlocked(ally)) {
                continue;
            }
            CowMilkAggressionBrain allyBrain = brains.get(ally.getEntityId());
            if (allyBrain != null && currentTick < allyBrain.socialAlertBlockedUntilTick) {
                continue;
            }
            if (!activateCowAggression(ally, aggressor, naturalCowPredicate.test(ally), settings.aggressionDurationTicks(), hostilityCause)) {
                continue;
            }
            if (socialAlertSettings.joinCooldownTicks() > 0) {
                CowMilkAggressionBrain recruitedBrain = brains.get(ally.getEntityId());
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
            socialAlertCooldownUntilByCowId.put(emitter.getEntityId(), currentTick + socialAlertSettings.cooldownTicks());
        }
        }
    }

    private boolean isProvocationBlocked(Cow cow) {
        if (cow == null) {
            return true;
        }
        if (isWorldDisallowed(cow.getWorld()) || cow.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return true;
        }
        ensureActiveCowCachesFresh();
        int worldActives = targetingIndex.activeInWorld(cow.getWorld().getUID());
        int chunkActives = targetingIndex.activeInChunk(cow);
        CowMilkAggressionBrain selfBrain = brains.get(cow.getEntityId());
        if (selfBrain != null
                && selfBrain.state != CowMilkAggressionState.IDLE
                && selfBrain.targetUuid != null) {
            worldActives = Math.max(0, worldActives - 1);
            chunkActives = Math.max(0, chunkActives - 1);
        }
        if (chunkActives >= global.maxActiveHostilePerChunk()) {
            return true;
        }
        return worldActives >= global.maxActiveHostilePerWorld();
    }

    private boolean activateCowAggression(
            Cow cow,
            Player aggressor,
            boolean naturalCow,
            int aggressionDurationTicks,
            HostilityCause hostilityCause
    ) {
        int cowId = cow.getEntityId();
        trackedCows.put(cowId, cow);
        CowMilkAggressionBrain brain = brains.get(cowId);
        if (brain == null) {
            brain = new CowMilkAggressionBrain();
            brains.put(cowId, brain);
            resetProcessingCursor();
        }
        UUID nextTarget = aggressor.getUniqueId();
        if (isTargetOnReactivationCooldown(nextTarget, brain)) {
            return false;
        }
        if (activationPolicy.isActivationBlocked(cow, aggressor, naturalCow(cow, naturalCow))) {
            return false;
        }
        if (isRetargetBlocked(brain, nextTarget)) {
            return false;
        }
        captureOriginalVisualState(cow, brain);
        UUID previousTarget = brain.targetUuid;
        if (previousTarget != null && !previousTarget.equals(nextTarget) && settings.retargetGraceTicks() > 0) {
            brain.ignoreTargetUuid = previousTarget;
            brain.ignoreTargetUntilTick = currentTick + settings.retargetGraceTicks();
        }
        targetingIndex.replaceTarget(previousTarget, nextTarget);
        brain.targetUuid = nextTarget;
        brain.aggressionUntilTick = Math.max(brain.aggressionUntilTick, currentTick + Math.max(1, aggressionDurationTicks));
        int persistenceTicks = Math.max(1, (int) Math.round(settings.forgetTargetAfterTicks() * environmentCache.context(cow.getWorld()).modifiers().targetPersistenceMultiplier()));
        brain.forgetTargetAtTick = Math.max(brain.forgetTargetAtTick, currentTick + persistenceTicks);
        brain.warningUntilTick = currentTick + settings.warningDurationTicks();
        transitionState(brain, settings.warningDurationTicks() > 0 ? CowMilkAggressionState.ALERT : CowMilkAggressionState.CHASE);
        brain.lastAttackTick = Long.MIN_VALUE;
        brain.nextChargeTick = currentTick + randomChargeDelay();
        brain.nextTargetRefreshTick = currentTick + TARGET_REFRESH_INTERVAL_TICKS;
        brain.lastLineOfSightCheckTick = Long.MIN_VALUE;
        brain.nextMovementUpdateTick = currentTick;
        brain.nextParticleTick = nextParticleTick(cowId);
        brain.hostilityCause = hostilityCause;
        HostilityContextTracker.record(nextTarget, HostileSpecies.COW, hostilityCause);

        cow.setTarget(aggressor);
        cow.setAggressive(true);
        faceTarget(cow, aggressor);
        playWarningAudio(cow, aggressor);
        invalidateActiveCowCache();
        return true;
    }

    void removeCow(int cowId) {
        synchronized (stateLock) {
        Cow cow = trackedCows.get(cowId);
        CowMilkAggressionBrain removedBrain = brains.remove(cowId);
        if (cow != null && cow.isValid()) {
            restoreCowRuntimeState(cow, removedBrain);
        }
        if (removedBrain != null) {
            if (cow != null && cow.isValid()) {
                targetingIndex.unregisterActive(cow, removedBrain.targetUuid);
            } else {
                targetingIndex.replaceTarget(removedBrain.targetUuid, null);
            }
        }
        trackedCows.remove(cowId);
        socialAlertCooldownUntilByCowId.remove(cowId);
        originalMovementSpeedByCowId.remove(cowId);
        activationPolicy.forget(cowId);
        resetProcessingCursor();
        invalidateActiveCowCache();
        }
    }

    void removeTarget(UUID targetId) {
        synchronized (stateLock) {
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
            if (cow != null && cow.isValid()) {
                calmDown(cow, brain);
            } else {
                targetingIndex.replaceTarget(brain.targetUuid, null);
            }
            trackedCows.remove(entry.getIntKey());
            originalMovementSpeedByCowId.remove(entry.getIntKey());
            iterator.remove();
            resetProcessingCursor();
            invalidateActiveCowCache();
        }
        }
    }

    void clearAll() {
        synchronized (stateLock) {
        for (var entry : brains.int2ObjectEntrySet()) {
            Cow cow = trackedCows.get(entry.getIntKey());
            if (cow != null && cow.isValid()) {
                calmDown(cow, entry.getValue());
            }
        }
        brains.clear();
        trackedCows.clear();
        targetingIndex.clear();
        milkTriggerCooldownUntilByPlayer.clear();
        resourceTriggerCooldownUntilByPlayer.clear();
        socialAlertCooldownUntilByCowId.clear();
        targetReactivationCooldownUntil.clear();
        originalMovementSpeedByCowId.clear();
        activationPolicy.clear();
        resetProcessingCursor();
        invalidateActiveCowCache();
        }
    }

    long currentTick() {
        synchronized (stateLock) {
            return currentTick;
        }
    }

    private void processState(Cow cow, Player target, CowMilkAggressionBrain brain) {
        switch (brain.state) {
            case IDLE -> transitionState(brain, CowMilkAggressionState.ALERT);
            case ALERT -> handleAlert(cow, target, brain);
            case CHASE -> handleChase(cow, target, brain);
            case ATTACK -> handleAttack(cow, target, brain);
        }
    }

    private void handleAlert(Cow cow, Player target, CowMilkAggressionBrain brain) {
        faceTarget(cow, target);
        setAggressiveMovement(cow, brain, 0.0D);
        if (currentTick < brain.warningUntilTick) {
            return;
        }
        transitionState(brain, CowMilkAggressionState.CHASE);
        playChargeSound(cow, target);
    }

    private void handleChase(Cow cow, Player target, CowMilkAggressionBrain brain) {
        if (!pursue(cow, target, brain)) {
            return;
        }
        if (distanceSq(cow, target) <= ATTACK_RANGE_SQ) {
            transitionState(brain, CowMilkAggressionState.ATTACK);
        }
    }

    private void handleAttack(Cow cow, Player target, CowMilkAggressionBrain brain) {
        if (distanceSq(cow, target) > ATTACK_RANGE_SQ) {
            transitionState(brain, CowMilkAggressionState.CHASE);
            return;
        }
        if (!tryAttack(cow, target, brain)) {
            return;
        }
        transitionState(brain, CowMilkAggressionState.CHASE);
    }

    private boolean pursue(Cow cow, Player target, CowMilkAggressionBrain brain) {
        if (settings.requireLineOfSight() && !hasLineOfSight(cow, target, brain)) {
            if (currentTick > brain.forgetTargetAtTick) {
                calmDown(cow, brain);
            }
            return false;
        }

        double distanceSq = distanceSq(cow, target);
        EnvironmentAggressionModifiers env = environmentCache.context(cow.getWorld()).modifiers();
        double effectiveDetectionRangeSq = effectiveDetectionRangeSq(settings.detectionRange(), env);
        if (distanceSq > effectiveDetectionRangeSq || Math.abs(cow.getY() - target.getY()) > MAX_VERTICAL_GAP) {
            if (currentTick > brain.forgetTargetAtTick) {
                calmDown(cow, brain);
            }
            return false;
        }

        if (currentTick >= brain.nextTargetRefreshTick) {
            cow.setTarget(target);
            cow.setAggressive(true);
            brain.nextTargetRefreshTick = currentTick + TARGET_REFRESH_INTERVAL_TICKS;
        }
        setAggressiveMovement(cow, brain, normalizedIntensity(brain));

        scratch.setX(target.getX() - cow.getX());
        scratch.setY(0.0D);
        scratch.setZ(target.getZ() - cow.getZ());
        double length = scratch.length();
        if (length <= 0.001D) {
            return true;
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
        return true;
    }

    private boolean tryAttack(Cow cow, Player target, CowMilkAggressionBrain brain) {
        if (distanceSq(cow, target) > ATTACK_RANGE_SQ) {
            return false;
        }
        int effectiveAttackCooldownTicks = Math.max(1, (int) Math.round(settings.attackCooldownTicks() * environmentCache.context(cow.getWorld()).modifiers().attackCooldownMultiplier()));
        if (brain.lastAttackTick != Long.MIN_VALUE && currentTick - brain.lastAttackTick < effectiveAttackCooldownTicks) {
            return false;
        }
        brain.lastAttackTick = currentTick;

        double envDamageMultiplier = environmentCache.context(cow.getWorld()).modifiers().attackDamageMultiplier();
        HostilityContextTracker.record(target.getUniqueId(), HostileSpecies.COW, brain.hostilityCause);
        target.damage(settings.attackDamage() * resolveDamageMultiplier(cow.getWorld()) * envDamageMultiplier, cow);
        double dx = target.getX() - cow.getX();
        double dz = target.getZ() - cow.getZ();
        double lengthSq = dx * dx + dz * dz;
        if (lengthSq > 0.0001D) {
            double scale = settings.knockbackStrength() / Math.sqrt(lengthSq);
            Vector velocity = target.getVelocity();
            velocity.setX(velocity.getX() + (dx * scale));
            velocity.setZ(velocity.getZ() + (dz * scale));
            target.setVelocity(velocity);
        }
        playAttackSound(cow, target);
        return true;
    }

    private Player resolveTarget(UUID targetUuid) {
        if (targetUuid == null) {
            return null;
        }
        Player player = org.bukkit.Bukkit.getPlayer(targetUuid);
        if (targetEligibilityService.isIneligible(player, global.worldFilter(), currentTick)) {
            return null;
        }
        return player;
    }

    private void setAggressiveMovement(Cow cow, CowMilkAggressionBrain brain, double intensity) {
        if (currentTick < brain.nextMovementUpdateTick) {
            return;
        }
        AttributeInstance movement = cow.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }
        int cowId = cow.getEntityId();
        double originalBase = originalMovementSpeedByCowId.get(cowId);
        if (Double.isNaN(originalBase)) {
            originalBase = movement.getBaseValue();
            originalMovementSpeedByCowId.put(cowId, originalBase);
        }
        double envSpeedMultiplier = environmentCache.context(cow.getWorld()).modifiers().movementSpeedMultiplier();
        double desired = originalBase * (1.0D + (((settings.speedMultiplier() * envSpeedMultiplier) - 1.0D) * Math.max(0.0D, intensity)));
        desired = Math.clamp(desired, 0.05D, 0.8D);
        if (Double.isNaN(brain.lastMovementBaseValue)
                || Math.abs(brain.lastMovementBaseValue - desired) >= MOVEMENT_ATTRIBUTE_EPSILON) {
            movement.setBaseValue(desired);
            brain.lastMovementBaseValue = desired;
        }
        brain.nextMovementUpdateTick = currentTick + MOVEMENT_ATTRIBUTE_UPDATE_INTERVAL_TICKS;
    }

    private void calmDown(Cow cow, CowMilkAggressionBrain brain) {
        UUID cooledTarget = brain.targetUuid;
        targetingIndex.replaceTarget(brain.targetUuid, null);
        cow.setTarget(null);
        cow.setAggressive(false);
        restoreCowRuntimeState(cow, brain);
        brain.targetUuid = null;
        transitionState(brain, CowMilkAggressionState.IDLE);
        applyTargetReactivationCooldown(cooledTarget);
        invalidateActiveCowCache();
    }

    private void transitionState(CowMilkAggressionBrain brain, CowMilkAggressionState nextState) {
        if (brain.state == nextState) {
            return;
        }
        brain.state = nextState;
        brain.stateStartedTick = currentTick;
    }

    private boolean hasLineOfSight(Cow cow, Player target, CowMilkAggressionBrain brain) {
        if (brain.lastLineOfSightCheckTick == Long.MIN_VALUE
                || currentTick - brain.lastLineOfSightCheckTick >= LINE_OF_SIGHT_CACHE_TICKS) {
            brain.lastLineOfSightResult = cow.hasLineOfSight(target);
            brain.lastLineOfSightCheckTick = currentTick;
        }
        return brain.lastLineOfSightResult;
    }

    private boolean isRetargetBlocked(CowMilkAggressionBrain brain, UUID candidateTargetUuid) {
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

    private boolean isCooldownActive(Map<UUID, Int2LongOpenHashMap> cooldowns, UUID playerId, int cowId) {
        Int2LongOpenHashMap cooldownByCowId = cooldowns.get(playerId);
        return cooldownByCowId != null && currentTick < cooldownByCowId.get(cowId);
    }

    private void putCooldown(Map<UUID, Int2LongOpenHashMap> cooldowns, UUID playerId, int cowId, long untilTick) {
        Int2LongOpenHashMap cooldownByCowId = cooldowns.get(playerId);
        if (cooldownByCowId == null) {
            cooldownByCowId = new Int2LongOpenHashMap();
            cooldownByCowId.defaultReturnValue(Long.MIN_VALUE);
            cooldowns.put(playerId, cooldownByCowId);
        }
        cooldownByCowId.put(cowId, untilTick);
    }

    private void cleanupCooldownMap(Map<UUID, Int2LongOpenHashMap> cooldowns) {
        for (var iterator = cooldowns.entrySet().iterator(); iterator.hasNext(); ) {
            Int2LongOpenHashMap cooldownByCowId = iterator.next().getValue();
            cooldownByCowId.int2LongEntrySet().removeIf(entry -> currentTick >= entry.getLongValue());
            if (cooldownByCowId.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private boolean isTargetOnReactivationCooldown(UUID targetUuid, CowMilkAggressionBrain brain) {
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

    private boolean naturalCow(Cow cow, boolean providedNatural) {
        if (!global.onlyNatural()) {
            return true;
        }
        return cow != null && providedNatural;
    }

    private void playWarningAudio(Cow cow, Player target) {
        if (!global.visualEffects().soundEnabled()) {
            return;
        }
        if (settings.playWarningSound()) {
            cow.getWorld().playSound(cow, Sound.ENTITY_COW_AMBIENT, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.75F);
        }
        if (settings.playStompSound()) {
            cow.getWorld().playSound(cow, Sound.ENTITY_COW_STEP, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.6F);
        }
        if (settings.playAggressiveSounds()) {
            target.playSound(target, Sound.ENTITY_COW_HURT, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.85F);
        }
    }

    private void playChargeSound(Cow cow, Player target) {
        if (!settings.playAggressiveSounds() || !global.visualEffects().soundEnabled()) {
            return;
        }
        cow.getWorld().playSound(cow, Sound.ENTITY_COW_HURT, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.65F);
        target.playSound(target, Sound.ENTITY_COW_HURT, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.75F);
    }

    private void playStompSound(Cow cow) {
        if (!settings.playStompSound() || !global.visualEffects().soundEnabled()) {
            return;
        }
        cow.getWorld().playSound(cow, Sound.ENTITY_COW_STEP, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.55F);
    }

    private void playAttackSound(Cow cow, Player target) {
        if (!settings.playAggressiveSounds() || !global.visualEffects().soundEnabled()) {
            return;
        }
        cow.getWorld().playSound(cow, Sound.ENTITY_COW_HURT, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.6F);
        target.playSound(target, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.HOSTILE, (float) global.visualEffects().soundVolume(), 0.9F);
    }

    private double distanceSq(Cow cow, Player player) {
        double dx = cow.getX() - player.getX();
        double dy = cow.getY() - player.getY();
        double dz = cow.getZ() - player.getZ();
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

    private void applyVisualEffects(Cow cow, CowMilkAggressionBrain brain) {
        SharedVisualEffectsSettings visual = global.visualEffects();
        if (visual.glowEnabled() && !cow.isGlowing()) {
            cow.setGlowing(true);
        }

        if (brain.lodTier != LodTier.OFF
                && brain.lodTier != LodTier.LOW
                && visual.particlesEnabled()
                && visual.particlesIntervalTicks() > 0
                && currentTick >= brain.nextParticleTick) {
            int amount = Math.max(1, (int) Math.round(2.0D * visual.particlesIntensity()));
            cow.getWorld().spawnParticle(
                    Particle.ANGRY_VILLAGER,
                    cow.getX(),
                    cow.getY() + 1.2D,
                    cow.getZ(),
                    amount,
                    0.25D,
                    0.25D,
                    0.25D,
                    0.0D
            );
            int intervalMultiplier = brain.lodTier == LodTier.MEDIUM ? 2 : 1;
            long particleIntervalTicks = (long) visual.particlesIntervalTicks() * intervalMultiplier;
            brain.nextParticleTick = currentTick + particleIntervalTicks;
        }
    }

    private void restoreCowRuntimeState(Cow cow, CowMilkAggressionBrain brain) {
        if (brain != null && brain.originalGlowCaptured && cow.isGlowing() != brain.originallyGlowing) {
            cow.setGlowing(brain.originallyGlowing);
        }
        restoreMovementBase(cow, brain);
    }

    private void restoreMovementBase(Cow cow, CowMilkAggressionBrain brain) {
        AttributeInstance movement = cow.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }
        int cowId = cow.getEntityId();
        if (originalMovementSpeedByCowId.containsKey(cowId)) {
            double original = originalMovementSpeedByCowId.remove(cowId);
            movement.setBaseValue(original);
        }
        if (brain != null) {
            brain.lastMovementBaseValue = Double.NaN;
            brain.nextMovementUpdateTick = currentTick;
        }
    }

    private int nextProcessingCowId() {
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

    private void removeAggressionState(int cowId, Cow cow, CowMilkAggressionBrain brain, boolean calm) {
        if (calm && cow != null && brain != null) {
            calmDown(cow, brain);
        } else if (brain != null) {
            targetingIndex.replaceTarget(brain.targetUuid, null);
        }
        if (cow != null && cow.isValid() && brain != null && !calm) {
            restoreCowRuntimeState(cow, brain);
        }
        brains.remove(cowId);
        trackedCows.remove(cowId);
        socialAlertCooldownUntilByCowId.remove(cowId);
        originalMovementSpeedByCowId.remove(cowId);
        activationPolicy.forget(cowId);
        resetProcessingCursor();
        invalidateActiveCowCache();
    }

    private void captureOriginalVisualState(Cow cow, CowMilkAggressionBrain brain) {
        if (brain.originalGlowCaptured) {
            return;
        }
        brain.originallyGlowing = cow.isGlowing();
        brain.originalGlowCaptured = true;
    }

    private long nextParticleTick(int cowId) {
        SharedVisualEffectsSettings visual = global.visualEffects();
        if (!visual.particlesEnabled() || visual.particlesIntervalTicks() <= 0) {
            return Long.MAX_VALUE;
        }
        return currentTick + Math.floorMod(cowId, visual.particlesIntervalTicks());
    }

    private void resetProcessingCursor() {
        processingCursor = null;
    }

    private void invalidateActiveCowCache() {
        activeCacheTick = Long.MIN_VALUE;
    }

    private void ensureActiveCowCachesFresh() {
        if (activeCacheTick == currentTick) {
            return;
        }
        rebuildActiveCowCaches();
    }

    private static double effectiveDetectionRangeSq(double baseDetectionRange, EnvironmentAggressionModifiers env) {
        double effectiveRange = Math.max(1.0D, (baseDetectionRange * env.detectionRadiusMultiplier()) + env.detectionRadiusBonus());
        return effectiveRange * effectiveRange;
    }

    private void updateLodState(Cow cow, Player target, CowMilkAggressionBrain brain) {
        boolean forceHigh = brain.state == CowMilkAggressionState.ATTACK;
        double distanceSq = distanceSq(cow, target);
        brain.lodTier = LodResolver.resolveTier(lodSettings, brain.lodTier, distanceSq, forceHigh);
    }
    private void rebuildActiveCowCaches() {
        targetingIndex.clear();
        for (Cow cow : trackedCows.values()) {
            if (cow == null || !cow.isValid() || cow.isDead()) {
                continue;
            }
            CowMilkAggressionBrain brain = brains.get(cow.getEntityId());
            targetingIndex.registerActive(cow, brain == null ? null : brain.targetUuid);
        }
        activeCacheTick = currentTick;
    }

}




