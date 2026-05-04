package io.github.devskycore.faunareborn.config;

import io.github.devskycore.faunareborn.animal.chicken.config.ChickenHostilitySettings;
import io.github.devskycore.faunareborn.animal.cow.CowSettings;

public record PluginSettings(
        GlobalSettings global,
        ChickenHostilitySettings chickenHostility,
        CowSettings cow
) {
}
