package nz.ninjagoldfinch.minecraft;

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * One snapshot of server stats, taken once per refresh and shared by every display.
 * {@link #resolver(Player)} turns it into MiniMessage placeholders for a given viewer.
 */
record ServerStats(TagResolver shared) {

    static ServerStats capture(Server server) {
        double[] tps = server.getTPS();
        List<World> worlds = server.getWorlds();
        // Day, time and weather follow the main (first-loaded) world.
        World world = worlds.isEmpty() ? null : worlds.getFirst();

        return new ServerStats(TagResolver.resolver(
                Placeholder.component("tps_1m", tps(tps[0])),
                Placeholder.component("tps_5m", tps(tps[1])),
                Placeholder.component("tps_15m", tps(tps[2])),
                Placeholder.component("mspt", mspt(server.getAverageTickTime())),
                Placeholder.component("mspt_max", mspt(maxTickMillis(server.getTickTimes()))),
                Placeholder.unparsed("online", Integer.toString(server.getOnlinePlayers().size())),
                Placeholder.unparsed("max_players", Integer.toString(server.getMaxPlayers())),
                Placeholder.unparsed("day", world == null ? "?" : Long.toString(day(world.getFullTime()))),
                Placeholder.unparsed("time", world == null ? "?" : clock(world.getTime())),
                Placeholder.unparsed("weather", world == null ? "?" : weather(world))));
    }

    TagResolver resolver(Player viewer) {
        return TagResolver.resolver(shared, Placeholder.unparsed("ping", Integer.toString(viewer.getPing())));
    }

    /** Same numbering as vanilla {@code /time query day}: the world starts on day 0. */
    static long day(long fullTime) {
        return fullTime / 24_000L;
    }

    /** In-game clock as HH:MM. Tick 0 of a day is 06:00. */
    static String clock(long dayTime) {
        long ticks = Math.floorMod(dayTime, 24_000L);
        long hours = (ticks / 1000 + 6) % 24;
        long minutes = (ticks % 1000) * 60 / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", hours, minutes);
    }

    private static String weather(World world) {
        if (world.isThundering()) {
            return "Thunder";
        }
        return world.hasStorm() ? "Rain" : "Clear";
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
