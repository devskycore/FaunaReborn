package io.github.devskycore.faunareborn.system.platform;

public final class RuntimePlatform {

    private static final boolean FOLIA = detectFolia();

    private RuntimePlatform() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
