package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.FaunaReloadService;
import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
import io.github.devskycore.faunareborn.command.permission.PermissionService;
import org.bukkit.command.CommandSender;

public final class ReloadCommand implements FaunaSubcommand {

    private static final CommandInfo INFO = new CommandInfo(
            "reload",
            "/fauna reload",
            "Reload configuration and modules.",
            PermissionConstants.COMMAND_RELOAD,
            true
    );

    private final FaunaReloadService reloadService;

    public ReloadCommand(FaunaReloadService reloadService) {
        this.reloadService = reloadService;
    }

    @Override
    public CommandInfo info() {
        return INFO;
    }

    @Override
    public boolean canAccess(CommandSender sender, PermissionService permissions) {
        return permissions.canUseReload(sender);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        reloadService.reload(sender);
    }
}
