package io.github.devskycore.faunareborn.system.lifecycle;

import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginLifecycleLogger {
    private PluginLifecycleLogger() {}

    public static void onEnable(final JavaPlugin plugin, final long startedAtNanos) {
        final ConsoleCommandSender console = plugin.getServer().getConsoleSender();
        console.sendMessage(Component.text("[Status] ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Plugin enabled in ", NamedTextColor.GRAY))
                .append(Component.text(elapsedMillis(startedAtNanos) + " ms", NamedTextColor.GREEN))
                .append(Component.text(".", NamedTextColor.GRAY)));
    }

    public static void onDisable(final JavaPlugin plugin, final long startedAtNanos) {
        final ConsoleCommandSender console = plugin.getServer().getConsoleSender();
        console.sendMessage(Component.text("[Status] ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Plugin disabled in ", NamedTextColor.GRAY))
                .append(Component.text(elapsedMillis(startedAtNanos) + " ms", NamedTextColor.RED))
                .append(Component.text(".", NamedTextColor.GRAY)));
    }

    private static long elapsedMillis(final long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }
}