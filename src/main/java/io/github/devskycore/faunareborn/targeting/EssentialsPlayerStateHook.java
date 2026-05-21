package io.github.devskycore.faunareborn.targeting;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class EssentialsPlayerStateHook implements PlayerStateHook {

    @Override
    public String name() {
        return "EssentialsX";
    }

    @Override
    public boolean isAvailable() {
        return essentials() != null;
    }

    @Override
    public boolean isProtected(Player player) {
        Object user = user(player);
        if (user == null) {
            return false;
        }
        Boolean godMode = ReflectionHookSupport.invokeBoolean(
                user,
                "isGodModeEnabled",
                "getGodModeEnabled",
                "isGodMode",
                "getGodMode"
        );
        return godMode != null && godMode;
    }

    @Override
    public boolean isHidden(Player player) {
        Object user = user(player);
        if (user == null) {
            return false;
        }
        Boolean vanished = ReflectionHookSupport.invokeBoolean(
                user,
                "isVanished",
                "getVanished",
                "isHidden",
                "getHidden"
        );
        return vanished != null && vanished;
    }

    private Object user(Player player) {
        Plugin essentials = essentials();
        if (essentials == null || player == null) {
            return null;
        }
        return ReflectionHookSupport.invokeWithPlayer(essentials, player, "getUser");
    }

    private Plugin essentials() {
        Plugin plugin = ReflectionHookSupport.plugin("Essentials");
        return plugin != null ? plugin : ReflectionHookSupport.plugin("EssentialsX");
    }
}
