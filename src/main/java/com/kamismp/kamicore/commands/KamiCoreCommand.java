package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import com.kamismp.kamicore.util.Suggest;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * /kamicore        - list all abilities
 * /kamicore reload - reload config.yml (admins only)
 */
public final class KamiCoreCommand implements BasicCommand {

    private final KamiCore plugin;

    public KamiCoreCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();

        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("kamicore.admin")) {
                Msg.send(sender, "<red>Only the Kais can do that.");
                return;
            }
            plugin.reloadEverything();
            Msg.send(sender, "<green>KamiCore config reloaded.");
            return;
        }

        String version = plugin.getPluginMeta().getVersion();
        Msg.raw(sender, "<gradient:#ff8c00:#ffd700><b>KamiCore</b></gradient> <gray>v" + version + " - Dragon Ball powers for KamiSMP");
        line(sender, "/kimark set|del|list [name]", "save places as ki marks");
        line(sender, "/shunkan [mark]", "Instant Transmission to a ki mark");
        line(sender, "/shunkan [x] [y] [z]", "Instant Transmission to coordinates");
        line(sender, "/kisense [player]", "ask to teleport to a player");
        line(sender, "/kiaccept, /kideny", "answer a ki sense request");
        line(sender, "/nimbus", "ride the Flying Nimbus (toggle)");
        line(sender, "/senzu", "full heal + food (long cooldown)");
        line(sender, "/gravity", "gravity chamber training for XP");
        line(sender, "/kaioken", "huge power boost (1 day cooldown)");
        if (sender.hasPermission("kamicore.admin")) {
            line(sender, "/kamicore reload", "reload config.yml");
        }
    }

    private void line(CommandSender sender, String command, String description) {
        Msg.raw(sender, " <yellow>" + command + "</yellow> <dark_gray>-</dark_gray> <gray>" + description);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (Suggest.index(args) == 0 && source.getSender().hasPermission("kamicore.admin")) {
            return Suggest.filter(List.of("reload"), Suggest.current(args));
        }
        return List.of();
    }
}
