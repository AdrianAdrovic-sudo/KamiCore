package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import com.kamismp.kamicore.util.Suggest;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * /kisense <player> - sense someone's ki and ask to Instant Transmission to them.
 * They answer with /kiaccept or /kideny (or click the buttons in chat).
 */
public final class KiSenseCommand implements BasicCommand {

    private static final String COOLDOWN_KEY = "kisense";

    private final KamiCore plugin;

    public KiSenseCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            Msg.playersOnly(source.getSender());
            return;
        }
        if (args.length != 1) {
            Msg.send(player, "<red>Usage: /kisense [player]");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            Msg.send(player, "<red>You can't sense anyone called <yellow>" + args[0] + "</yellow> right now.");
            return;
        }
        if (target.equals(player)) {
            Msg.send(player, "<red>You can't sense your own ki... you're already here!");
            return;
        }
        if (plugin.teleportRequests().has(player.getUniqueId(), target.getUniqueId())) {
            Msg.send(player, "<yellow>" + target.getName() + "</yellow> <gray>already has a request from you.");
            return;
        }
        if (!plugin.cooldowns().checkReady(player, COOLDOWN_KEY, "Ki sense")) {
            return;
        }

        plugin.teleportRequests().add(player.getUniqueId(), target.getUniqueId());
        plugin.cooldowns().start(player, COOLDOWN_KEY, plugin.getConfig().getLong("kisense.cooldown", 30));
        long timeout = plugin.getConfig().getLong("kisense.request-timeout", 60);

        Msg.send(player, "<aqua>You lock onto <yellow>" + target.getName() + "</yellow>'s ki...</aqua> <gray>Request sent (expires in " + timeout + "s).");

        String name = player.getName();
        Msg.send(target, "<yellow>" + name + "</yellow> <aqua>is sensing your ki and wants to Instant Transmission to you.");
        Msg.raw(target, "   <click:run_command:'/kiaccept " + name + "'><hover:show_text:'Let them come'><green><b>[ACCEPT]</b></green></hover></click>"
                + "   <click:run_command:'/kideny " + name + "'><hover:show_text:'Block them'><red><b>[DENY]</b></red></hover></click>");
        target.playSound(target.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.8f);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (Suggest.index(args) != 0) {
            return List.of();
        }
        List<String> names = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(source.getSender()))
                .map(Player::getName)
                .toList();
        return Suggest.filter(names, Suggest.current(args));
    }

    @Override
    public String permission() {
        return "kamicore.kisense";
    }
}
