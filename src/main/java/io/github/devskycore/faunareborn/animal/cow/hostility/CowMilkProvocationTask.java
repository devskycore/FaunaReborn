package io.github.devskycore.faunareborn.animal.cow.hostility;

import io.github.devskycore.faunareborn.animal.cow.CowSettings;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;

public final class CowMilkProvocationTask {

    private static final long TICK_RATE = 1L;

    private final FaunaRebornPlugin plugin;
    private final CowMilkAggressionController aggressionController;
    private final CowMilkInteractionListener interactionListener;

    private BukkitTask task;

    public CowMilkProvocationTask(FaunaRebornPlugin plugin, CowSettings.MilkProvocationSettings settings, CowSettings.GlobalHostilitySettings globalSettings) {
        this.plugin = plugin;
        this.aggressionController = new CowMilkAggressionController(settings, globalSettings);
        this.interactionListener = new CowMilkInteractionListener(plugin, settings, globalSettings, aggressionController);
    }

    public void start() {
        if (task != null) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(interactionListener, plugin);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, aggressionController::tick, 1L, TICK_RATE);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        HandlerList.unregisterAll(interactionListener);
        aggressionController.clearAll();
    }
}
