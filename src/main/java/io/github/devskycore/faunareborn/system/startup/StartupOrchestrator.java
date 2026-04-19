package io.github.devskycore.faunareborn.system.startup;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public final class StartupOrchestrator {

    private final FaunaRebornPlugin plugin;
    private final List<Runnable> startupSteps = new ArrayList<>();

    public StartupOrchestrator(final FaunaRebornPlugin plugin) {
        this.plugin = plugin;
        registerDefaultSteps();
    }

    public void run() {
        for (Runnable step : startupSteps) {
            step.run();
        }
    }

    private void registerDefaultSteps() {
        startupSteps.add(() -> plugin.getServer().getConsoleSender().sendMessage(
                Component.text("[Startup] ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Boot sequence initialized.", NamedTextColor.AQUA))
        ));
        startupSteps.add(() -> plugin.getServer().getConsoleSender().sendMessage(
                Component.text("[Startup] ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Server version: ", NamedTextColor.GREEN))
                        .append(Component.text(Bukkit.getVersion(), NamedTextColor.YELLOW))
        ));
    }
}
