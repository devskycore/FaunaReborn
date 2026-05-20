package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.permission.PermissionService;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public interface FaunaSubcommand {

    CommandInfo info();

    void execute(CommandSender sender, String[] args);

    default List<String> suggest(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    default boolean cannotAccess(CommandSender sender, PermissionService permissions) {
        return !permissions.hasPermission(sender, info().permission());
    }
}
