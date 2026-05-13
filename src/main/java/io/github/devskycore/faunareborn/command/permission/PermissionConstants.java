package io.github.devskycore.faunareborn.command.permission;

import org.bukkit.command.CommandSender;

public final class PermissionConstants {

    public static final String WILDCARD = "fauna.*";
    public static final String ADMIN = "fauna.admin";

    public static final String COMMAND_HELP = "fauna.command.help";
    public static final String COMMAND_HELP_ADMIN = "fauna.command.help.admin";
    public static final String COMMAND_VERSION = "fauna.command.version";
    public static final String COMMAND_ABOUT = "fauna.command.about";
    public static final String COMMAND_ENTITIES = "fauna.command.entities";
    public static final String COMMAND_RELOAD = "fauna.command.reload";
    public static final String COMMAND_GUI = "fauna.command.gui";

    private PermissionConstants() {
    }

    public static boolean hasAny(CommandSender sender, String... permissions) {
        for (String permission : permissions) {
            if (sender.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
}
