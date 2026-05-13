package io.github.devskycore.faunareborn.system.environment;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapter;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapters;
import io.github.devskycore.faunareborn.system.scheduler.TaskHandle;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WorldEnvironmentContextCache implements Listener {

    private static final long DAY_TICKS = 24000L;

    private final FaunaRebornPlugin plugin;
    private final SchedulerAdapter scheduler;
    private final EnvironmentAggressionSettings settings;
    private final Map<UUID, WorldEnvironmentContext> contextByWorldId = new ConcurrentHashMap<>();
    private TaskHandle refreshTask;

    public WorldEnvironmentContextCache(FaunaRebornPlugin plugin, EnvironmentAggressionSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.scheduler = SchedulerAdapters.create(plugin);
    }

    public void start() {
        if (refreshTask != null) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        refreshAllWorlds();
        refreshTask = scheduler.runAtFixedRate(this::refreshAllWorlds, settings.refreshIntervalTicks(), settings.refreshIntervalTicks());
    }

    public void stop() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        HandlerList.unregisterAll(this);
        contextByWorldId.clear();
    }

    public WorldEnvironmentContext context(World world) {
        if (world == null) {
            return defaultContext();
        }
        return contextByWorldId.computeIfAbsent(world.getUID(), ignored -> computeContext(world));
    }

    @EventHandler
    private void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        contextByWorldId.put(world.getUID(), computeContext(world));
    }

    @EventHandler
    private void onWorldUnload(WorldUnloadEvent event) {
        contextByWorldId.remove(event.getWorld().getUID());
    }

    private void refreshAllWorlds() {
        for (World world : plugin.getServer().getWorlds()) {
            contextByWorldId.put(world.getUID(), computeContext(world));
        }
    }

    private WorldEnvironmentContext computeContext(World world) {
        boolean night = isNight(world);
        boolean raining = world.hasStorm();
        boolean thundering = world.isThundering();
        MoonPhase moonPhase = resolveMoonPhase(world);
        boolean fullMoon = moonPhase == MoonPhase.FULL_MOON;

        WorldEnvironmentContext base = new WorldEnvironmentContext(
                night,
                raining,
                thundering,
                moonPhase,
                fullMoon,
                EnvironmentAggressionModifiers.identity()
        );
        return new WorldEnvironmentContext(night, raining, thundering, moonPhase, fullMoon, settings.resolve(base));
    }

    private static boolean isNight(World world) {
        long time = world.getTime();
        return time >= 13000L && time <= 23000L;
    }

    private static MoonPhase resolveMoonPhase(World world) {
        long day = Math.floorDiv(world.getFullTime(), DAY_TICKS);
        return MoonPhase.fromIndex((int) day);
    }

    private static WorldEnvironmentContext defaultContext() {
        return new WorldEnvironmentContext(false, false, false, MoonPhase.FULL_MOON, true, EnvironmentAggressionModifiers.identity());
    }
}
