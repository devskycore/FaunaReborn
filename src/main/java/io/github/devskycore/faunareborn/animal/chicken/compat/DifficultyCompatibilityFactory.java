package io.github.devskycore.faunareborn.animal.chicken.compat;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.World;

import java.util.function.Consumer;

public final class DifficultyCompatibilityFactory {

    private static final String PAPER_DIFFICULTY_EVENT = "io.papermc.paper.event.world.WorldDifficultyChangeEvent";

    private DifficultyCompatibilityFactory() {
    }

    public static DifficultyCompatibilityHook create(FaunaRebornPlugin plugin, Consumer<World> onWorldPeaceful) {
        try {
            Class.forName(PAPER_DIFFICULTY_EVENT);
            return new PaperDifficultyCompatibilityHook(plugin, onWorldPeaceful);
        } catch (ClassNotFoundException ignored) {
            plugin.getLogger().warning("WorldDifficultyChangeEvent not available; using polling compatibility hook.");
            return new PollingDifficultyCompatibilityHook(plugin, onWorldPeaceful);
        }
    }
}
