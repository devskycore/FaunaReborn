package io.github.devskycore.faunareborn.animal.chicken.hostility;

import io.github.devskycore.faunareborn.animal.chicken.config.ItemPickupTerritorialityConfig;
import io.github.devskycore.faunareborn.combat.deathmessage.HostilityCause;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class TerritorialPickupService {

    private final boolean enabled;
    private final int eggThreshold;
    private final int featherThreshold;
    private final int rawChickenThreshold;
    private final double detectionRadius;
    private final double detectionRadiusSq;
    private final int timeWindowTicks;
    private final int aggressionDurationTicks;
    private final int maxItemAgeTicks;
    private final boolean nightModifierEnabled;
    private final double nightThresholdMultiplier;
    private final boolean socialPropagationEnabled;
    private final int maxSimultaneousAttackersPerTarget;
    private final WorldNightStateCache worldNightStateCache;
    private final SocialAlertService socialAlertService;
    private final ChickenRecruitment recruitment;
    private final TargetValidator targetValidator;
    private final Map<UUID, TerritorialPickupCounter> counters = new HashMap<>();

    TerritorialPickupService(
            ItemPickupTerritorialityConfig settings,
            int maxSimultaneousAttackersPerTarget,
            WorldNightStateCache worldNightStateCache,
            SocialAlertService socialAlertService,
            ChickenRecruitment recruitment,
            TargetValidator targetValidator
    ) {
        this.enabled = settings.enabled();
        this.eggThreshold = settings.eggThreshold();
        this.featherThreshold = settings.featherThreshold();
        this.rawChickenThreshold = settings.rawChickenThreshold();
        this.detectionRadius = settings.detectionRadius();
        this.detectionRadiusSq = detectionRadius * detectionRadius;
        this.timeWindowTicks = settings.timeWindowTicks();
        this.aggressionDurationTicks = settings.aggressionDurationTicks();
        this.maxItemAgeTicks = settings.maxItemAgeTicks();
        this.nightModifierEnabled = settings.nightModifierEnabled();
        this.nightThresholdMultiplier = settings.nightThresholdMultiplier();
        this.socialPropagationEnabled = settings.socialPropagationEnabled();
        this.maxSimultaneousAttackersPerTarget = maxSimultaneousAttackersPerTarget;
        this.worldNightStateCache = worldNightStateCache;
        this.socialAlertService = socialAlertService;
        this.recruitment = recruitment;
        this.targetValidator = targetValidator;
    }

    boolean isDisabled() {
        return !enabled;
    }

    double detectionRadius() {
        return detectionRadius;
    }

    int maxItemAgeTicks() {
        return maxItemAgeTicks;
    }

    boolean isNonTerritorialPickupMaterial(Material material) {
        return material != Material.EGG && material != Material.FEATHER && material != Material.CHICKEN;
    }

    void recordPickup(Player player, Material material, int amount, List<Entity> nearbyEntities, long currentTick) {
        if (!enabled) {
            return;
        }
        if (amount <= 0 || isNonTerritorialPickupMaterial(material)) {
            return;
        }
        if (player == null || !player.isOnline() || player.isDead()) {
            return;
        }

        int threshold = resolveItemPickupThreshold(material, player.getWorld());
        TerritorialPickupCounter counter = counters.computeIfAbsent(player.getUniqueId(), ignored -> new TerritorialPickupCounter());
        int updatedAmount = counter.add(material, amount, currentTick, timeWindowTicks);
        if (updatedAmount < threshold) {
            return;
        }

        counter.reset(material);
        triggerTerritorialItemAggression(player, nearbyEntities, currentTick);
    }

    boolean hasTerritorialWitness(Player player, Item item, List<Entity> nearbyEntities) {
        if (player == null || item == null || nearbyEntities.isEmpty()) {
            return false;
        }
        if (player.getWorld() != item.getWorld()) {
            return false;
        }
        if (HostilityDistances.distanceSq(player, item) > detectionRadiusSq) {
            return false;
        }

        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof Chicken chicken)) {
                continue;
            }
            if (cannotPerceiveTerritorialPickup(chicken, player)) {
                continue;
            }
            if (HostilityDistances.distanceSq(chicken, item) <= detectionRadiusSq) {
                return true;
            }
        }
        return false;
    }

    void cleanupCounters(long currentTick) {
        if (counters.isEmpty()) {
            return;
        }
        counters.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTick));
    }

    void removePlayer(UUID playerId) {
        counters.remove(playerId);
    }

    void clear() {
        counters.clear();
    }

    private void triggerTerritorialItemAggression(Player player, List<Entity> nearbyEntities, long currentTick) {
        if (nearbyEntities.isEmpty()) {
            return;
        }

        int recruited = 0;
        Chicken firstRecruit = null;
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof Chicken chicken)) {
                continue;
            }
            if (cannotPerceiveTerritorialPickup(chicken, player)) {
                continue;
            }
            if (!recruitment.tryRecruit(chicken, player, aggressionDurationTicks, true, HostilityCause.TERRITORIAL_PICKUP)) {
                continue;
            }

            if (firstRecruit == null) {
                firstRecruit = chicken;
            }
            recruited++;
            if (recruited >= maxSimultaneousAttackersPerTarget) {
                break;
            }
        }

        if (recruited > 0 && socialPropagationEnabled && socialAlertService.enabled()) {
            List<Entity> socialNearby = firstRecruit.getNearbyEntities(
                    socialAlertService.radius(),
                    socialAlertService.radius(),
                    socialAlertService.radius()
            );
            socialAlertService.emit(firstRecruit.getEntityId(), player, socialNearby, aggressionDurationTicks, currentTick, HostilityCause.TERRITORIAL_PICKUP);
        }
    }

    private boolean cannotPerceiveTerritorialPickup(Chicken chicken, Player player) {
        if (chicken == null || player == null) {
            return true;
        }
        if (!chicken.isValid() || chicken.isDead()) {
            return true;
        }
        if (targetValidator.isInvalidTarget(chicken, player)) {
            return true;
        }
        if (HostilityDistances.distanceSq(chicken, player) > detectionRadiusSq) {
            return true;
        }
        if (Math.abs(chicken.getY() - player.getY()) > ChickenHostilityConstants.MAX_VERTICAL_GAP) {
            return true;
        }
        return !chicken.hasLineOfSight(player);
    }

    private int resolveItemPickupThreshold(Material material, World world) {
        int threshold = switch (material) {
            case EGG -> eggThreshold;
            case FEATHER -> featherThreshold;
            case CHICKEN -> rawChickenThreshold;
            default -> Integer.MAX_VALUE;
        };
        if (!nightModifierEnabled || !worldNightStateCache.isNight(world)) {
            return threshold;
        }
        return Math.max(1, (int) Math.ceil(threshold * nightThresholdMultiplier));
    }

    interface ChickenRecruitment {
        boolean tryRecruit(Chicken chicken, Player aggressor, int aggressionDurationTicks, boolean applyJoinCooldown, HostilityCause hostilityCause);
    }

    interface TargetValidator {
        boolean isInvalidTarget(Chicken chicken, Player player);
    }

    private static final class TerritorialPickupCounter {
        private int eggAmount;
        private long eggExpiresTick;
        private int featherAmount;
        private long featherExpiresTick;
        private int rawChickenAmount;
        private long rawChickenExpiresTick;

        private int add(Material material, int amount, long currentTick, int windowTicks) {
            return switch (material) {
                case EGG -> {
                    if (currentTick > eggExpiresTick) {
                        eggAmount = 0;
                    }
                    eggAmount = boundedAdd(eggAmount, amount);
                    eggExpiresTick = currentTick + windowTicks;
                    yield eggAmount;
                }
                case FEATHER -> {
                    if (currentTick > featherExpiresTick) {
                        featherAmount = 0;
                    }
                    featherAmount = boundedAdd(featherAmount, amount);
                    featherExpiresTick = currentTick + windowTicks;
                    yield featherAmount;
                }
                case CHICKEN -> {
                    if (currentTick > rawChickenExpiresTick) {
                        rawChickenAmount = 0;
                    }
                    rawChickenAmount = boundedAdd(rawChickenAmount, amount);
                    rawChickenExpiresTick = currentTick + windowTicks;
                    yield rawChickenAmount;
                }
                default -> 0;
            };
        }

        private void reset(Material material) {
            switch (material) {
                case EGG -> {
                    eggAmount = 0;
                    eggExpiresTick = 0L;
                }
                case FEATHER -> {
                    featherAmount = 0;
                    featherExpiresTick = 0L;
                }
                case CHICKEN -> {
                    rawChickenAmount = 0;
                    rawChickenExpiresTick = 0L;
                }
                default -> {
                }
            }
        }

        private boolean isExpired(long currentTick) {
            return currentTick > eggExpiresTick
                    && currentTick > featherExpiresTick
                    && currentTick > rawChickenExpiresTick;
        }

        private static int boundedAdd(int current, int amount) {
            long result = (long) current + amount;
            return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
        }
    }
}

