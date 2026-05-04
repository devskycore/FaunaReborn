package io.github.devskycore.faunareborn.command;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.shutdown.ShutdownOrchestrator;
import io.github.devskycore.faunareborn.system.startup.StartupOrchestrator;
import org.bukkit.command.CommandSender;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class FaunaCommand implements BasicCommand {

    private static final String SUBCOMMAND_RELOAD = "reload";
    private static final String RELOAD_PERMISSION = "faunareborn.command.reload";

    private final FaunaRebornPlugin plugin;

    public FaunaCommand(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NonNull CommandSourceStack source, String[] args) {
        final CommandSender sender = source.getSender();

        if (args.length != 1 || !SUBCOMMAND_RELOAD.equalsIgnoreCase(args[0])) {
            sender.sendMessage("Usage: /fauna reload");
            return;
        }

        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            sender.sendMessage("You do not have permission to use this command.");
            return;
        }

        long startedAt = System.nanoTime();
        Map<Path, String> previousConfigs = readConfigSnapshots(
                plugin.getDataFolder().toPath().resolve("config.yml"),
                plugin.getDataFolder().toPath().resolve("entities/chicken.yml"),
                plugin.getDataFolder().toPath().resolve("entities/cow.yml")
        );

        try {
            new ShutdownOrchestrator(plugin).run();
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.SEVERE, "Failed to stop current runtime before reload.", throwable);
            sender.sendMessage("FaunaReborn reload failed. Check console for details.");
            return;
        }

        if (new StartupOrchestrator(plugin, false).run()) {
            sender.sendMessage("FaunaReborn reloaded successfully in " + elapsedMillis(startedAt) + " ms.");
            return;
        }

        plugin.getLogger().severe("Reload startup failed. Trying to restore previous config snapshot.");

        writeConfigSnapshots(previousConfigs);

        boolean restored = new StartupOrchestrator(plugin, false).run();
        if (!restored) {
            plugin.getLogger().severe("Failed to restore previous runtime state after reload failure.");
        }

        sender.sendMessage("FaunaReborn reload failed. Check console for details.");
    }

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack source, String[] args) {
        final CommandSender sender = source.getSender();
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

    @Override
    public @NonNull String permission() {
        return RELOAD_PERMISSION;
    }

    private Map<Path, String> readConfigSnapshots(Path... configPaths) {
        Map<Path, String> snapshots = new HashMap<>();
        for (Path configPath : configPaths) {
            if (!Files.exists(configPath)) {
                continue;
            }
            try {
                snapshots.put(configPath, Files.readString(configPath, StandardCharsets.UTF_8));
            } catch (IOException ioException) {
                plugin.getLogger().log(Level.WARNING, "Could not read config snapshot before reload: " + configPath, ioException);
            }
        }
        return snapshots;
    }

    private void writeConfigSnapshots(Map<Path, String> snapshots) {
        for (Map.Entry<Path, String> entry : snapshots.entrySet()) {
            try {
                Files.createDirectories(entry.getKey().getParent());
                Files.writeString(entry.getKey(), entry.getValue(), StandardCharsets.UTF_8);
            } catch (IOException ioException) {
                plugin.getLogger().log(Level.SEVERE, "Could not restore previous config snapshot: " + entry.getKey(), ioException);
            }
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}

