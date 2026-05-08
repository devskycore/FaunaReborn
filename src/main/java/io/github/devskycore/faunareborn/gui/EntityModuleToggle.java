package io.github.devskycore.faunareborn.gui;

import io.github.devskycore.faunareborn.config.entity.EntityType;
import org.bukkit.Material;

public record EntityModuleToggle(
        EntityType entityType,
        String moduleId,
        String label,
        String enabledPath,
        Material icon
) {
}
