package nz.ninjagoldfinch.minecraft;

import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/** Periodically writes TPS / MSPT into every player's tab list header and footer. */
final class TabListUpdater {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final NinjaMinecraft plugin;
    private BukkitTask task;
    private String headerFormat = "";
    private String footerFormat = "";

    TabListUpdater(NinjaMinecraft plugin) {
        this.plugin = plugin;
    }

    void reload(FileConfiguration config) {
        stop();
        if (!config.getBoolean("tablist.enabled", true)) {
            return;
        }
        headerFormat = config.getString("tablist.header", "");
        footerFormat = config.getString("tablist.footer", "");
        long interval = Math.max(1, config.getLong("tablist.update-interval-ticks", 20));
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::update, 1L, interval);
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            }
        }
    }

    private void update() {
        Server server = plugin.getServer();
        if (server.getOnlinePlayers().isEmpty()) {
            return;
        }

        double[] tps = server.getTPS();
        double mspt = server.getAverageTickTime();
        double msptMax = maxTickMillis(server.getTickTimes());

        TagResolver shared = TagResolver.resolver(
                Placeholder.component("tps_1m", tps(tps[0])),
                Placeholder.component("tps_5m", tps(tps[1])),
                Placeholder.component("tps_15m", tps(tps[2])),
                Placeholder.component("mspt", mspt(mspt)),
                Placeholder.component("mspt_max", mspt(msptMax)),
                Placeholder.unparsed("online", Integer.toString(server.getOnlinePlayers().size())),
                Placeholder.unparsed("max_players", Integer.toString(server.getMaxPlayers())));

        for (Player player : server.getOnlinePlayers()) {
            TagResolver resolver = TagResolver.resolver(
                    shared, Placeholder.unparsed("ping", Integer.toString(player.getPing())));
            player.sendPlayerListHeaderAndFooter(
                    MINI.deserialize(headerFormat, resolver),
                    MINI.deserialize(footerFormat, resolver));
        }
    }

    private static double maxTickMillis(long[] tickTimesNanos) {
        long max = 0;
        for (long t : tickTimesNanos) {
            max = Math.max(max, t);
        }
        return max / 1_000_000.0;
    }

    private static Component tps(double value) {
        double shown = Math.min(20.0, value);
        NamedTextColor color = shown >= 19.5 ? NamedTextColor.GREEN
                : shown >= 17.0 ? NamedTextColor.YELLOW
                : NamedTextColor.RED;
        return Component.text(String.format(Locale.ROOT, "%.1f", shown), color);
    }

    private static Component mspt(double value) {
        // 50 ms is the full budget for one tick at 20 TPS.
        NamedTextColor color = value < 40.0 ? NamedTextColor.GREEN
                : value < 50.0 ? NamedTextColor.YELLOW
                : NamedTextColor.RED;
        return Component.text(String.format(Locale.ROOT, "%.1f", value), color);
    }
}
