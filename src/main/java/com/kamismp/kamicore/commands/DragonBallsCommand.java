package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.dragonballs.DragonBalls;
import com.kamismp.kamicore.util.Cooldowns;
import com.kamismp.kamicore.util.Msg;
import com.kamismp.kamicore.util.Suggest;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Admin tools for the Dragon Balls:
 * /dragonballs status  - where every ball is
 * /dragonballs scatter - start a new cycle now (old balls turn to stone)
 * /dragonballs give    - get all 7 balls yourself (for testing the wishes)
 */
public final class DragonBallsCommand implements BasicCommand {

    private final KamiCore plugin;

    public DragonBallsCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        DragonBalls balls = plugin.dragonBalls();
        String action = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);

        switch (action) {
            case "status" -> {
                if (balls.isInert()) {
                    Msg.send(sender, "<gray>The Dragon Balls are stone. They return in <yellow>"
                            + Cooldowns.format(Math.max(0, balls.returnsAt() - System.currentTimeMillis())) + "</yellow>.");
                    return;
                }
                Msg.send(sender, "<gold><b>Dragon Balls</b></gold>");
                balls.statusLines().forEach(line -> Msg.raw(sender, line));
            }
            case "scatter" -> {
                balls.scatterAll(true);
                Msg.send(sender, "<green>The Dragon Balls have been scattered again. Old balls are now worthless.");
            }
            case "give" -> {
                if (!(sender instanceof Player player)) {
                    Msg.playersOnly(sender);
                    return;
                }
                balls.giveAll(player);
                Msg.send(sender, "<green>You received all 7 Dragon Balls. Try <yellow>/shenron</yellow>.");
            }
            default -> Msg.send(sender, "<gray>Usage: <yellow>/dragonballs status|scatter|give");
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (Suggest.index(args) == 0) {
            return Suggest.filter(List.of("status", "scatter", "give"), Suggest.current(args));
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "kamicore.admin";
    }
}
