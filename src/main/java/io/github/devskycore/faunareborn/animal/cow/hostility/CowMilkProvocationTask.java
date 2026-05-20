package io.github.devskycore.faunareborn.animal.cow.hostility;

import io.github.devskycore.faunareborn.animal.common.hostility.AbstractProvocationTaskRunner;
import io.github.devskycore.faunareborn.animal.cow.CowSettings;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.environment.EnvironmentAggressionSettings;
import io.github.devskycore.faunareborn.system.environment.WorldEnvironmentContextCache;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapter;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapters;
import io.github.devskycore.faunareborn.system.lod.LodSettings;

public final class CowMilkProvocationTask extends AbstractProvocationTaskRunner {

    public CowMilkProvocationTask(
            FaunaRebornPlugin plugin,
            CowSettings.MilkProvocationSettings settings,
            CowSettings.ResourceProvocationSettings resourceSettings,
            CowSettings.SocialAlertSettings socialAlertSettings,
            CowSettings.GlobalHostilitySettings globalSettings,
            EnvironmentAggressionSettings environmentSettings,
            LodSettings lodSettings
    ) {
        this(createBundle(plugin, settings, resourceSettings, socialAlertSettings, globalSettings, environmentSettings, lodSettings));
    }

    private CowMilkProvocationTask(Bundle bundle) {
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
            CowSettings.MilkProvocationSettings settings,
            CowSettings.ResourceProvocationSettings resourceSettings,
            CowSettings.SocialAlertSettings socialAlertSettings,
            CowSettings.GlobalHostilitySettings globalSettings,
            EnvironmentAggressionSettings environmentSettings,
            LodSettings lodSettings
    ) {
        SchedulerAdapter scheduler = SchedulerAdapters.create(plugin);
        WorldEnvironmentContextCache environmentCache = new WorldEnvironmentContextCache(plugin, environmentSettings);
        CowMilkAggressionController aggressionController = new CowMilkAggressionController(
                scheduler,
                settings,
                globalSettings,
                lodSettings,
                environmentCache
        );
        CowMilkInteractionListener interactionListener = new CowMilkInteractionListener(
                plugin,
                settings,
                socialAlertSettings,
                globalSettings,
                aggressionController,
                resourceSettings
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
            CowMilkInteractionListener interactionListener,
            WorldEnvironmentContextCache environmentCache,
            CowMilkAggressionController aggressionController
    ) {
    }
}
