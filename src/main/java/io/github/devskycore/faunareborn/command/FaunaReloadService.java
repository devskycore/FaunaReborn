package io.github.devskycore.faunareborn.command;

import io.github.devskycore.faunareborn.config.entity.EntityType;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.shutdown.ShutdownOrchestrator;
import io.github.devskycore.faunareborn.system.startup.StartupOrchestrator;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class FaunaReloadService {

    private final FaunaRebornPlugin plugin;

    public FaunaReloadService(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload(CommandSender sender) {
        long startedAt = System.nanoTime();
        Map<Path, String> previousConfigs = readConfigSnapshots();

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

    private Map<Path, String> readConfigSnapshots() {
        Map<Path, String> snapshots = new HashMap<>();
        Path dataFolder = plugin.getDataFolder().toPath();
        readSnapshotIfExists(dataFolder.resolve("config.yml"), snapshots);
        for (EntityType entityType : EntityType.values()) {
            readSnapshotIfExists(dataFolder.resolve(entityType.resourcePath()), snapshots);
        }
        return snapshots;
    }

    private void readSnapshotIfExists(Path configPath, Map<Path, String> snapshots) {
        if (!Files.exists(configPath)) {
            return;
        }
        try {
            snapshots.put(configPath, Files.readString(configPath, StandardCharsets.UTF_8));
        } catch (IOException ioException) {
            plugin.getLogger().log(Level.WARNING, "Could not read config snapshot before reload: " + configPath, ioException);
        }
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
