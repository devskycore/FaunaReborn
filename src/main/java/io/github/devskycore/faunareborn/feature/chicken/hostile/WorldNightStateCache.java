package io.github.devskycore.faunareborn.feature.chicken.hostile;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class WorldNightStateCache implements Listener {

    private static final long REFRESH_INTERVAL_TICKS = 60L;
    private static final long NIGHT_START_TICK = 13000L;
    private static final long NIGHT_END_TICK = 23000L;

    private final FaunaRebornPlugin plugin;
    private final Map<UUID, Boolean> nightByWorldId = new HashMap<>();

    private ScheduledTask refreshTask;

    WorldNightStateCache(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
    }

    void start() {
        if (refreshTask != null) {
            return;
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        refreshAllWorlds();
        refreshTask = plugin.getServer()
                .getGlobalRegionScheduler()
                .runAtFixedRate(plugin, task -> refreshAllWorlds(), REFRESH_INTERVAL_TICKS, REFRESH_INTERVAL_TICKS);
    }

    void stop() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        HandlerList.unregisterAll(this);
        nightByWorldId.clear();
    }

    boolean isNight(World world) {
        if (world == null) {
            return false;
        }
        return nightByWorldId.computeIfAbsent(world.getUID(), ignored -> computeNight(world));
    }

    @EventHandler
    private void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        nightByWorldId.put(world.getUID(), computeNight(world));
    }

    @EventHandler
    private void onWorldUnload(WorldUnloadEvent event) {
        nightByWorldId.remove(event.getWorld().getUID());
    }

    private void refreshAllWorlds() {
        for (World world : plugin.getServer().getWorlds()) {
            nightByWorldId.put(world.getUID(), computeNight(world));
        }
    }

    private boolean computeNight(World world) {
        long time = world.getTime();
        return time >= NIGHT_START_TICK && time <= NIGHT_END_TICK;
    }
}
