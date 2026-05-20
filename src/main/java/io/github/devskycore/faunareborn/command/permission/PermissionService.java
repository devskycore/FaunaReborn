package io.github.devskycore.faunareborn.command.permission;

import org.bukkit.command.CommandSender;

public final class PermissionService {

    public boolean canUseReload(CommandSender sender) {
        return PermissionConstants.hasAny(sender,
                PermissionConstants.COMMAND_RELOAD,
                PermissionConstants.ADMIN,
                PermissionConstants.WILDCARD
        );
    }

    public boolean canUseGui(CommandSender sender) {
        return PermissionConstants.hasAny(sender,
                PermissionConstants.COMMAND_GUI,
                PermissionConstants.ADMIN,
                PermissionConstants.WILDCARD
        );
    }

    public boolean canUseLang(CommandSender sender) {
        return PermissionConstants.hasAny(sender,
                PermissionConstants.COMMAND_LANG,
                PermissionConstants.ADMIN,
                PermissionConstants.WILDCARD
        );
    }

    public boolean canViewAdminHelp(CommandSender sender) {
        return PermissionConstants.hasAny(sender,
                PermissionConstants.COMMAND_HELP_ADMIN,
                PermissionConstants.ADMIN,
                PermissionConstants.WILDCARD
        );
    }

    public boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission(PermissionConstants.WILDCARD);
    }
}
