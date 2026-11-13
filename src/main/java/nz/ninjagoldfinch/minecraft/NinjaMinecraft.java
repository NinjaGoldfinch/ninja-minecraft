package nz.ninjagoldfinch.minecraft;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class NinjaMinecraft extends JavaPlugin {

    private TabListUpdater tabListUpdater;
    private DifficultyAccess difficultyAccess;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        tabListUpdater = new TabListUpdater(this);
        difficultyAccess = new DifficultyAccess(this);
        getServer().getPluginManager().registerEvents(difficultyAccess, this);

        applyConfig();
        registerCommands();
    }

    @Override
    public void onDisable() {
        if (tabListUpdater != null) {
            tabListUpdater.stop();
        }
        if (difficultyAccess != null) {
            difficultyAccess.revokeAll();
        }
    }

    private void applyConfig() {
        tabListUpdater.reload(getConfig());
        difficultyAccess.reload(getConfig());
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        Commands.literal("ninjaminecraft")
                                .requires(source -> source.getSender().hasPermission("ninjaminecraft.reload"))
                                .then(Commands.literal("reload").executes(ctx -> {
                                    reloadConfig();
                                    applyConfig();
                                    ctx.getSource().getSender().sendMessage(
                                            Component.text("NinjaMinecraft config reloaded.", NamedTextColor.GREEN));
                                    return 1;
                                }))
                                .build(),
                        "NinjaMinecraft admin commands"));
    }
}
