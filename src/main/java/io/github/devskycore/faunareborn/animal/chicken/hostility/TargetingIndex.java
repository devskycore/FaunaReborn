package io.github.devskycore.faunareborn.animal.chicken.hostility;

import io.github.devskycore.faunareborn.animal.common.hostility.SharedTargetingIndex;
import org.bukkit.entity.Chicken;

import java.util.UUID;

final class TargetingIndex {

    private final SharedTargetingIndex delegate = new SharedTargetingIndex();

    void clear() {
        delegate.clear();
    }

    int attackersForTarget(UUID targetId) {
        return delegate.attackersForTarget(targetId);
    }

    int activeInWorld(UUID worldId) {
        return delegate.activeInWorld(worldId);
    }

    int activeInChunk(Chicken chicken) {
        return delegate.activeInChunk(chicken);
    }

    void registerActive(Chicken chicken, UUID targetUuid) {
        delegate.registerActive(chicken, targetUuid);
    }

    void unregisterActive(Chicken chicken, UUID targetUuid) {
        delegate.unregisterActive(chicken, targetUuid);
    }

    void replaceTarget(UUID previousTarget, UUID nextTarget) {
        delegate.replaceTarget(previousTarget, nextTarget);
    }
}
