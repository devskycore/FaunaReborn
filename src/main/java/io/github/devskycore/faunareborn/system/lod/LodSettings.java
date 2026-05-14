package io.github.devskycore.faunareborn.system.lod;

public record LodSettings(
        boolean enabled,
        double highDistance,
        double mediumDistance,
        double lowDistance,
        double hysteresisDistance,
        int highIntervalTicks,
        int mediumIntervalTicks,
        int lowIntervalTicks,
        int offIntervalTicks
) {
    public LodSettings {
        highDistance = Math.max(1.0D, highDistance);
        mediumDistance = Math.max(highDistance, mediumDistance);
        lowDistance = Math.max(mediumDistance, lowDistance);
        hysteresisDistance = Math.max(0.0D, hysteresisDistance);
        highIntervalTicks = Math.max(1, highIntervalTicks);
        mediumIntervalTicks = Math.max(1, mediumIntervalTicks);
        lowIntervalTicks = Math.max(1, lowIntervalTicks);
        offIntervalTicks = Math.max(1, offIntervalTicks);
    }

    public double highDistanceSq() {
        return highDistance * highDistance;
    }

    public double mediumDistanceSq() {
        return mediumDistance * mediumDistance;
    }

    public double lowDistanceSq() {
        return lowDistance * lowDistance;
    }

    public double highPromoteSq() {
        return square(Math.max(1.0D, highDistance - hysteresisDistance));
    }

    public double mediumPromoteSq() {
        return square(Math.max(highDistance, mediumDistance - hysteresisDistance));
    }

    public double lowPromoteSq() {
        return square(Math.max(mediumDistance, lowDistance - hysteresisDistance));
    }

    public double highDemoteSq() {
        return square(highDistance + hysteresisDistance);
    }

    public double mediumDemoteSq() {
        return square(mediumDistance + hysteresisDistance);
    }

    public double lowDemoteSq() {
        return square(lowDistance + hysteresisDistance);
    }

    public int intervalFor(LodTier tier) {
        return switch (tier) {
            case HIGH -> highIntervalTicks;
            case MEDIUM -> mediumIntervalTicks;
            case LOW -> lowIntervalTicks;
            case OFF -> offIntervalTicks;
        };
    }

    private static double square(double value) {
        return value * value;
    }
}
