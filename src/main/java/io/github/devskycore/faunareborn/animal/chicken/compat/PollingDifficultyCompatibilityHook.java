package io.github.devskycore.faunareborn.animal.chicken.compat;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

final class PollingDifficultyCompatibilityHook implements DifficultyCompatibilityHook {

    private static final long POLL_INTERVAL_TICKS = 20L;

    private final FaunaRebornPlugin plugin;
    private final Consumer<World> onWorldPeaceful;
    private final Map<UUID, Difficulty> knownDifficultyByWorld = new HashMap<>();
    private BukkitTask task;

    PollingDifficultyCompatibilityHook(FaunaRebornPlugin plugin, Consumer<World> onWorldPeaceful) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.onWorldPeaceful = Objects.requireNonNull(onWorldPeaceful, "onWorldPeaceful");
    }

    @Override
    public void start() {
        if (task != null) {
            return;
        }
        bootstrapSnapshot();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::poll, POLL_INTERVAL_TICKS, POLL_INTERVAL_TICKS);
    }

    @Override
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        knownDifficultyByWorld.clear();
    }

    private void bootstrapSnapshot() {
        knownDifficultyByWorld.clear();
        for (World world : plugin.getServer().getWorlds()) {
            knownDifficultyByWorld.put(world.getUID(), world.getDifficulty());
        }
    }

    private void poll() {
        Map<UUID, Difficulty> next = new HashMap<>();
        for (World world : plugin.getServer().getWorlds()) {
            Difficulty current = world.getDifficulty();
            Difficulty previous = knownDifficultyByWorld.get(world.getUID());
            if (current == Difficulty.PEACEFUL && previous != Difficulty.PEACEFUL) {
                onWorldPeaceful.accept(world);
            }
            next.put(world.getUID(), current);
        }
        knownDifficultyByWorld.clear();
        knownDifficultyByWorld.putAll(next);
    }
}
