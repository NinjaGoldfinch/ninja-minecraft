package nz.ninjagoldfinch.minecraft;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

/** Runs the refresh loop and shows each display to the players subscribed to it. */
final class DisplayManager implements Listener {

    private final NinjaMinecraft plugin;
    private final Subscriptions subscriptions;
    private final TabListDisplay tabList = new TabListDisplay();
    private final ScoreboardDisplay scoreboard = new ScoreboardDisplay();
    private BukkitTask task;

    DisplayManager(NinjaMinecraft plugin) {
        this.plugin = plugin;
        this.subscriptions = new Subscriptions(plugin);
    }

    void reload(FileConfiguration config) {
        // Clear everything shown under the old settings; the next refresh redraws it.
        stop();
        tabList.reload(config);
        scoreboard.reload(config);
        subscriptions.reload(config);

        long interval = Math.max(1, config.getLong("update-interval-ticks", 20));
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refresh, 1L, interval);
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            tabList.hide(player);
            scoreboard.hide(player);
        }
    }

    boolean isEnabled(Display display) {
        return switch (display) {
            case TABLIST -> tabList.enabled();
            case SCOREBOARD -> scoreboard.enabled();
        };
    }

    boolean isSubscribed(Player player, Display display) {
        return subscriptions.isSubscribed(player, display);
    }

    void setSubscribed(Player player, Display display, boolean subscribed) {
        subscriptions.set(player, display, subscribed);
        if (subscribed) {
            refresh(player, ServerStats.capture(plugin.getServer()));
        } else {
            switch (display) {
                case TABLIST -> tabList.hide(player);
                case SCOREBOARD -> scoreboard.hide(player);
            }
        }
    }

    private void refresh() {
        if (plugin.getServer().getOnlinePlayers().isEmpty()) {
            return;
        }
        ServerStats stats = ServerStats.capture(plugin.getServer());
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            refresh(player, stats);
        }
    }

    private void refresh(Player player, ServerStats stats) {
        var placeholders = stats.resolver(player);
        if (tabList.enabled() && subscriptions.isSubscribed(player, Display.TABLIST)) {
            tabList.show(player, placeholders);
        }
        if (scoreboard.enabled() && subscriptions.isSubscribed(player, Display.SCOREBOARD)) {
            scoreboard.show(player, placeholders);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        scoreboard.forget(event.getPlayer().getUniqueId());
    }
}
