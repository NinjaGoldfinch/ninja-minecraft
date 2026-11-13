package nz.ninjagoldfinch.minecraft;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ClockTimeSkipEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * Sleep messages on top of vanilla sleeping.
 *
 * <p>The night skip itself is left entirely to vanilla: this only sets the
 * {@code players_sleeping_percentage} gamerule, so vanilla still decides when to skip, waits for
 * players to be fully asleep, wakes everyone, clears rain and resets phantom timers. The plugin
 * just watches and announces who is sleeping, when the skip is coming, and when it happened.
 */
final class SleepManager implements Listener {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final NinjaMinecraft plugin;
    /** Worlds where the "skipping" message has been shown and the skip hasn't happened yet. */
    private final Set<UUID> skipAnnounced = new HashSet<>();

    private boolean manageGamerule;
    private int percentage;
    private boolean messagesEnabled;
    private String sleeping = "";
    private String skippingNight = "";
    private String skippingStorm = "";
    private String skippedNight = "";
    private String skippedStorm = "";
    private String cancelled = "";

    SleepManager(NinjaMinecraft plugin) {
        this.plugin = plugin;
    }

    void reload(FileConfiguration config) {
        manageGamerule = config.getBoolean("sleep.manage-gamerule");
        percentage = Math.clamp(config.getInt("sleep.percentage"), 0, 100);
        messagesEnabled = config.getBoolean("sleep.messages.enabled");
        sleeping = string(config, "sleep.messages.sleeping");
        skippingNight = string(config, "sleep.messages.skipping-night");
        skippingStorm = string(config, "sleep.messages.skipping-storm");
        skippedNight = string(config, "sleep.messages.skipped-night");
        skippedStorm = string(config, "sleep.messages.skipped-storm");
        cancelled = string(config, "sleep.messages.cancelled");
        skipAnnounced.clear();

        if (manageGamerule) {
            plugin.getServer().getWorlds().forEach(this::applyGamerule);
        }
    }

    int percentage() {
        return percentage;
    }

    boolean managesGamerule() {
        return manageGamerule;
    }

    boolean messagesEnabled() {
        return messagesEnabled;
    }

    private void applyGamerule(World world) {
        world.setGameRule(GameRules.PLAYERS_SLEEPING_PERCENTAGE, percentage);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (manageGamerule) {
            applyGamerule(event.getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        Player player = event.getPlayer();
        // The player is only marked as sleeping after the event, so count on the next tick.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!messagesEnabled || !player.isOnline() || !player.isSleeping()) {
                return;
            }
            World world = player.getWorld();
            Count count = count(world);
            broadcast(sleeping, Placeholder.unparsed("player", player.getName()), count.placeholders(world));
            if (count.enough() && skipAnnounced.add(world.getUID())) {
                broadcast(isNight(world) ? skippingNight : skippingStorm, count.placeholders(world));
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedLeave(PlayerBedLeaveEvent event) {
        recheckLater(event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        recheckLater(event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        recheckLater(event.getFrom());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        recheckLater(event.getPlayer().getWorld());
    }

    /** Fired by the server just before vanilla wakes everyone up after a successful night skip. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTimeSkip(TimeSkipEvent event) {
        if (event.getSkipReason() != ClockTimeSkipEvent.SkipReason.NIGHT_SKIP) {
            return;
        }
        World world = event.getWorld();
        skipAnnounced.remove(world.getUID());
        if (!messagesEnabled) {
            return;
        }
        // Everyone who slept is still in bed at this point.
        List<String> sleepers = world.getPlayers().stream()
                .filter(Player::isSleeping)
                .map(Player::getName)
                .toList();
        broadcast(isNight(world) ? skippedNight : skippedStorm,
                Placeholder.unparsed("players", joinNames(sleepers)),
                count(world).placeholders(world));
    }

    /**
     * After someone leaves bed / the world / the game, check on the next tick whether an
     * announced skip can no longer happen.
     */
    private void recheckLater(World world) {
        if (!skipAnnounced.contains(world.getUID())) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Still announced means the skip didn't happen (a skip clears it before waking players).
            if (!skipAnnounced.contains(world.getUID())) {
                return;
            }
            Count count = count(world);
            if (!count.enough()) {
                skipAnnounced.remove(world.getUID());
                if (messagesEnabled) {
                    broadcast(cancelled, count.placeholders(world));
                }
            }
        });
    }

    /**
     * Counts the same way the server does: spectators don't count; players marked as
     * sleeping-ignored (e.g. AFK or vanished by another plugin) count as asleep; at least one
     * sleeper is always needed.
     */
    private static Count count(World world) {
        int active = 0;
        int asleep = 0;
        for (Player player : world.getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            active++;
            if (player.isSleeping() || player.isSleepingIgnored()) {
                asleep++;
            }
        }
        int percent = Objects.requireNonNullElse(world.getGameRuleValue(GameRules.PLAYERS_SLEEPING_PERCENTAGE), 100);
        int needed = Math.max(1, (int) Math.ceil(active * percent / 100.0));
        return new Count(asleep, needed);
    }

    /**
     * Whether a skip now counts as skipping the night (any time after sunset, 18:00) rather than
     * sleeping through a daytime thunderstorm.
     */
    private static boolean isNight(World world) {
        return Math.floorMod(world.getTime(), 24_000L) >= 12_000;
    }

    private void broadcast(String format, TagResolver... placeholders) {
        if (format.isEmpty()) {
            return;
        }
        Component message = MINI.deserialize(format, TagResolver.resolver(placeholders));
        plugin.getServer().broadcast(message);
    }

    private static String joinNames(List<String> names) {
        if (names.isEmpty()) {
            return "nobody";
        }
        if (names.size() == 1) {
            return names.getFirst();
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.getLast();
    }

    private static String string(FileConfiguration config, String path) {
        return Objects.requireNonNullElse(config.getString(path), "");
    }

    private record Count(int sleeping, int needed) {
        boolean enough() {
            return sleeping >= needed;
        }

        TagResolver placeholders(World world) {
            return TagResolver.resolver(
                    Placeholder.unparsed("sleeping", Integer.toString(sleeping)),
                    Placeholder.unparsed("needed", Integer.toString(needed)),
                    Placeholder.unparsed("world", world.getName()));
        }
    }
}
