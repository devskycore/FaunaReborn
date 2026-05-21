package io.github.devskycore.faunareborn.targeting;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

final class ReflectionHookSupport {

    private ReflectionHookSupport() {
    }

    static Plugin plugin(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.isEnabled() ? plugin : null;
    }

    static Object invokeAny(Object target, String... methodNames) {
        if (target == null) {
            return null;
        }
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // Try the next candidate name.
            } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    static Boolean invokeBoolean(Object target, String... methodNames) {
        Object value = invokeAny(target, methodNames);
        return value instanceof Boolean booleanValue ? booleanValue : null;
    }

    static Object invokeWithPlayer(Object target, Player player, String methodName) {
        if (target == null || player == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName, Player.class);
            return method.invoke(target, player);
        } catch (NoSuchMethodException ignored) {
            // Try UUID and name variants below.
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName, UUID.class);
            return method.invoke(target, player.getUniqueId());
        } catch (NoSuchMethodException ignored) {
            // Try name variant below.
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName, String.class);
            return method.invoke(target, player.getName());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
            return null;
        }
    }

    static Boolean invokeBooleanWithPlayer(Object target, Player player, String methodName) {
        Object value = invokeWithPlayer(target, player, methodName);
        return value instanceof Boolean booleanValue ? booleanValue : null;
    }
}
