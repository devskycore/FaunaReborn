package io.github.devskycore.faunareborn.system.lod;

public final class LodResolver {

    private LodResolver() {
    }

    public static LodTier resolveTier(LodSettings settings, LodTier currentTier, double distanceSq, boolean forceHigh) {
        if (!settings.enabled() || forceHigh) {
            return LodTier.HIGH;
        }

        if (currentTier == null) {
            if (distanceSq <= settings.highDistanceSq()) return LodTier.HIGH;
            if (distanceSq <= settings.mediumDistanceSq()) return LodTier.MEDIUM;
            if (distanceSq <= settings.lowDistanceSq()) return LodTier.LOW;
            return LodTier.OFF;
        }

        return switch (currentTier) {
            case HIGH -> distanceSq > settings.highDemoteSq() ? LodTier.MEDIUM : LodTier.HIGH;
            case MEDIUM -> {
                if (distanceSq <= settings.highPromoteSq()) yield LodTier.HIGH;
                if (distanceSq > settings.mediumDemoteSq()) yield LodTier.LOW;
                yield LodTier.MEDIUM;
            }
            case LOW -> {
                if (distanceSq <= settings.mediumPromoteSq()) yield LodTier.MEDIUM;
                if (distanceSq > settings.lowDemoteSq()) yield LodTier.OFF;
                yield LodTier.LOW;
            }
            case OFF -> distanceSq <= settings.lowPromoteSq() ? LodTier.LOW : LodTier.OFF;
        };
    }
}
