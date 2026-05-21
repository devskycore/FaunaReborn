package io.github.devskycore.faunareborn.targeting;

import org.bukkit.entity.Player;

final class MetadataProtectionService implements PlayerProtectionService {

    private final ExternalPlayerStateService delegate = new ExternalPlayerStateService();

    @Override
    public boolean isProtected(Player player) {
        return delegate.isProtected(player);
    }
}
