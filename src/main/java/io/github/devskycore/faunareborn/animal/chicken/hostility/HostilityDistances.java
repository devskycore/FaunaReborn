package io.github.devskycore.faunareborn.animal.chicken.hostility;

import org.bukkit.entity.Entity;

final class HostilityDistances {

    private HostilityDistances() {
    }

    static double distanceSq(Entity a, Entity b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    static double distanceSq2D(Entity a, Entity b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
