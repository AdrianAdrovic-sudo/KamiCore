package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Effects;
import com.kamismp.kamicore.util.Msg;
import com.kamismp.kamicore.util.Suggest;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * One class for both /kiaccept and /kideny.
 * /kiaccept [player] - let them teleport to you (newest request if no name given)
 * /kideny [player]   - refuse
 */
public final class KiResponseCommand implements BasicCommand {

    private final KamiCore plugin;
    private final boolean accept;

    public KiResponseCommand(KamiCore plugin, boolean accept) {
        this.plugin = plugin;
        this.accept = accept;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player target)) {
            Msg.playersOnly(source.getSender());
            return;
        }

        Player requester;
        if (args.length >= 1) {
            requester = Bukkit.getPlayerExact(args[0]);
            if (requester == null || !plugin.teleportRequests().has(requester.getUniqueId(), target.getUniqueId())) {
                Msg.send(target, "<red>You have no active request from <yellow>" + args[0] + "</yellow>.");
                return;
            }
        } else {
            UUID newest = plugin.teleportRequests().newest(target.getUniqueId());
            requester = newest == null ? null : Bukkit.getPlayer(newest);
            if (requester == null) {
                Msg.send(target, "<red>Nobody is sensing your ki right now.");
                return;
            }
        }

        plugin.teleportRequests().remove(requester.getUniqueId(), target.getUniqueId());

        if (accept) {
            Msg.send(target, "<green>You let <yellow>" + requester.getName() + "</yellow> lock onto your ki.");
            Msg.send(requester, "<yellow>" + target.getName() + "</yellow> <green>accepted.</green> <aqua>Instant Transmission!");
            Effects.instantTransmission(requester, target.getLocation());
        } else {
            Msg.send(target, "<gray>You hid your ki from <yellow>" + requester.getName() + "</yellow>.");
            Msg.send(requester, "<yellow>" + target.getName() + "</yellow> <red>hid their ki. Request denied.");
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (Suggest.index(args) != 0 || !(source.getSender() instanceof Player target)) {
            return List.of();
        }
        List<String> names = Bukkit.getOnlinePlayers().stream()
                .filter(p -> plugin.teleportRequests().has(p.getUniqueId(), target.getUniqueId()))
                .map(Player::getName)
                .toList();
        return Suggest.filter(names, Suggest.current(args));
    }

    @Override
    public String permission() {
        return "kamicore.kisense";
    }
}
