package io.github.devskycore.faunareborn.system.scheduler;

import org.bukkit.entity.Entity;

public interface SchedulerAdapter {

    void runLater(Runnable task, long delayTicks);

    TaskHandle runAtFixedRate(Runnable task, long delayTicks, long periodTicks);

    void runNextTick(Runnable task);

    void runForEntity(Entity entity, Runnable task);
}
