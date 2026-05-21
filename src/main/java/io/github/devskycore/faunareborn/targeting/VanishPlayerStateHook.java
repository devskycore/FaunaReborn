package io.github.devskycore.faunareborn.targeting;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class VanishPlayerStateHook implements PlayerStateHook {

    private final String pluginName;

    VanishPlayerStateHook(String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    public String name() {
        return pluginName;
    }

    @Override
    public boolean isAvailable() {
        return plugin() != null;
    }

    @Override
    public boolean isHidden(Player player) {
        Plugin plugin = plugin();
        if (plugin == null || player == null) {
            return false;
        }

        Object vanishStateManager = ReflectionHookSupport.invokeAny(
                plugin,
                "getVanishStateMgr",
                "getVanishStateManager",
                "getPlayerVisibilityManager"
        );
        if (vanishStateManager != null) {
            Boolean vanished = ReflectionHookSupport.invokeBooleanWithPlayer(vanishStateManager, player, "isVanished");
            if (vanished != null) {
                return vanished;
            }
            vanished = ReflectionHookSupport.invokeBooleanWithPlayer(vanishStateManager, player, "isInvisible");
            if (vanished != null) {
                return vanished;
            }
        }

        Boolean vanished = ReflectionHookSupport.invokeBooleanWithPlayer(plugin, player, "isVanished");
        if (vanished != null) {
            return vanished;
        }
        vanished = ReflectionHookSupport.invokeBooleanWithPlayer(plugin, player, "isInvisible");
        return vanished != null && vanished;
    }

    private Plugin plugin() {
        return ReflectionHookSupport.plugin(pluginName);
    }
}
