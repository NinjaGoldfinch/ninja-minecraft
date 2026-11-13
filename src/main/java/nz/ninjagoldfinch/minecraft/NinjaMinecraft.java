package nz.ninjagoldfinch.minecraft;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

    private static final String RELOAD_PERMISSION = "ninjaminecraft.reload";
    private static final String SLEEP_ADMIN_PERMISSION = "ninjaminecraft.sleep.admin";

    private DisplayManager displays;
    private DifficultyAccess difficultyAccess;
    private SleepManager sleep;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        displays = new DisplayManager(this);
        difficultyAccess = new DifficultyAccess(this);
        sleep = new SleepManager(this);
        getServer().getPluginManager().registerEvents(displays, this);
        getServer().getPluginManager().registerEvents(difficultyAccess, this);
        getServer().getPluginManager().registerEvents(sleep, this);

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
        sleep.reload(getConfig());
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(adminCommand(), "NinjaMinecraft admin commands");
            event.registrar().register(subscribeCommand(),
                    "Turn the tab list and scoreboard displays on or off for yourself", List.of("sub"));
        });
    }

    /**
     * /ninjaminecraft reload
     * /ninjaminecraft sleep                          - show sleep settings
     * /ninjaminecraft sleep percentage <0-100>
     * /ninjaminecraft sleep messages <on|off>
     */
    private com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> adminCommand() {
        return Commands.literal("ninjaminecraft")
                .requires(source -> source.getSender().hasPermission(RELOAD_PERMISSION)
                        || source.getSender().hasPermission(SLEEP_ADMIN_PERMISSION))
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission(RELOAD_PERMISSION))
                        .executes(ctx -> {
                            reloadConfig();
                            applyConfig();
                            ctx.getSource().getSender().sendMessage(
                                    Component.text("NinjaMinecraft config reloaded.", NamedTextColor.GREEN));
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("sleep")
                        .requires(source -> source.getSender().hasPermission(SLEEP_ADMIN_PERMISSION))
                        .executes(ctx -> {
                            sendSleepStatus(ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.literal("percentage")
                                .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> {
                                            setSleepSetting("sleep.percentage",
                                                    IntegerArgumentType.getInteger(ctx, "percent"));
                                            sendSleepStatus(ctx.getSource().getSender());
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .then(Commands.literal("messages")
                                .then(Commands.literal("on").executes(ctx -> {
                                    setSleepSetting("sleep.messages.enabled", true);
                                    sendSleepStatus(ctx.getSource().getSender());
                                    return Command.SINGLE_SUCCESS;
                                }))
                                .then(Commands.literal("off").executes(ctx -> {
                                    setSleepSetting("sleep.messages.enabled", false);
                                    sendSleepStatus(ctx.getSource().getSender());
                                    return Command.SINGLE_SUCCESS;
                                }))))
                .build();
    }

    /** Saves one sleep setting to config.yml and applies it straight away. */
    private void setSleepSetting(String path, Object value) {
        getConfig().set(path, value);
        saveConfig();
        sleep.reload(getConfig());
    }

    private void sendSleepStatus(org.bukkit.command.CommandSender sender) {
        int percent = sleep.percentage();
        String meaning = percent == 0 ? "one player is enough" : percent + "% of players must sleep";
        sender.sendMessage(Component.text("Sleep settings", NamedTextColor.GOLD));
        sender.sendMessage(Component.text(" Percentage: ", NamedTextColor.GRAY)
                .append(Component.text(percent + "%", NamedTextColor.WHITE))
                .append(Component.text(" (" + meaning + ")", NamedTextColor.DARK_GRAY)));
        if (!sleep.managesGamerule()) {
            sender.sendMessage(Component.text(
                    " manage-gamerule is off, so worlds use their own players_sleeping_percentage gamerule.",
                    NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text(" Messages: ", NamedTextColor.GRAY)
                .append(sleep.messagesEnabled()
                        ? Component.text("on", NamedTextColor.GREEN)
                        : Component.text("off", NamedTextColor.RED)));
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
