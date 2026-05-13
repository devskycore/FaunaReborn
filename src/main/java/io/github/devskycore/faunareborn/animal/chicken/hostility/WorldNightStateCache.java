package io.github.devskycore.faunareborn.animal.chicken.hostility;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.environment.EnvironmentAggressionSettings;
import io.github.devskycore.faunareborn.system.environment.WorldEnvironmentContext;
import io.github.devskycore.faunareborn.system.environment.WorldEnvironmentContextCache;
import org.bukkit.World;

final class WorldNightStateCache {

    private final WorldEnvironmentContextCache delegate;

    WorldNightStateCache(FaunaRebornPlugin plugin, EnvironmentAggressionSettings settings) {
        this.delegate = new WorldEnvironmentContextCache(plugin, settings);
    }

    void start() {
        delegate.start();
    }

    void stop() {
        delegate.stop();
    }

    boolean isNight(World world) {
        return delegate.context(world).night();
    }

    WorldEnvironmentContext context(World world) {
        return delegate.context(world);
    }
}
