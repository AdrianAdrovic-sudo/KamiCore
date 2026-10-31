package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.duel.Duels;
import com.kamismp.kamicore.util.Msg;
import com.kamismp.kamicore.util.Suggest;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * /duel <player>          - challenge someone
 * /duel accept [player]   - accept (newest challenge if no name)
 * /duel deny [player]     - decline
 * /duel forfeit           - give up the duel you're in
 */
public final class DuelCommand implements BasicCommand {

    private final KamiCore plugin;

    public DuelCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            Msg.playersOnly(source.getSender());
            return;
        }
        Duels duels = plugin.duels();
        if (args.length == 0) {
            Msg.send(player, "<gold>Duels:</gold> <yellow>/duel [player]</yellow><gray>,</gray> <yellow>/duel accept</yellow><gray>,</gray> "
                    + "<yellow>/duel deny</yellow><gray>,</gray> <yellow>/duel forfeit</yellow>");
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "accept" -> duels.accept(player, challenger(player, args));
            case "deny", "decline" -> duels.deny(player, challenger(player, args));
            case "forfeit", "giveup", "leave" -> duels.forfeit(player);
            default -> {
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    Msg.send(player, "<red>No player called <yellow>" + args[0] + "</yellow> is online.");
                    return;
                }
                duels.challenge(player, target);
            }
        }
    }

    private Player challenger(Player target, String[] args) {
        if (args.length >= 2) {
            return Bukkit.getPlayerExact(args[1]);
        }
        return plugin.duels().newestChallenger(target);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (Suggest.index(args) != 0) {
            return List.of();
        }
        List<String> options = new ArrayList<>(List.of("accept", "deny", "forfeit"));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(source.getSender())) {
                options.add(p.getName());
            }
        }
        return Suggest.filter(options, Suggest.current(args));
    }

    @Override
    public String permission() {
        return "kamicore.duel";
    }
}
