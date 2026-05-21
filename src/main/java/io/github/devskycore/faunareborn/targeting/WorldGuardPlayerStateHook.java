package io.github.devskycore.faunareborn.targeting;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class WorldGuardPlayerStateHook implements PlayerStateHook {

    @Override
    public String name() {
        return "WorldGuard";
    }

    @Override
    public boolean isAvailable() {
        return ReflectionHookSupport.plugin("WorldGuard") != null;
    }

    @Override
    public boolean isProtected(Player player) {
        if (player == null || !isAvailable()) {
            return false;
        }
        Boolean invincible = queryState(player, "INVINCIBILITY");
        if (Boolean.TRUE.equals(invincible)) {
            return true;
        }
        Boolean mobDamage = queryState(player, "MOB_DAMAGE");
        return Boolean.FALSE.equals(mobDamage);
    }

    private Boolean queryState(Player player, String flagName) {
        try {
            Object adaptedLocation = adaptLocation(player);
            Object localPlayer = wrapPlayer(player);
            Object flag = flag(flagName);
            Object query = regionQuery();
            if (adaptedLocation == null || localPlayer == null || flag == null || query == null) {
                return null;
            }

            for (Method method : query.getClass().getMethods()) {
                if (!"testState".equals(method.getName()) || method.getParameterCount() != 3) {
                    continue;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (!parameterTypes[0].isInstance(adaptedLocation) || !parameterTypes[1].isInstance(localPlayer)) {
                    continue;
                }
                if (!parameterTypes[2].isArray()) {
                    continue;
                }
                Object flags = Array.newInstance(parameterTypes[2].getComponentType(), 1);
                Array.set(flags, 0, flag);
                Object result = method.invoke(query, adaptedLocation, localPlayer, flags);
                return result instanceof Boolean booleanResult ? booleanResult : null;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private Object adaptLocation(Player player) throws ReflectiveOperationException {
        Class<?> adapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
        Method adapt = adapterClass.getMethod("adapt", org.bukkit.Location.class);
        return adapt.invoke(null, player.getLocation());
    }

    private Object wrapPlayer(Player player) throws ReflectiveOperationException {
        Class<?> pluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
        Method inst = pluginClass.getMethod("inst");
        Object plugin = inst.invoke(null);
        Method wrapPlayer = pluginClass.getMethod("wrapPlayer", Player.class);
        return wrapPlayer.invoke(plugin, player);
    }

    private Object flag(String flagName) throws ReflectiveOperationException {
        Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
        Field field = flagsClass.getField(flagName);
        return field.get(null);
    }

    private Object regionQuery() throws ReflectiveOperationException {
        Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
        Method getInstance = worldGuardClass.getMethod("getInstance");
        Object worldGuard = getInstance.invoke(null);
        Object platform = worldGuardClass.getMethod("getPlatform").invoke(worldGuard);
        Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);
        return regionContainer.getClass().getMethod("createQuery").invoke(regionContainer);
    }
}
