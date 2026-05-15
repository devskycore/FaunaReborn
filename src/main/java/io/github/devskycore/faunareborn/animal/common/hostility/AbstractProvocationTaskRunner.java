package io.github.devskycore.faunareborn.animal.common.hostility;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.environment.WorldEnvironmentContextCache;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapter;
import io.github.devskycore.faunareborn.system.scheduler.TaskHandle;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public abstract class AbstractProvocationTaskRunner {

    private static final long TICK_RATE = 1L;

    private final FaunaRebornPlugin plugin;
    private final SchedulerAdapter scheduler;
    private final Listener interactionListener;
    private final WorldEnvironmentContextCache environmentCache;
    private final Runnable tickAction;
    private final Runnable clearAllAction;
    private final Runnable clearListenerStateAction;

    private TaskHandle task;

    protected AbstractProvocationTaskRunner(
            FaunaRebornPlugin plugin,
            SchedulerAdapter scheduler,
            Listener interactionListener,
            WorldEnvironmentContextCache environmentCache,
            Runnable tickAction,
            Runnable clearAllAction,
            Runnable clearListenerStateAction
    ) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.interactionListener = interactionListener;
        this.environmentCache = environmentCache;
        this.tickAction = tickAction;
        this.clearAllAction = clearAllAction;
        this.clearListenerStateAction = clearListenerStateAction;
    }

    public final void start() {
        if (task != null) {
            return;
        }
        environmentCache.start();
        plugin.getServer().getPluginManager().registerEvents(interactionListener, plugin);
        task = scheduler.runAtFixedRate(tickAction, 1L, TICK_RATE);
    }

    public final void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        HandlerList.unregisterAll(interactionListener);
        clearListenerStateAction.run();
        clearAllAction.run();
        environmentCache.stop();
    }
}
