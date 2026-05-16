package io.github.devskycore.faunareborn.system.update;

public record UpdateCheckResult(boolean updateAvailable, String latestVersion, String releaseUrl) {
}
