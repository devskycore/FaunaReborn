package io.github.devskycore.faunareborn.targeting;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

final class PlayerVisibilityService {

    private final ExternalPlayerStateService externalPlayerStateService;

    PlayerVisibilityService(ExternalPlayerStateService externalPlayerStateService) {
        this.externalPlayerStateService = externalPlayerStateService;
    }

    boolean isHidden(Player player, boolean checkPotion, boolean checkVanish) {
        if (player == null) {
            return true;
        }
        if (checkPotion && player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            return true;
        }
        if (!checkVanish) {
            return false;
        }
        return externalPlayerStateService.isHidden(player);
    }
}
