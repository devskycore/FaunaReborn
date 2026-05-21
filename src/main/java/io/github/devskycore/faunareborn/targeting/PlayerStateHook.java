package io.github.devskycore.faunareborn.targeting;

import org.bukkit.entity.Player;

interface PlayerStateHook {

    String name();

    boolean isAvailable();

    default boolean isProtected(Player player) {
        return false;
    }

    default boolean isHidden(Player player) {
        return false;
    }
}
