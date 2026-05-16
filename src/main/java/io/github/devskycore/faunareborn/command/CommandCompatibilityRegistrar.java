package io.github.devskycore.faunareborn.command;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;

public final class CommandCompatibilityRegistrar {

    private static final String COMMAND_NAME = "fauna";
    private static final String COMMAND_DESCRIPTION = "Main command for FaunaReborn.";
    private static final List<String> COMMAND_ALIASES = List.of("faunareborn", "fr");

    private CommandCompatibilityRegistrar() {
    }

    public static void register(FaunaRebornPlugin plugin, FaunaCommand faunaCommand) {
        if (tryPaperRegisterCommand(plugin, faunaCommand)) {
            return;
        }
        if (tryPluginYamlRegistration(plugin, faunaCommand)) {
            return;
        }
        registerInCommandMap(plugin, faunaCommand);
    }

    private static boolean tryPluginYamlRegistration(FaunaRebornPlugin plugin, FaunaCommand faunaCommand) {
        try {
            org.bukkit.command.PluginCommand pluginCommand = plugin.getCommand(COMMAND_NAME);
            if (pluginCommand == null) {
                return false;
            }
            pluginCommand.setExecutor(faunaCommand);
            pluginCommand.setTabCompleter(faunaCommand);
            pluginCommand.setAliases(COMMAND_ALIASES);
            pluginCommand.setDescription(COMMAND_DESCRIPTION);
            pluginCommand.setUsage("/fauna help");
            pluginCommand.setPermission("fauna.command.help");
            return true;
        } catch (UnsupportedOperationException ignored) {
            // Paper plugins do not support YAML command declarations via getCommand().
            return false;
        }
    }

    private static boolean tryPaperRegisterCommand(FaunaRebornPlugin plugin, FaunaCommand faunaCommand) {
        try {
            Class<?> basicCommandClass = Class.forName("io.papermc.paper.command.brigadier.BasicCommand");
            Method registerCommandMethod = plugin.getClass().getMethod(
                    "registerCommand",
                    String.class,
                    String.class,
                    Collection.class,
                    basicCommandClass
            );

            InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
                case "execute" -> {
                    CommandSender sender = resolveSender(args[0]);
                    if (sender == null) {
                        yield null;
                    }
                    faunaCommand.execute(sender, (String[]) args[1]);
                    yield null;
                }
                case "suggest" -> {
                    CommandSender sender = resolveSender(args[0]);
                    if (sender == null) {
                        yield List.of();
                    }
                    yield faunaCommand.suggest(sender, (String[]) args[1]);
                }
                case "canUse" -> {
                    CommandSender sender = resolveSender(args[0]);
                    yield sender != null && sender.hasPermission("fauna.command.help");
                }
                case "permission" -> "";
                default -> method.getDefaultValue();
            };

            Object basicCommandProxy = Proxy.newProxyInstance(
                    basicCommandClass.getClassLoader(),
                    new Class<?>[]{basicCommandClass},
                    handler
            );

            MethodHandle registerCommandHandle = MethodHandles.lookup().unreflect(registerCommandMethod);
            registerCommandHandle.invokeWithArguments(
                    plugin,
                    COMMAND_NAME,
                    COMMAND_DESCRIPTION,
                    COMMAND_ALIASES,
                    basicCommandProxy
            );
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static CommandSender resolveSender(Object source) {
        if (source instanceof CommandSender sender) {
            return sender;
        }
        if (source == null) {
            return null;
        }
        try {
            Method getSender = source.getClass().getMethod("getSender");
            Object sender = getSender.invoke(source);
            return sender instanceof CommandSender commandSender ? commandSender : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void registerInCommandMap(FaunaRebornPlugin plugin, FaunaCommand faunaCommand) {
        try {
            Method getCommandMap = plugin.getServer().getClass().getMethod("getCommandMap");
            CommandMap commandMap = (CommandMap) getCommandMap.invoke(plugin.getServer());
            Command command = new LegacyFaunaCommand(faunaCommand);
            commandMap.register(plugin.getName().toLowerCase(), command);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to register command 'fauna' using compatibility fallback.", exception);
        }
    }

    private static final class LegacyFaunaCommand extends Command {

        private final FaunaCommand faunaCommand;

        private LegacyFaunaCommand(FaunaCommand faunaCommand) {
            super(COMMAND_NAME, COMMAND_DESCRIPTION, "/fauna help", COMMAND_ALIASES);
            this.faunaCommand = faunaCommand;
            setPermission("fauna.command.help");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String @NotNull [] args) {
            faunaCommand.execute(sender, args);
            return true;
        }

        @Override
        public @NotNull List<String> tabComplete(
                @NotNull CommandSender sender,
                @NotNull String alias,
                @NotNull String @NotNull [] args
        ) {
            return faunaCommand.suggest(sender, args);
        }
    }
}
