package io.github.devskycore.faunareborn.animal.common.hostility;

import io.github.devskycore.faunareborn.combat.deathmessage.HostilityCause;
import org.bukkit.Material;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class HostilityEventSupport {

    private HostilityEventSupport() {
    }

    public static boolean isNaturalSpawn(CreatureSpawnEvent.SpawnReason spawnReason) {
        return spawnReason == CreatureSpawnEvent.SpawnReason.NATURAL
                || spawnReason == CreatureSpawnEvent.SpawnReason.DEFAULT
                || spawnReason == CreatureSpawnEvent.SpawnReason.BREEDING
                || spawnReason == CreatureSpawnEvent.SpawnReason.COMMAND
                || "CHUNK_GEN".equals(spawnReason.name());
    }

    public static Player resolveDamagingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        if (damager instanceof Tameable tameable) {
            AnimalTamer owner = tameable.getOwner();
            if (owner instanceof Player player) {
                return player;
            }
        }
        if (damager instanceof TNTPrimed tnt) {
            Entity source = tnt.getSource();
            if (source instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    public static HostilityCause resolveCookingCause(Material cookerType) {
        return switch (cookerType) {
            case FURNACE -> HostilityCause.COOKING_FURNACE;
            case SMOKER -> HostilityCause.COOKING_SMOKER;
            case CAMPFIRE, SOUL_CAMPFIRE -> HostilityCause.COOKING_CAMPFIRE;
            default -> HostilityCause.TERRITORIAL_PICKUP;
        };
    }
}
