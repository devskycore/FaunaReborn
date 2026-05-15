package io.github.devskycore.faunareborn.animal.chicken.compat;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.papermc.paper.event.world.WorldDifficultyChangeEvent;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.Objects;
import java.util.function.Consumer;

final class PaperDifficultyCompatibilityHook implements DifficultyCompatibilityHook, Listener {

    private final FaunaRebornPlugin plugin;
    private final Consumer<World> onWorldPeaceful;
    private boolean started;

    PaperDifficultyCompatibilityHook(FaunaRebornPlugin plugin, Consumer<World> onWorldPeaceful) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.onWorldPeaceful = Objects.requireNonNull(onWorldPeaceful, "onWorldPeaceful");
    }

    @Override
    public void start() {
        if (started) {
            return;
        }
        started = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void stop() {
        if (!started) {
            return;
        }
        started = false;
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onWorldDifficultyChange(WorldDifficultyChangeEvent event) {
        if (event.getDifficulty() == Difficulty.PEACEFUL) {
            onWorldPeaceful.accept(event.getWorld());
        }
    }
}
