package io.github.devskycore.faunareborn.command;

import io.github.devskycore.faunareborn.command.message.CommandMessages;
import io.github.devskycore.faunareborn.command.permission.PermissionService;
import io.github.devskycore.faunareborn.command.subcommand.AboutCommand;
import io.github.devskycore.faunareborn.command.subcommand.CommandRegistry;
import io.github.devskycore.faunareborn.command.subcommand.EntitiesCommand;
import io.github.devskycore.faunareborn.command.subcommand.GuiCommand;
import io.github.devskycore.faunareborn.command.subcommand.HelpCommand;
import io.github.devskycore.faunareborn.command.subcommand.LangCommand;
import io.github.devskycore.faunareborn.command.subcommand.ReloadCommand;
import io.github.devskycore.faunareborn.command.subcommand.VersionCommand;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.gui.FaunaMainGui;
import io.github.devskycore.faunareborn.gui.PluginGuiConfigService;
import io.github.devskycore.faunareborn.lang.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class FaunaCommand implements CommandExecutor, TabCompleter {

    private final CommandRegistry registry;

    public FaunaCommand(FaunaRebornPlugin plugin, FaunaMainGui mainGui, PluginGuiConfigService guiConfigService, LanguageManager language) {
        PermissionService permissionService = new PermissionService();
        CommandMessages commandMessages = new CommandMessages(language);

        HelpCommand helpCommand = new HelpCommand(permissionService, commandMessages, language);
        CommandRegistry builtRegistry = new CommandRegistry(
                CommandRegistry.defaults(
                        helpCommand,
                        new VersionCommand(plugin, language),
                        new AboutCommand(language),
                        new EntitiesCommand(guiConfigService, language),
                        new ReloadCommand(plugin.reloadService()),
                        new GuiCommand(mainGui, commandMessages),
                        new LangCommand(language, mainGui)
                ),
                permissionService,
                commandMessages
        );
        helpCommand.bindRegistry(builtRegistry);
        this.registry = builtRegistry;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        execute(sender, args);
        return true;
    }

    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        registry.execute(sender, args);
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        return suggest(sender, args);
    }

    public @NotNull List<String> suggest(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> suggestions = registry.suggest(sender, args);
        return suggestions.isEmpty() ? Collections.emptyList() : List.copyOf(suggestions);
    }
}
