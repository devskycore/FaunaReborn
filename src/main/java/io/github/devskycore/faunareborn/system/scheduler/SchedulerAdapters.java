package io.github.devskycore.faunareborn.system.scheduler;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.platform.RuntimePlatform;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

public final class SchedulerAdapters {

    private SchedulerAdapters() {
    }

    public static SchedulerAdapter create(FaunaRebornPlugin plugin) {
        return RuntimePlatform.isFolia()
                ? new FoliaSchedulerAdapter(plugin)
                : new BukkitSchedulerAdapter(plugin);
    }

    private record BukkitSchedulerAdapter(FaunaRebornPlugin plugin) implements SchedulerAdapter {
        @Override
        public TaskHandle runLater(Runnable task, long delayTicks) {
            BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
            return bukkitTask::cancel;
        }

        @Override
        public TaskHandle runAtFixedRate(Runnable task, long delayTicks, long periodTicks) {
            BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
            return bukkitTask::cancel;
        }

        @Override
        public void runNextTick(Runnable task) {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }

        @Override
        public void runForEntity(Entity entity, Runnable task) {
            runNextTick(task);
        }

        @Override
        public void runForLocation(Location location, Runnable task) {
            runNextTick(task);
        }
    }

    private record FoliaSchedulerAdapter(FaunaRebornPlugin plugin) implements SchedulerAdapter {
        @Override
        public TaskHandle runLater(Runnable task, long delayTicks) {
            ScheduledTask scheduledTask = plugin.getServer().getGlobalRegionScheduler()
                    .runDelayed(plugin, t -> task.run(), delayTicks);
            return scheduledTask::cancel;
        }

        @Override
        public TaskHandle runAtFixedRate(Runnable task, long delayTicks, long periodTicks) {
            ScheduledTask scheduledTask = plugin.getServer().getGlobalRegionScheduler()
                    .runAtFixedRate(plugin, t -> task.run(), delayTicks, periodTicks);
            return scheduledTask::cancel;
        }

        @Override
        public void runNextTick(Runnable task) {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, t -> task.run());
        }

        @Override
        public void runForEntity(Entity entity, Runnable task) {
            if (entity == null || !entity.isValid()) {
                return;
            }
            entity.getScheduler().run(plugin, t -> task.run(), () -> {
            });
        }

        @Override
        public void runForLocation(Location location, Runnable task) {
            if (location == null || location.getWorld() == null) {
                return;
            }
            plugin.getServer().getRegionScheduler().execute(plugin, location, task);
        }
    }
}
