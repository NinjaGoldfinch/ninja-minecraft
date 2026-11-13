package nz.ninjagoldfinch.minecraft;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * A sidebar scoreboard per player. Each line is a score entry with a custom display name and
 * a blank number format, so no red score numbers are shown and lines can repeat (e.g. blanks).
 */
final class ScoreboardDisplay {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int MAX_LINES = 15; // the sidebar only shows 15 entries

    private final Map<UUID, Board> boards = new HashMap<>();
    private boolean enabled;
    private String title = "";
    private List<String> lines = List.of();

    void reload(FileConfiguration config) {
        enabled = config.getBoolean("scoreboard.enabled", true);
        title = config.getString("scoreboard.title", "");
        List<String> configured = config.getStringList("scoreboard.lines");
        lines = List.copyOf(configured.subList(0, Math.min(MAX_LINES, configured.size())));
    }

    boolean enabled() {
        return enabled;
    }

    void show(Player player, TagResolver placeholders) {
        Board board = boards.computeIfAbsent(player.getUniqueId(), id -> createBoard(player));
        board.update(MINI.deserialize(title, placeholders),
                lines.stream().map(line -> MINI.deserialize(line, placeholders)).toList());
    }

    void hide(Player player) {
        Board board = boards.remove(player.getUniqueId());
        // Only hand back the main scoreboard if nothing else has replaced ours since.
        if (board != null && player.getScoreboard() == board.scoreboard) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    /** Drops a player's board without touching the player (they're leaving). */
    void forget(UUID playerId) {
        boards.remove(playerId);
    }

    private Board createBoard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("ninjaminecraft", Criteria.DUMMY, Component.empty());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());
        for (int i = 0; i < lines.size(); i++) {
            // Higher score = higher up the sidebar, so line 0 gets the biggest score.
            objective.getScore(entry(i)).setScore(lines.size() - i);
        }
        player.setScoreboard(scoreboard);
        return new Board(scoreboard, objective, new Component[lines.size()]);
    }

    private static String entry(int line) {
        return "line" + line;
    }

    private static final class Board {
        final Scoreboard scoreboard;
        final Objective objective;
        final Component[] shown;
        Component shownTitle;

        Board(Scoreboard scoreboard, Objective objective, Component[] shown) {
            this.scoreboard = scoreboard;
            this.objective = objective;
            this.shown = shown;
        }

        /** Sends only what changed since the last refresh. */
        void update(Component title, List<Component> lines) {
            if (!Objects.equals(title, shownTitle)) {
                objective.displayName(title);
                shownTitle = title;
            }
            for (int i = 0; i < shown.length && i < lines.size(); i++) {
                Component line = lines.get(i);
                if (!Objects.equals(line, shown[i])) {
                    objective.getScore(entry(i)).customName(line);
                    shown[i] = line;
                }
            }
        }
    }
}
