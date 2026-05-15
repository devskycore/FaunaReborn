package io.github.devskycore.faunareborn.animal.pig.hostility;

import io.github.devskycore.faunareborn.animal.common.hostility.AbstractProvocationTaskRunner;
import io.github.devskycore.faunareborn.animal.pig.PigSettings;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.environment.EnvironmentAggressionSettings;
import io.github.devskycore.faunareborn.system.environment.WorldEnvironmentContextCache;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapter;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapters;
import io.github.devskycore.faunareborn.system.lod.LodSettings;

public final class PigProvocationTask extends AbstractProvocationTaskRunner {

    public PigProvocationTask(
            FaunaRebornPlugin plugin,
            PigSettings.RodProvocationSettings settings,
            PigSettings.ResourceProvocationSettings resourceSettings,
            PigSettings.SocialAlertSettings socialAlertSettings,
            PigSettings.GlobalHostilitySettings globalSettings,
            EnvironmentAggressionSettings environmentSettings,
            LodSettings lodSettings
    ) {
        this(createBundle(plugin, settings, resourceSettings, socialAlertSettings, globalSettings, environmentSettings, lodSettings));
    }

    private PigProvocationTask(Bundle bundle) {
        super(
                bundle.plugin(),
                bundle.scheduler(),
                bundle.interactionListener(),
                bundle.environmentCache(),
                bundle.aggressionController()::tick,
                bundle.aggressionController()::clearAll,
                bundle.interactionListener()::clearState
        );
    }

    private static Bundle createBundle(
            FaunaRebornPlugin plugin,
            PigSettings.RodProvocationSettings settings,
            PigSettings.ResourceProvocationSettings resourceSettings,
            PigSettings.SocialAlertSettings socialAlertSettings,
            PigSettings.GlobalHostilitySettings globalSettings,
            EnvironmentAggressionSettings environmentSettings,
            LodSettings lodSettings
    ) {
        SchedulerAdapter scheduler = SchedulerAdapters.create(plugin);
        WorldEnvironmentContextCache environmentCache = new WorldEnvironmentContextCache(plugin, environmentSettings);
        PigAggressionController aggressionController = new PigAggressionController(
                scheduler,
                settings,
                globalSettings,
                lodSettings,
                environmentCache
        );
        PigInteractionListener interactionListener = new PigInteractionListener(
                plugin,
                settings,
                socialAlertSettings,
                globalSettings,
                aggressionController,
                resourceSettings,
                environmentCache
        );
        return new Bundle(
                plugin,
                scheduler,
                interactionListener,
                environmentCache,
                aggressionController
        );
    }

    private record Bundle(
            FaunaRebornPlugin plugin,
            SchedulerAdapter scheduler,
            PigInteractionListener interactionListener,
            WorldEnvironmentContextCache environmentCache,
            PigAggressionController aggressionController
    ) {
    }
}
