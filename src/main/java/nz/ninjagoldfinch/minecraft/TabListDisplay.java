package nz.ninjagoldfinch.minecraft;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/** Writes the configured header and footer into a player's tab list. */
final class TabListDisplay {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private boolean enabled;
    private String header = "";
    private String footer = "";

    void reload(FileConfiguration config) {
        enabled = config.getBoolean("tablist.enabled");
        header = java.util.Objects.requireNonNullElse(config.getString("tablist.header"), "");
        footer = java.util.Objects.requireNonNullElse(config.getString("tablist.footer"), "");
    }

    boolean enabled() {
        return enabled;
    }

    void show(Player player, TagResolver placeholders) {
        player.sendPlayerListHeaderAndFooter(
                MINI.deserialize(header, placeholders),
                MINI.deserialize(footer, placeholders));
    }

    void hide(Player player) {
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
    }
}
