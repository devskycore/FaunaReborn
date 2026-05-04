package io.github.devskycore.faunareborn.system.lifecycle;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class PluginBanner {

    private static final TextColor PRIMARY = NamedTextColor.AQUA;
    private static final TextColor SECONDARY = NamedTextColor.GRAY;
    private static final String SEPARATOR = "==============================";

    private PluginBanner() {}

    public static void printEnable(JavaPlugin plugin) {
        final String version = plugin.getPluginMeta().getVersion();
        final String mcVersion = plugin.getServer().getMinecraftVersion();

        List<Component> lines = new ArrayList<>();

        lines.add(center(SEPARATOR, SECONDARY));
        lines.add(center("FAUNAREBORN", PRIMARY));
        lines.add(center("Version > " + version, SECONDARY));
        lines.add(center("MC > " + mcVersion, SECONDARY));
        lines.add(center(SEPARATOR, SECONDARY));

        send(lines, plugin);
    }

    public static void printDisable(JavaPlugin plugin) {
        final String version = plugin.getPluginMeta().getVersion();

        List<Component> lines = new ArrayList<>();

        lines.add(center(SEPARATOR, SECONDARY));
        lines.add(center("FAUNAREBORN", PRIMARY));
        lines.add(center("Version > " + version, SECONDARY));
        lines.add(center(SEPARATOR, SECONDARY));

        send(lines, plugin);
    }

    private static void send(List<Component> lines, JavaPlugin plugin) {
        var console = plugin.getServer().getConsoleSender();
        for (Component line : lines) {
            console.sendMessage(line);
        }
    }

    private static Component center(String text, TextColor color) {
        int width = 46;
        int padding = Math.max(0, (width - text.length()) / 2);

        return Component.text(" ".repeat(padding) + text, color);
    }
}

