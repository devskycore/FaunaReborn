package io.github.devskycore.faunareborn.animal.cow.hostility;

import io.github.devskycore.faunareborn.animal.cow.CowSettings;
import io.github.devskycore.faunareborn.combat.deathmessage.HostilityCause;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import io.github.devskycore.faunareborn.system.environment.WorldEnvironmentContextCache;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class CowTerritorialPickupService {

    private final CowSettings.ResourceProvocationSettings settings;
    private final CowSettings.SocialAlertSettings socialAlertSettings;
    private final CowMilkAggressionController aggressionController;
    private final NaturalCowResolver naturalCowResolver;
    private final boolean requireLineOfSight;
    private final WorldEnvironmentContextCache environmentCache;
    private final Map<UUID, TerritorialPickupCounter> counters = new ConcurrentHashMap<>();

    CowTerritorialPickupService(
            CowSettings.ResourceProvocationSettings settings,
            CowSettings.SocialAlertSettings socialAlertSettings,
            CowMilkAggressionController aggressionController,
            NaturalCowResolver naturalCowResolver,
            boolean requireLineOfSight,
            WorldEnvironmentContextCache environmentCache
    ) {
        this.settings = settings;
        this.socialAlertSettings = socialAlertSettings;
        this.aggressionController = aggressionController;
        this.naturalCowResolver = naturalCowResolver;
        this.requireLineOfSight = requireLineOfSight;
        this.environmentCache = environmentCache;
    }

    boolean isNonTerritorialMaterial(Material material) {
        return material != Material.LEATHER && material != Material.BEEF && material != Material.BONE;
    }

    void recordPickup(Player player, Material material, int amount, List<Entity> nearbyEntities, long currentTick) {
        if (!settings.enabled() || amount <= 0 || isNonTerritorialMaterial(material) || player == null) {
            return;
        }
        if (!player.isOnline() || player.isDead()) {
            return;
        }

        int threshold = resolveThreshold(material, player.getWorld());
        TerritorialPickupCounter counter = counters.computeIfAbsent(player.getUniqueId(), ignored -> new TerritorialPickupCounter());
        int updatedAmount = counter.add(material, amount, currentTick, settings.timeWindowTicks());
        if (updatedAmount < threshold) {
            return;
        }
        counter.reset(material);
        triggerAggression(player, nearbyEntities);
    }

    boolean hasTerritorialWitness(Player player, List<Entity> nearbyEntities) {
        if (player == null || nearbyEntities == null || nearbyEntities.isEmpty()) {
            return false;
        }
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Cow cow && canPerceiveTerritorialPickup(cow, player)) {
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

    void clear() {
        counters.clear();
    }

    void removePlayer(UUID playerId) {
        counters.remove(playerId);
    }

    private void triggerAggression(Player player, List<Entity> nearbyEntities) {
        if (nearbyEntities == null || nearbyEntities.isEmpty()) {
            return;
        }
        int recruited = 0;
        Cow firstRecruit = null;
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof Cow cow) || !canPerceiveTerritorialPickup(cow, player)) {
                continue;
            }
            if (!aggressionController.provokeCowFromResources(
                    cow,
                    player,
                    naturalCowResolver.isNaturalCow(cow),
                    settings.triggerCooldownTicks(),
                    settings.aggressionDurationTicks()
            )) {
                continue;
            }
            if (firstRecruit == null) {
                firstRecruit = cow;
            }
            recruited++;
            if (recruited >= settings.maxResponders()) {
                break;
            }
        }
        if (recruited > 0 && settings.socialPropagationEnabled()) {
            aggressionController.provokeNearbyCowsFromSocialAlert(
                    firstRecruit,
                    player,
                    firstRecruit.getNearbyEntities(socialAlertSettings.radius(), socialAlertSettings.radius(), socialAlertSettings.radius()),
                    socialAlertSettings,
                    naturalCowResolver::isNaturalCow,
                    HostilityCause.TERRITORIAL_PICKUP
            );
        }
    }

    private boolean canPerceiveTerritorialPickup(Cow cow, Player player) {
        if (cow == null || player == null) {
            return false;
        }
        if (!cow.isAdult() || !cow.isValid() || cow.isDead()) {
            return false;
        }
        if (cow.getWorld() != player.getWorld()) {
            return false;
        }
        if (Math.abs(cow.getY() - player.getY()) > 5.0D) {
            return false;
        }
        if (settings.detectionRadiusSq() < distanceSq(cow, player)) {
            return false;
        }
        return !requireLineOfSight || cow.hasLineOfSight(player);
    }

    private int resolveThreshold(Material material, World world) {
        int threshold = switch (material) {
            case LEATHER -> settings.leatherThreshold();
            case BEEF -> settings.rawBeefThreshold();
            case BONE -> settings.boneThreshold();
            default -> Integer.MAX_VALUE;
        };
        if (!settings.nightModifierEnabled() || world == null || !isNight(world)) {
            return threshold;
        }
        return Math.max(1, (int) Math.ceil(threshold * settings.nightThresholdMultiplier()));
    }

    private static boolean isNight(World world) {
        long time = world.getTime();
        return time >= 13000L && time <= 23000L;
    }

    private static double distanceSq(Cow cow, Player player) {
        double dx = cow.getX() - player.getX();
        double dy = cow.getY() - player.getY();
        double dz = cow.getZ() - player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    interface NaturalCowResolver {
        boolean isNaturalCow(Cow cow);
    }

    private static final class TerritorialPickupCounter {
        private int leatherAmount;
        private long leatherExpiresTick;
        private int rawBeefAmount;
        private long rawBeefExpiresTick;
        private int boneAmount;
        private long boneExpiresTick;

        private int add(Material material, int amount, long currentTick, int windowTicks) {
            return switch (material) {
                case LEATHER -> {
                    if (currentTick > leatherExpiresTick) {
                        leatherAmount = 0;
                    }
                    leatherAmount = boundedAdd(leatherAmount, amount);
                    leatherExpiresTick = currentTick + windowTicks;
                    yield leatherAmount;
                }
                case BEEF -> {
                    if (currentTick > rawBeefExpiresTick) {
                        rawBeefAmount = 0;
                    }
                    rawBeefAmount = boundedAdd(rawBeefAmount, amount);
                    rawBeefExpiresTick = currentTick + windowTicks;
                    yield rawBeefAmount;
                }
                case BONE -> {
                    if (currentTick > boneExpiresTick) {
                        boneAmount = 0;
                    }
                    boneAmount = boundedAdd(boneAmount, amount);
                    boneExpiresTick = currentTick + windowTicks;
                    yield boneAmount;
                }
                default -> 0;
            };
        }

        private void reset(Material material) {
            switch (material) {
                case LEATHER -> {
                    leatherAmount = 0;
                    leatherExpiresTick = 0L;
                }
                case BEEF -> {
                    rawBeefAmount = 0;
                    rawBeefExpiresTick = 0L;
                }
                case BONE -> {
                    boneAmount = 0;
                    boneExpiresTick = 0L;
                }
                default -> {
                }
            }
        }

        private boolean isExpired(long currentTick) {
            return currentTick > leatherExpiresTick
                    && currentTick > rawBeefExpiresTick
                    && currentTick > boneExpiresTick;
        }

        private static int boundedAdd(int current, int amount) {
            long result = (long) current + amount;
            return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
        }
    }
}

