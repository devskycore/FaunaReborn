package io.github.devskycore.faunareborn.command;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.shutdown.ShutdownOrchestrator;
import io.github.devskycore.faunareborn.system.startup.StartupOrchestrator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class FaunaCommand implements CommandExecutor, TabCompleter {

    private static final String SUBCOMMAND_RELOAD = "reload";
    private static final String RELOAD_PERMISSION = "faunareborn.command.reload";

    private final FaunaRebornPlugin plugin;

    public FaunaCommand(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String[] args) {
        if (args.length != 1 || !SUBCOMMAND_RELOAD.equalsIgnoreCase(args[0])) {
            sender.sendMessage("Usage: /" + label + " reload");
            return true;
        }

        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        long startedAt = System.nanoTime();
        Path configPath = plugin.getDataFolder().toPath().resolve("config.yml");
        String previousConfig = readConfigSnapshot(configPath);

        try {
            new ShutdownOrchestrator(plugin).run();
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.SEVERE, "Failed to stop current runtime before reload.", throwable);
            sender.sendMessage("FaunaReborn reload failed. Check console for details.");
            return true;
        }

        if (new StartupOrchestrator(plugin, false).run()) {
            sender.sendMessage("FaunaReborn reloaded successfully in " + elapsedMillis(startedAt) + " ms.");
            return true;
        }

        plugin.getLogger().severe("Reload startup failed. Trying to restore previous config snapshot.");

        if (previousConfig != null) {
            writeConfigSnapshot(configPath, previousConfig);
        }

        boolean restored = new StartupOrchestrator(plugin, false).run();
        if (!restored) {
            plugin.getLogger().severe("Failed to restore previous runtime state after reload failure.");
        }

        sender.sendMessage("FaunaReborn reload failed. Check console for details.");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            return Collections.emptyList();
        }
        String token = args[0].toLowerCase(Locale.ROOT);
        if (SUBCOMMAND_RELOAD.startsWith(token)) {
            return List.of(SUBCOMMAND_RELOAD);
        }
        return Collections.emptyList();
    }

    private String readConfigSnapshot(Path configPath) {
        if (!Files.exists(configPath)) {
            return null;
        }
        try {
            return Files.readString(configPath, StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            plugin.getLogger().log(Level.WARNING, "Could not read config snapshot before reload.", ioException);
            return null;
        }
    }

    private void writeConfigSnapshot(Path configPath, String content) {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, content, StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            plugin.getLogger().log(Level.SEVERE, "Could not restore previous config snapshot.", ioException);
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
