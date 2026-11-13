package nz.ninjagoldfinch.minecraft;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

/**
 * Per-player opt in/out for each {@link Display}. Choices are stored in the player's
 * persistent data, so they survive restarts. Players who never chose get the config default.
 */
final class Subscriptions {

    private final Map<Display, NamespacedKey> keys = new EnumMap<>(Display.class);
    private final Map<Display, Boolean> defaults = new EnumMap<>(Display.class);

    Subscriptions(NinjaMinecraft plugin) {
        for (Display display : Display.values()) {
            keys.put(display, new NamespacedKey(plugin, "subscribed_" + display.id));
        }
    }

    void reload(FileConfiguration config) {
        for (Display display : Display.values()) {
            defaults.put(display, config.getBoolean(display.id + ".default-subscribed", true));
        }
    }

    boolean isSubscribed(Player player, Display display) {
        Boolean stored = player.getPersistentDataContainer().get(keys.get(display), PersistentDataType.BOOLEAN);
        return stored != null ? stored : defaults.getOrDefault(display, true);
    }

    void set(Player player, Display display, boolean subscribed) {
        player.getPersistentDataContainer().set(keys.get(display), PersistentDataType.BOOLEAN, subscribed);
    }
}
