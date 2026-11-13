package nz.ninjagoldfinch.minecraft;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class NinjaMinecraft extends JavaPlugin {

    private DisplayManager displays;
    private DifficultyAccess difficultyAccess;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        displays = new DisplayManager(this);
        difficultyAccess = new DifficultyAccess(this);
        getServer().getPluginManager().registerEvents(displays, this);
        getServer().getPluginManager().registerEvents(difficultyAccess, this);

        applyConfig();
        registerCommands();
    }

    @Override
    public void onDisable() {
        if (displays != null) {
            displays.stop();
        }
        if (difficultyAccess != null) {
            difficultyAccess.revokeAll();
        }
    }

    private void applyConfig() {
        displays.reload(getConfig());
        difficultyAccess.reload(getConfig());
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(reloadCommand(), "NinjaMinecraft admin commands");
            event.registrar().register(subscribeCommand(),
                    "Turn the tab list and scoreboard displays on or off for yourself", List.of("sub"));
        });
    }

    private com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> reloadCommand() {
        return Commands.literal("ninjaminecraft")
                .requires(source -> source.getSender().hasPermission("ninjaminecraft.reload"))
                .then(Commands.literal("reload").executes(ctx -> {
                    reloadConfig();
                    applyConfig();
                    ctx.getSource().getSender().sendMessage(
                            Component.text("NinjaMinecraft config reloaded.", NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                }))
                .build();
    }

    /**
     * /subscribe                          - show what you're subscribed to
     * /subscribe <tablist|scoreboard>     - toggle it
     * /subscribe <tablist|scoreboard> <on|off>
     */
    private com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> subscribeCommand() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("subscribe")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission("ninjaminecraft.subscribe"))
                .executes(ctx -> {
                    Player player = (Player) ctx.getSource().getSender();
                    for (Display display : Display.values()) {
                        player.sendMessage(statusLine(player, display));
                    }
                    player.sendMessage(Component.text(
                            "Use /subscribe <tablist|scoreboard> [on|off] to change.", NamedTextColor.GRAY));
                    return Command.SINGLE_SUCCESS;
                });

        for (Display display : Display.values()) {
            root.then(Commands.literal(display.id)
                    .executes(ctx -> {
                        Player player = (Player) ctx.getSource().getSender();
                        return setSubscription(player, display, !displays.isSubscribed(player, display));
                    })
                    .then(Commands.literal("on").executes(ctx ->
                            setSubscription((Player) ctx.getSource().getSender(), display, true)))
                    .then(Commands.literal("off").executes(ctx ->
                            setSubscription((Player) ctx.getSource().getSender(), display, false))));
        }
        return root.build();
    }

    private int setSubscription(Player player, Display display, boolean subscribed) {
        if (!displays.isEnabled(display)) {
            player.sendMessage(Component.text(
                    display.label + " is turned off on this server.", NamedTextColor.RED));
            return 0;
        }
        displays.setSubscribed(player, display, subscribed);
        player.sendMessage(statusLine(player, display));
        return Command.SINGLE_SUCCESS;
    }

    private Component statusLine(Player player, Display display) {
        Component state;
        if (!displays.isEnabled(display)) {
            state = Component.text("disabled on this server", NamedTextColor.DARK_GRAY);
        } else if (displays.isSubscribed(player, display)) {
            state = Component.text("on", NamedTextColor.GREEN);
        } else {
            state = Component.text("off", NamedTextColor.RED);
        }
        return Component.text(display.label + ": ", NamedTextColor.GRAY).append(state);
    }
}
