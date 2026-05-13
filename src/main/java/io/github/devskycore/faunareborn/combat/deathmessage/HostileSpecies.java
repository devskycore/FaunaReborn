package io.github.devskycore.faunareborn.combat.deathmessage;

import org.bukkit.entity.EntityType;

public enum HostileSpecies {
    PIG(EntityType.PIG),
    COW(EntityType.COW),
    CHICKEN(EntityType.CHICKEN);

    private final EntityType entityType;

    HostileSpecies(EntityType entityType) {
        this.entityType = entityType;
    }

    public static HostileSpecies from(EntityType entityType) {
        for (HostileSpecies species : values()) {
            if (species.entityType == entityType) {
                return species;
            }
        }
        return null;
    }
}
