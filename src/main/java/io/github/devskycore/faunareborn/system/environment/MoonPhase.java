package io.github.devskycore.faunareborn.system.environment;

public enum MoonPhase {
    FULL_MOON,
    WANING_GIBBOUS,
    LAST_QUARTER,
    WANING_CRESCENT,
    NEW_MOON,
    WAXING_CRESCENT,
    FIRST_QUARTER,
    WAXING_GIBBOUS;

    public static MoonPhase fromIndex(int phase) {
        return switch (Math.floorMod(phase, 8)) {
            case 0 -> FULL_MOON;
            case 1 -> WANING_GIBBOUS;
            case 2 -> LAST_QUARTER;
            case 3 -> WANING_CRESCENT;
            case 4 -> NEW_MOON;
            case 5 -> WAXING_CRESCENT;
            case 6 -> FIRST_QUARTER;
            default -> WAXING_GIBBOUS;
        };
    }
}
