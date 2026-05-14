package io.github.devskycore.faunareborn.animal.cow.hostility;

import io.github.devskycore.faunareborn.animal.cow.CowSettings;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.environment.EnvironmentAggressionSettings;
import io.github.devskycore.faunareborn.system.environment.WorldEnvironmentContextCache;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapter;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapters;
import io.github.devskycore.faunareborn.system.scheduler.TaskHandle;
import io.github.devskycore.faunareborn.system.lod.LodSettings;
import org.bukkit.event.HandlerList;

public final class CowMilkProvocationTask {

    private static final long TICK_RATE = 1L;

    private final FaunaRebornPlugin plugin;
    private final SchedulerAdapter scheduler;
    private final CowMilkAggressionController aggressionController;
    private final CowMilkInteractionListener interactionListener;
    private final WorldEnvironmentContextCache environmentCache;

    private TaskHandle task;

    public CowMilkProvocationTask(
            FaunaRebornPlugin plugin,
            CowSettings.MilkProvocationSettings settings,
            CowSettings.ResourceProvocationSettings resourceSettings,
            CowSettings.SocialAlertSettings socialAlertSettings,
            CowSettings.GlobalHostilitySettings globalSettings,
            EnvironmentAggressionSettings environmentSettings,
            LodSettings lodSettings
    ) {
        this.plugin = plugin;
        this.scheduler = SchedulerAdapters.create(plugin);
        this.environmentCache = new WorldEnvironmentContextCache(plugin, environmentSettings);
        this.aggressionController = new CowMilkAggressionController(scheduler, settings, globalSettings, lodSettings, environmentCache);
        this.interactionListener = new CowMilkInteractionListener(
                plugin,
                settings,
                socialAlertSettings,
                globalSettings,
                aggressionController,
                resourceSettings,
                environmentCache
        );
    }

    public void start() {
        if (task != null) {
            return;
        }
        environmentCache.start();
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
        environmentCache.stop();
    }
}
