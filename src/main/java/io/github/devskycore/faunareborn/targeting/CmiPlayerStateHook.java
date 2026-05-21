package io.github.devskycore.faunareborn.targeting;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class CmiPlayerStateHook implements PlayerStateHook {

    @Override
    public String name() {
        return "CMI";
    }

    @Override
    public boolean isAvailable() {
        return cmi() != null;
    }

    @Override
    public boolean isProtected(Player player) {
        Object user = user(player);
        if (user == null) {
            return false;
        }
        Boolean godMode = ReflectionHookSupport.invokeBoolean(
                user,
                "isGod",
                "isGodMode",
                "isGodModeEnabled",
                "getGod",
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
                "isVanish",
                "getVanished",
                "getVanish"
        );
        return vanished != null && vanished;
    }

    private Object user(Player player) {
        Plugin cmi = cmi();
        if (cmi == null || player == null) {
            return null;
        }
        Object playerManager = ReflectionHookSupport.invokeAny(cmi, "getPlayerManager");
        if (playerManager == null) {
            return ReflectionHookSupport.invokeWithPlayer(cmi, player, "getUser");
        }
        Object user = ReflectionHookSupport.invokeWithPlayer(playerManager, player, "getUser");
        if (user != null) {
            return user;
        }
        return ReflectionHookSupport.invokeWithPlayer(playerManager, player, "getUserByName");
    }

    private Plugin cmi() {
        return ReflectionHookSupport.plugin("CMI");
    }
}
