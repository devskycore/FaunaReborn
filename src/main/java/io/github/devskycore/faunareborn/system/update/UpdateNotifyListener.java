package io.github.devskycore.faunareborn.system.update;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class UpdateNotifyListener implements Listener {

    private final GitHubUpdateChecker updateChecker;

    public UpdateNotifyListener(GitHubUpdateChecker updateChecker) {
        this.updateChecker = updateChecker;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updateChecker.notifyPlayerIfUpdateAvailable(event.getPlayer());
    }
}
