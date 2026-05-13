package io.github.devskycore.faunareborn.targeting;

import io.github.devskycore.faunareborn.config.common.TargetingSettings;
import io.github.devskycore.faunareborn.config.common.WorldFilter;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public final class TargetEligibilityService {

    private static final long CACHE_TTL_TICKS = 10L;

    private final TargetingSettings settings;
    private final PlayerProtectionService protectionService;
    private final PlayerVisibilityService visibilityService = new PlayerVisibilityService();
    private final Map<UUID, CachedEligibility> cache = new Object2ObjectOpenHashMap<>();

    public TargetEligibilityService(TargetingSettings settings) {
        this(settings, new MetadataProtectionService());
    }

    public TargetEligibilityService(TargetingSettings settings, PlayerProtectionService protectionService) {
        this.settings = settings;
        this.protectionService = protectionService;
    }

    public void clearExpired(long currentTick) {
        if (cache.isEmpty()) {
            return;
        }
        cache.entrySet().removeIf(entry -> currentTick >= entry.getValue().expiresAtTick);
    }

    public boolean isEligible(LivingEntity source, Player player, WorldFilter worldFilter, long currentTick) {
        if (source == null || player == null) {
            return false;
        }
        World world = source.getWorld();
        if (worldFilter != null && worldFilter.isWorldDisallowed(world.getName())) {
            return false;
        }
        if (player.getWorld() != world) {
            return false;
        }
        if (!evaluateBaseEligibility(player, currentTick)) {
            return false;
        }
        if (source instanceof Player sourcePlayer && !sourcePlayer.canSee(player)) {
            return false;
        }
        return true;
    }

    public boolean isEligible(Player player, WorldFilter worldFilter, long currentTick) {
        if (player == null) {
            return false;
        }
        if (worldFilter != null && worldFilter.isWorldDisallowed(player.getWorld().getName())) {
            return false;
        }
        return evaluateBaseEligibility(player, currentTick);
    }

    private boolean evaluateBaseEligibility(Player player, long currentTick) {
        UUID playerId = player.getUniqueId();
        CachedEligibility cached = cache.get(playerId);
        if (cached != null && currentTick >= 0L && currentTick < cached.expiresAtTick) {
            return cached.eligible;
        }

        boolean eligible = true;
        if (!player.isOnline() || !player.isValid() || player.isDead()) {
            eligible = false;
        }

        if (eligible) {
            GameMode mode = player.getGameMode();
            if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR || mode == GameMode.ADVENTURE) {
                eligible = false;
            }
        }

        if (eligible && visibilityService.isHidden(
                player,
                settings.ignore().invisiblePotion(),
                settings.ignore().vanished()
        )) {
            eligible = false;
        }

        if (eligible && settings.ignore().godMode() && protectionService.isProtected(player)) {
            eligible = false;
        }

        long expiresAtTick = currentTick >= 0L ? currentTick + CACHE_TTL_TICKS : Long.MIN_VALUE;
        cache.put(playerId, new CachedEligibility(eligible, expiresAtTick));
        return eligible;
    }

    private record CachedEligibility(boolean eligible, long expiresAtTick) {
    }
}
