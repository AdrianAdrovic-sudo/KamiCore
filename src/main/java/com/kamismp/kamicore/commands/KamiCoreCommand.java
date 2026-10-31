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
        line(sender, "/gravity [10x|50x|100x|300x]", "gravity chamber training for XP");
        line(sender, "/kaioken [x2|x3|x10|x20]", "power boost that costs health");
        line(sender, "/ssj [form|off]", "Super Saiyan forms (drain XP, give flight)");
        line(sender, "/kamehameha, /galickgun, /finalflash", "charge (hold Shift), release to fire");
        line(sender, "/specialbeamcannon, /spiritbomb", "piercing beam / giant energy ball");
        line(sender, "/solarflare", "blind everyone who is looking at you");
        line(sender, "/scouter [player]", "power levels and locations");
        line(sender, "/scouter dragonballs", "Dragon Radar: find the nearest ball");
        line(sender, "/shenron", "summon the dragon with all 7 balls");
        line(sender, "/duel [player]", "fight someone in a walled arena");
        if (sender.hasPermission("kamicore.admin")) {
            line(sender, "/kamicore reload", "reload config.yml");
            line(sender, "/dragonballs status|scatter|give", "Dragon Ball admin tools");
        }
        if (sender.hasPermission("kamicore.kaioshin")) {
            line(sender, "/kaioshin", "list the powers of the Kaioshin");
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
