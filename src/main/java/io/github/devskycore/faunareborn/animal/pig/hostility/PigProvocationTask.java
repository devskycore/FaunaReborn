package io.github.devskycore.faunareborn.animal.pig.hostility;

import io.github.devskycore.faunareborn.animal.pig.PigSettings;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapter;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapters;
import io.github.devskycore.faunareborn.system.scheduler.TaskHandle;
import org.bukkit.event.HandlerList;

public final class PigProvocationTask {

    private static final long TICK_RATE = 1L;

    private final FaunaRebornPlugin plugin;
    private final SchedulerAdapter scheduler;
    private final PigAggressionController aggressionController;
    private final PigInteractionListener interactionListener;

    private TaskHandle task;

    public PigProvocationTask(
            FaunaRebornPlugin plugin,
            PigSettings.RodProvocationSettings settings,
            PigSettings.ResourceProvocationSettings resourceSettings,
            PigSettings.SocialAlertSettings socialAlertSettings,
            PigSettings.GlobalHostilitySettings globalSettings
    ) {
        this.plugin = plugin;
        this.scheduler = SchedulerAdapters.create(plugin);
        this.aggressionController = new PigAggressionController(scheduler, settings, globalSettings);
        this.interactionListener = new PigInteractionListener(
                plugin,
                settings,
                socialAlertSettings,
                globalSettings,
                aggressionController,
                resourceSettings
        );
    }

    public void start() {
        if (task != null) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(interactionListener, plugin);
        task = scheduler.runAtFixedRate(aggressionController::tick, 1L, TICK_RATE);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        HandlerList.unregisterAll(interactionListener);
        interactionListener.clearState();
        aggressionController.clearAll();
    }
}


