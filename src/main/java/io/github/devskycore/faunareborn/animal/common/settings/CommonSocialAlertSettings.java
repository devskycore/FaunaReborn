package io.github.devskycore.faunareborn.animal.common.settings;

public class CommonSocialAlertSettings extends BaseSocialAlertSettings {
    public CommonSocialAlertSettings(
            boolean enabled,
            boolean onDamage,
            boolean onNearbyDeath,
            boolean responderAdultsOnly,
            double radius,
            int cooldownTicks,
            int joinCooldownTicks,
            int maxResponders
    ) {
        super(
                enabled,
                onDamage,
                onNearbyDeath,
                responderAdultsOnly,
                radius,
                cooldownTicks,
                joinCooldownTicks,
                maxResponders
        );
    }
}
