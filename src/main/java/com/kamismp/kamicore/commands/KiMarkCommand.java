package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import com.kamismp.kamicore.util.Suggest;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * /kimark set [name]  - save where you stand as a ki mark
 * /kimark del [name]  - delete a ki mark
 * /kimark list        - show your ki marks
 */
public final class KiMarkCommand implements BasicCommand {

    private static final String NAME_PATTERN = "[a-z0-9_-]{1,16}";

    private final KamiCore plugin;

    public KiMarkCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            Msg.playersOnly(source.getSender());
            return;
        }
        if (args.length == 0) {
            sendUsage(player);
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> set(player, args);
            case "del", "delete", "remove" -> delete(player, args);
            case "list" -> list(player);
            default -> sendUsage(player);
        }
    }

    private void set(Player player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "<red>Usage: /kimark set [name]");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        if (!name.matches(NAME_PATTERN)) {
            Msg.send(player, "<red>Names can only use letters, numbers, _ and - (max 16).");
            return;
        }

        Set<String> marks = plugin.data().getMarkNames(player.getUniqueId());
        int max = plugin.getConfig().getInt("kimark.max-marks", 3);
        boolean overwriting = marks.contains(name);
        if (!overwriting && marks.size() >= max && !player.hasPermission("kamicore.bypass.marklimit")) {
            Msg.send(player, "<red>You can only hold <yellow>" + max + "</yellow> ki marks. Delete one with <yellow>/kimark del [name]</yellow>.");
            return;
        }

        Location loc = player.getLocation();
        plugin.data().setMark(player.getUniqueId(), name, loc);
        Msg.send(player, "<aqua>Ki mark <yellow>" + name + "</yellow> " + (overwriting ? "updated" : "saved")
                + "<aqua>. Use <yellow>/shunkan " + name + "</yellow> to return here.");
    }

    private void delete(Player player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "<red>Usage: /kimark del [name]");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        if (!plugin.data().hasMark(player.getUniqueId(), name)) {
            Msg.send(player, "<red>You have no ki mark called <yellow>" + name + "</yellow>.");
            return;
        }
        plugin.data().deleteMark(player.getUniqueId(), name);
        Msg.send(player, "<gray>Ki mark <yellow>" + name + "</yellow> faded away.");
    }

    private void list(Player player) {
        Set<String> marks = plugin.data().getMarkNames(player.getUniqueId());
        if (marks.isEmpty()) {
            Msg.send(player, "<gray>You have no ki marks yet. Try <yellow>/kimark set base</yellow>.");
            return;
        }
        int max = plugin.getConfig().getInt("kimark.max-marks", 3);
        Msg.send(player, "<aqua>Your ki marks <gray>(" + marks.size() + "/" + max + ")<aqua>:");
        for (String name : marks) {
            Location loc = plugin.data().getMark(player.getUniqueId(), name);
            String where = loc == null
                    ? "<red>world missing"
                    : "<gray>" + loc.getWorld().getName() + " " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
            Msg.raw(player, " <dark_gray>-</dark_gray> <click:run_command:'/shunkan " + name + "'><hover:show_text:'Click to teleport'><yellow>"
                    + name + "</yellow></hover></click> " + where);
        }
    }

    private void sendUsage(Player player) {
        Msg.send(player, "<gold>Ki marks:</gold> <yellow>/kimark set [name]</yellow><gray>,</gray> <yellow>/kimark del [name]</yellow><gray>,</gray> <yellow>/kimark list</yellow>");
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        int index = Suggest.index(args);
        String typed = Suggest.current(args);
        if (index == 0) {
            return Suggest.filter(List.of("set", "del", "list"), typed);
        }
        if (index == 1 && source.getSender() instanceof Player player
                && (args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("set"))) {
            return Suggest.filter(plugin.data().getMarkNames(player.getUniqueId()), typed);
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "kamicore.kimark";
    }
}
