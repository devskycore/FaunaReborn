package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.message.CommandMessages;
import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
import io.github.devskycore.faunareborn.command.permission.PermissionService;
import io.github.devskycore.faunareborn.gui.FaunaMainGui;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GuiCommand implements FaunaSubcommand {

    private static final CommandInfo INFO = new CommandInfo(
            "gui",
            "/fauna gui",
            "Open the FaunaReborn admin GUI.",
            PermissionConstants.COMMAND_GUI,
            true
    );

    private final FaunaMainGui mainGui;

    public GuiCommand(FaunaMainGui mainGui) {
        this.mainGui = mainGui;
    }

    @Override
    public CommandInfo info() {
        return INFO;
    }

    @Override
    public boolean canAccess(CommandSender sender, PermissionService permissions) {
        return permissions.canUseGui(sender);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            CommandMessages.sendPlayerOnly(sender);
            return;
        }
        mainGui.open(player);
    }
}
