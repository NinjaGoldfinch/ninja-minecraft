package nz.ninjagoldfinch.minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;

/** Grants the vanilla /difficulty permission to every player while enabled in config. */
final class DifficultyAccess implements Listener {

    static final String PERMISSION = "minecraft.command.difficulty";

    private final NinjaMinecraft plugin;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private boolean enabled;

    DifficultyAccess(NinjaMinecraft plugin) {
        this.plugin = plugin;
    }

    void reload(FileConfiguration config) {
        enabled = config.getBoolean("difficulty.allow-everyone", true);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (enabled) {
                grant(player);
            } else {
                revoke(player);
            }
        }
    }

    void revokeAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            revoke(player);
        }
        attachments.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (enabled) {
            grant(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Attachments die with the Player object; just drop our reference.
        attachments.remove(event.getPlayer().getUniqueId());
    }

    private void grant(Player player) {
        if (attachments.containsKey(player.getUniqueId())) {
            return;
        }
        attachments.put(player.getUniqueId(), player.addAttachment(plugin, PERMISSION, true));
        // Resend the command tree so the client shows /difficulty in tab-complete.
        player.updateCommands();
    }

    private void revoke(Player player) {
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            player.removeAttachment(attachment);
            player.updateCommands();
        }
    }
}
