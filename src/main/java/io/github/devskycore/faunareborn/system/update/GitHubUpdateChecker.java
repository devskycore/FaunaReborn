package io.github.devskycore.faunareborn.system.update;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GitHubUpdateChecker {

    private static final String GITHUB_REPOSITORY = "devskycore/FaunaReborn";
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern HTML_URL_PATTERN = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");

    private final FaunaRebornPlugin plugin;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private volatile UpdateCheckResult lastResult;

    public GitHubUpdateChecker(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "FaunaReborn-UpdateCheck");
            thread.setDaemon(true);
            return thread;
        };
        this.executor = Executors.newSingleThreadExecutor(factory);
    }

    public void checkNowAsync() {
        if (!plugin.getConfig().getBoolean("update-checker.enabled", true)) {
            return;
        }
        CompletableFuture.supplyAsync(this::fetchLatestRelease, executor)
                .thenAccept(result -> {
                    lastResult = result;
                    if (!result.updateAvailable()) {
                        return;
                    }
                    String currentVersion = plugin.getPluginMeta().getVersion();
                    plugin.getLogger().info(plugin.languageManager().text(
                            "logs.update.available",
                            "A new version is available: {latest} (current: {current}) -> {url}",
                            Map.of(
                                    "latest", result.latestVersion(),
                                    "current", currentVersion,
                                    "url", result.releaseUrl()
                            )
                    ));
                    notifyOnlineAdmins(result, currentVersion);
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().log(Level.FINE, "Update check failed: " + throwable.getMessage());
                    return null;
                });
    }

    public void notifyPlayerIfUpdateAvailable(Player player) {
        UpdateCheckResult result = lastResult;
        if (result == null || !result.updateAvailable()) {
            return;
        }
        if (!player.hasPermission("fauna.admin")) {
            return;
        }
        String currentVersion = plugin.getPluginMeta().getVersion();
        player.sendMessage(plugin.languageManager().text(
                "logs.update.available",
                "A new version is available: {latest} (current: {current}) -> {url}",
                Map.of(
                        "latest", result.latestVersion(),
                        "current", currentVersion,
                        "url", result.releaseUrl()
                )
        ));
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private UpdateCheckResult fetchLatestRelease() {
        String encodedRepository = encodeRepository();
        String apiUrl = "https://api.github.com/repos/" + encodedRepository + "/releases/latest";

        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .GET()
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "FaunaReborn-UpdateChecker")
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("GitHub API returned HTTP " + response.statusCode());
            }
            String body = response.body();
            String latestTag = readValue(TAG_NAME_PATTERN, body);
            String releaseUrl = readValue(HTML_URL_PATTERN, body);
            String currentVersion = plugin.getPluginMeta().getVersion();
            boolean newer = isVersionNewer(latestTag, currentVersion);
            return new UpdateCheckResult(newer, normalizeVersion(latestTag), releaseUrl);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Could not reach GitHub releases API.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not reach GitHub releases API.", exception);
        }
    }

    private void notifyOnlineAdmins(UpdateCheckResult result, String currentVersion) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.hasPermission("fauna.admin")) {
                    continue;
                }
                onlinePlayer.sendMessage(plugin.languageManager().text(
                        "logs.update.available",
                        "A new version is available: {latest} (current: {current}) -> {url}",
                        Map.of(
                                "latest", result.latestVersion(),
                                "current", currentVersion,
                                "url", result.releaseUrl()
                        )
                ));
            }
        });
    }

    private String encodeRepository() {
        String trimmed = GITHUB_REPOSITORY.trim();
        String[] split = trimmed.split("/", 2);
        if (split.length != 2) {
            return "devskycore/FaunaReborn";
        }
        return URLEncoder.encode(split[0], StandardCharsets.UTF_8) + "/" + URLEncoder.encode(split[1], StandardCharsets.UTF_8);
    }

    private String readValue(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new IllegalStateException("Missing expected field in GitHub API response.");
        }
        return matcher.group(1);
    }

    private boolean isVersionNewer(String latestRaw, String currentRaw) {
        String latest = normalizeVersion(latestRaw);
        String current = normalizeVersion(currentRaw);
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");
        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int latestPart = i < latestParts.length ? parsePart(latestParts[i]) : 0;
            int currentPart = i < currentParts.length ? parsePart(currentParts[i]) : 0;
            if (latestPart > currentPart) {
                return true;
            }
            if (latestPart < currentPart) {
                return false;
            }
        }
        return false;
    }

    private String normalizeVersion(String value) {
        if (value == null) {
            return "0.0.0";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("v")) {
            normalized = normalized.substring(1);
        }
        int dash = normalized.indexOf('-');
        if (dash > 0) {
            normalized = normalized.substring(0, dash);
        }
        return normalized;
    }

    private int parsePart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
