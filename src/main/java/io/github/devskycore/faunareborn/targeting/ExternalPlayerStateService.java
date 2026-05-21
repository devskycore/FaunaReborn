package io.github.devskycore.faunareborn.targeting;

import org.bukkit.entity.Player;

import java.util.List;

final class ExternalPlayerStateService implements PlayerProtectionService {

    private final List<PlayerStateHook> hooks = List.of(
            new EssentialsPlayerStateHook(),
            new CmiPlayerStateHook(),
            new WorldGuardPlayerStateHook(),
            new VanishPlayerStateHook("SuperVanish"),
            new VanishPlayerStateHook("PremiumVanish"),
            new MetadataPlayerStateHook()
    );

    @Override
    public boolean isProtected(Player player) {
        if (player == null) {
            return false;
        }
        for (PlayerStateHook hook : hooks) {
            if (!hook.isAvailable()) {
                continue;
            }
            if (hook.isProtected(player)) {
                return true;
            }
        }
        return false;
    }

    boolean isHidden(Player player) {
        if (player == null) {
            return true;
        }
        for (PlayerStateHook hook : hooks) {
            if (!hook.isAvailable()) {
                continue;
            }
            if (hook.isHidden(player)) {
                return true;
            }
        }
        return false;
    }
}
