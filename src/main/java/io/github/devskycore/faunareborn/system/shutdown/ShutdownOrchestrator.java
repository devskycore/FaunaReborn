package io.github.devskycore.faunareborn.system.shutdown;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;

public final class ShutdownOrchestrator {
    private static final TextColor PREMIUM_ORANGE = TextColor.color(0xFF8C42);

    private final FaunaRebornPlugin plugin;
    private final List<Runnable> shutdownSteps = new ArrayList<>();

    public ShutdownOrchestrator(final FaunaRebornPlugin plugin) {
        this.plugin = plugin;
        registerDefaultSteps();
    }

    public void run() {
        for (Runnable step : shutdownSteps) {
            step.run();
        }
    }

    private void registerDefaultSteps() {
        shutdownSteps.add(() -> plugin.getServer().getConsoleSender().sendMessage(
                Component.text("[Shutdown] ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Stopping plugin services...", PREMIUM_ORANGE))
        ));
        shutdownSteps.add(() -> plugin.getServer().getConsoleSender().sendMessage(
                Component.text("[Shutdown] ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Finalizing shutdown sequence.", NamedTextColor.RED))
        ));
    }
}
