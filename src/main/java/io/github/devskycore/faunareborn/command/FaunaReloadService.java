package io.github.devskycore.faunareborn.command;

import io.github.devskycore.faunareborn.combat.deathmessage.HostilityDeathMessageListener;
import io.github.devskycore.faunareborn.config.PluginConfigManager;
import io.github.devskycore.faunareborn.config.PluginSettings;
import io.github.devskycore.faunareborn.config.entity.EntityType;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.module.FaunaFeatureRegistry;
import io.github.devskycore.faunareborn.module.ModuleManager;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapter;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapters;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class FaunaReloadService {

    private final FaunaRebornPlugin plugin;
    private final AtomicBoolean inProgress = new AtomicBoolean(false);
    private final ExecutorService reloadExecutor;
    private final SchedulerAdapter scheduler;

    public FaunaReloadService(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "FaunaReborn-Reload");
            thread.setDaemon(true);
            return thread;
        };
        this.reloadExecutor = Executors.newSingleThreadExecutor(threadFactory);
        this.scheduler = SchedulerAdapters.create(plugin);
    }

    public void reload(CommandSender sender) {
        reload(sender, null);
    }

    public void reload(CommandSender sender, Runnable completion) {
        if (!inProgress.compareAndSet(false, true)) {
            sender.sendMessage(plugin.languageManager().text(
                    "commands.reload.already-in-progress",
                    "FaunaReborn reload is already in progress."
            ));
            if (completion != null) {
                completion.run();
            }
            return;
        }

        final long startedAt = System.nanoTime();
        sender.sendMessage(plugin.languageManager().text(
                "commands.reload.started",
                "FaunaReborn reload started..."
        ));

        ensureConfigFilesExist();

        CompletableFuture
                .supplyAsync(this::loadCandidateSettings, reloadExecutor)
                .whenComplete((candidate, throwable) -> scheduleOnMainThread(() -> {
                    try {
                        if (throwable != null) {
                            logReloadFailure("Configuration validation failed.", throwable);
                            sender.sendMessage(plugin.languageManager().text(
                                    "commands.reload.failed",
                                    "FaunaReborn reload failed. Previous configuration is still active."
                            ));
                            return;
                        }

                        boolean applied = applyCandidate(candidate);
                        long elapsedMillis = elapsedMillis(startedAt);
                        if (applied) {
                            plugin.languageManager().reload();
                            sender.sendMessage(plugin.languageManager().text(
                                    "commands.reload.completed",
                                    "FaunaReborn reload completed in {elapsedMillis} ms.",
                                    java.util.Map.of("elapsedMillis", String.valueOf(elapsedMillis))
                            ));
                            plugin.getLogger().info(plugin.languageManager().text(
                                    "logs.reload.hot-reload-complete",
                                    "Hot reload completed in {elapsedMillis} ms.",
                                    java.util.Map.of("elapsedMillis", String.valueOf(elapsedMillis))
                            ));
                        } else {
                            sender.sendMessage(plugin.languageManager().text(
                                    "commands.reload.failed",
                                    "FaunaReborn reload failed. Previous configuration is still active."
                            ));
                        }
                    } finally {
                        inProgress.set(false);
                        if (completion != null) {
                            completion.run();
                        }
                    }
                }));
    }

    public boolean isInProgress() {
        return inProgress.get();
    }

    private PluginSettings loadCandidateSettings() {
        try {
            FileConfiguration globalConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
            FaunaFeatureRegistry featureRegistry = FaunaFeatureRegistry.defaults();
            PluginConfigManager configManager = new PluginConfigManager(plugin, featureRegistry.createSettingsLoaders(plugin));
            return configManager.load(globalConfig);
        } catch (Throwable throwable) {
            throw new IllegalStateException(plugin.languageManager().text(
                    "logs.reload.failed-to-parse-yaml",
                    "Failed to parse one or more YAML configuration files."
            ), throwable);
        }
    }

    private boolean applyCandidate(PluginSettings candidate) {
        FaunaFeatureRegistry featureRegistry = FaunaFeatureRegistry.defaults();
        ModuleManager oldManager = plugin.moduleManager();
        HostilityDeathMessageListener oldListener = plugin.deathMessageListener();

        ModuleManager newManager = new ModuleManager(plugin, featureRegistry.createModules(plugin, candidate));
        HostilityDeathMessageListener newListener = new HostilityDeathMessageListener(plugin.languageManager());

        try {
            if (oldManager != null) {
                oldManager.disableAll();
            }
            if (oldListener != null) {
                HandlerList.unregisterAll(oldListener);
            }

            newManager.enableAll();
            plugin.getServer().getPluginManager().registerEvents(newListener, plugin);
            plugin.setModuleManager(newManager);
            plugin.setDeathMessageListener(newListener);
            return true;
        } catch (Throwable applyFailure) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    plugin.languageManager().text(
                            "logs.reload.apply-failed",
                            "Failed while applying reloaded runtime. Rolling back previous runtime."
                    ),
                    applyFailure
            );
            rollbackRuntime(oldManager, oldListener);
            return false;
        }
    }

    private void rollbackRuntime(ModuleManager oldManager, HostilityDeathMessageListener oldListener) {
        try {
            if (oldManager != null) {
                oldManager.enableAll();
                plugin.setModuleManager(oldManager);
            }
            if (oldListener != null) {
                plugin.getServer().getPluginManager().registerEvents(oldListener, plugin);
                plugin.setDeathMessageListener(oldListener);
            }
        } catch (Throwable rollbackFailure) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    plugin.languageManager().text(
                            "logs.reload.rollback-failed",
                            "Rollback failed. Manual intervention may be required."
                    ),
                    rollbackFailure
            );
        }
    }

    private void ensureConfigFilesExist() {
        plugin.configMigrationService().migrateIfNeeded();
        plugin.saveDefaultConfig();
        File entitiesDirectory = new File(plugin.getDataFolder(), "entities");
        if (!entitiesDirectory.exists() && !entitiesDirectory.mkdirs() && !entitiesDirectory.exists()) {
            plugin.getLogger().warning(plugin.languageManager().text(
                    "logs.reload.entities-dir-create-failed",
                    "Failed to create entities configuration directory: {path}",
                    java.util.Map.of("path", entitiesDirectory.getAbsolutePath())
            ));
        }
        for (EntityType entityType : EntityType.values()) {
            String resourcePath = entityType.resourcePath();
            File file = new File(plugin.getDataFolder(), resourcePath);
            if (!file.exists()) {
                plugin.saveResource(resourcePath, false);
            }
        }
    }

    private void scheduleOnMainThread(Runnable action) {
        try {
            scheduler.runNextTick(action);
        } catch (Throwable throwable) {
            inProgress.set(false);
            logReloadFailure("Failed to schedule reload apply task on server thread.", throwable);
        }
    }

    private void logReloadFailure(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, plugin.languageManager().text("logs.reload.validation-failed", message), throwable);
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
