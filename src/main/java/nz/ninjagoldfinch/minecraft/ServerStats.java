package nz.ninjagoldfinch.minecraft;

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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

    private static final int BAR_SEGMENTS = 20;
    private static final TextColor NIGHT_BLUE = TextColor.color(0x8FA8FF);

    static ServerStats capture(Server server) {
        double[] tps = server.getTPS();
        double tps1m = Math.min(20.0, tps[0]);
        double mspt = server.getAverageTickTime();
        List<World> worlds = server.getWorlds();
        // Day, time and weather follow the main (first-loaded) world.
        World world = worlds.isEmpty() ? null : worlds.getFirst();

        TagResolver.Builder builder = TagResolver.builder().resolvers(
                Placeholder.component("tps_1m", tps(tps[0])),
                Placeholder.component("tps_5m", tps(tps[1])),
                Placeholder.component("tps_15m", tps(tps[2])),
                Placeholder.component("tps_bar", bar(tps1m / 20.0, tpsColor(tps1m))),
                Placeholder.component("mspt", mspt(mspt)),
                Placeholder.component("mspt_max", mspt(maxTickMillis(server.getTickTimes()))),
                // How much of the 50 ms tick budget is used.
                Placeholder.component("mspt_bar", bar(mspt / 50.0, msptColor(mspt))),
                Placeholder.unparsed("online", Integer.toString(server.getOnlinePlayers().size())),
                Placeholder.unparsed("max_players", Integer.toString(server.getMaxPlayers())));

        if (world == null) {
            builder.resolvers(
                    Placeholder.unparsed("day", "?"),
                    Placeholder.unparsed("time", "?"),
                    Placeholder.unparsed("weather", "?"),
                    Placeholder.unparsed("time_icon", ""),
                    Placeholder.unparsed("weather_icon", ""));
        } else {
            builder.resolvers(
                    Placeholder.unparsed("day", Long.toString(day(world.getFullTime()))),
                    Placeholder.unparsed("time", clock(world.getTime())),
                    Placeholder.unparsed("weather", weatherName(world)),
                    Placeholder.component("time_icon", world.isDayTime()
                            ? Component.text("☀", NamedTextColor.YELLOW)
                            : Component.text("☽", NIGHT_BLUE)),
                    Placeholder.component("weather_icon", weatherIcon(world)));
        }
        return new ServerStats(builder.build());
    }

    TagResolver resolver(Player viewer) {
        return TagResolver.resolver(shared, Placeholder.component("ping", ping(viewer.getPing())));
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

    /** A row of {@value #BAR_SEGMENTS} thin bars, the first {@code fraction} of them lit. */
    static Component bar(double fraction, TextColor color) {
        int lit = (int) Math.round(Math.clamp(fraction, 0.0, 1.0) * BAR_SEGMENTS);
        return Component.text()
                .append(Component.text("|".repeat(lit), color))
                .append(Component.text("|".repeat(BAR_SEGMENTS - lit), NamedTextColor.DARK_GRAY))
                .build();
    }

    private static String weatherName(World world) {
        if (world.isThundering()) {
            return "Thunder";
        }
        return world.hasStorm() ? "Rain" : "Clear";
    }

    private static Component weatherIcon(World world) {
        if (world.isThundering()) {
            return Component.text("⚡", NamedTextColor.GOLD);
        }
        return world.hasStorm()
                ? Component.text("☂", NamedTextColor.AQUA)
                : Component.text("☀", NamedTextColor.YELLOW);
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
        return Component.text(String.format(Locale.ROOT, "%.1f", shown), tpsColor(shown));
    }

    private static NamedTextColor tpsColor(double tps) {
        return tps >= 19.5 ? NamedTextColor.GREEN
                : tps >= 17.0 ? NamedTextColor.YELLOW
                : NamedTextColor.RED;
    }

    private static Component mspt(double value) {
        return Component.text(String.format(Locale.ROOT, "%.1f", value), msptColor(value));
    }

    /** 50 ms is the full budget for one tick at 20 TPS. */
    private static NamedTextColor msptColor(double mspt) {
        return mspt < 40.0 ? NamedTextColor.GREEN
                : mspt < 50.0 ? NamedTextColor.YELLOW
                : NamedTextColor.RED;
    }

    private static Component ping(int ms) {
        NamedTextColor color = ms < 80 ? NamedTextColor.GREEN
                : ms < 200 ? NamedTextColor.YELLOW
                : NamedTextColor.RED;
        return Component.text(Integer.toString(ms), color);
    }
}
