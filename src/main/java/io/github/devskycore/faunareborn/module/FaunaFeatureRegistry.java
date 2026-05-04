package io.github.devskycore.faunareborn.module;

import io.github.devskycore.faunareborn.animal.chicken.hostility.ChickenHostilityFeature;
import io.github.devskycore.faunareborn.animal.cow.CowFeature;
import io.github.devskycore.faunareborn.config.PluginSettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettingsLoader;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;

import java.util.ArrayList;
import java.util.List;

public final class FaunaFeatureRegistry {

    private final List<FaunaFeature<? extends EntitySettings>> features;

    public FaunaFeatureRegistry(List<FaunaFeature<? extends EntitySettings>> features) {
        this.features = List.copyOf(features);
    }

    public static FaunaFeatureRegistry defaults() {
        return new FaunaFeatureRegistry(List.of(
                new ChickenHostilityFeature(),
                new CowFeature()
        ));
    }

    public List<EntitySettingsLoader<?>> createSettingsLoaders(FaunaRebornPlugin plugin) {
        List<EntitySettingsLoader<?>> loaders = new ArrayList<>(features.size());
        for (FaunaFeature<? extends EntitySettings> feature : features) {
            loaders.add(feature.createSettingsLoader(plugin));
        }
        return List.copyOf(loaders);
    }

    public List<FaunaModule> createModules(FaunaRebornPlugin plugin, PluginSettings settings) {
        List<FaunaModule> modules = new ArrayList<>(features.size());
        for (FaunaFeature<? extends EntitySettings> feature : features) {
            modules.add(createModule(plugin, settings, feature));
        }
        return List.copyOf(modules);
    }

    private static <T extends EntitySettings> FaunaModule createModule(
            FaunaRebornPlugin plugin,
            PluginSettings settings,
            FaunaFeature<T> feature
    ) {
        return feature.createModule(plugin, settings.require(feature.settingsType()), settings.global());
    }
}
