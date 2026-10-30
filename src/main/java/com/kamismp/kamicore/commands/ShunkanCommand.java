package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Effects;
import com.kamismp.kamicore.util.Msg;
import com.kamismp.kamicore.util.Suggest;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Shunkan Ido = Instant Transmission.
 * /shunkan <mark>      - teleport to one of your ki marks
 * /shunkan <x> <y> <z> - teleport to coordinates (supports ~ like vanilla)
 * /shunkan             - if you only have one ki mark, go there
 */
public final class ShunkanCommand implements BasicCommand {

    private static final String COOLDOWN_KEY = "shunkan";

    private final KamiCore plugin;

    public ShunkanCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            Msg.playersOnly(source.getSender());
            return;
        }

        Location target;
        String description;

        if (args.length == 3) {
            target = parseCoordinates(player, args);
            if (target == null) {
                return; // error message already sent
            }
            description = target.getBlockX() + ", " + target.getBlockY() + ", " + target.getBlockZ();
        } else if (args.length <= 1) {
            String name;
            if (args.length == 1) {
                name = args[0].toLowerCase(Locale.ROOT);
            } else {
                Set<String> marks = plugin.data().getMarkNames(player.getUniqueId());
                if (marks.size() != 1) {
                    sendUsage(player);
                    return;
                }
                name = marks.iterator().next();
            }
            if (!plugin.data().hasMark(player.getUniqueId(), name)) {
                Msg.send(player, "<red>You can't sense a ki mark called <yellow>" + name + "</yellow>. See <yellow>/kimark list</yellow>.");
                return;
            }
            target = plugin.data().getMark(player.getUniqueId(), name);
            if (target == null) {
                Msg.send(player, "<red>The world of that ki mark no longer exists.");
                return;
            }
            description = "ki mark <yellow>" + name + "</yellow>";
        } else {
            sendUsage(player);
            return;
        }

        if (!plugin.cooldowns().checkReady(player, COOLDOWN_KEY, "Instant Transmission")) {
            return;
        }

        Effects.instantTransmission(player, target);
        plugin.cooldowns().start(player, COOLDOWN_KEY, plugin.getConfig().getLong("shunkan.cooldown", 30));
        Msg.send(player, "<aqua>Instant Transmission!</aqua> <gray>You appear at " + description + "<gray>.");
    }

    /** Reads x y z. Supports absolute numbers and ~ / ~5 relative coordinates. */
    private Location parseCoordinates(Player player, String[] args) {
        Location here = player.getLocation();
        World world = here.getWorld();
        double x;
        double y;
        double z;
        try {
            x = parseOne(args[0], here.getX(), true);
            y = parseOne(args[1], here.getY(), false);
            z = parseOne(args[2], here.getZ(), true);
        } catch (NumberFormatException e) {
            Msg.send(player, "<red>Those aren't valid coordinates. Example: <yellow>/shunkan 100 70 -250</yellow>");
            return null;
        }

        if (y < world.getMinHeight() || y > world.getMaxHeight() + 100) {
            Msg.send(player, "<red>Y must be between <yellow>" + world.getMinHeight() + "</yellow> and <yellow>" + (world.getMaxHeight() + 100) + "</yellow>.");
            return null;
        }

        Location target = new Location(world, x, y, z, here.getYaw(), here.getPitch());
        if (!world.getWorldBorder().isInside(target)) {
            Msg.send(player, "<red>That spot is outside the world border.");
            return null;
        }
        return target;
    }

    /** "~" = current, "~5" = current + 5, "100" = 100.5 (block center, like vanilla /tp). */
    private double parseOne(String input, double current, boolean centerBlock) {
        if (input.startsWith("~")) {
            String rest = input.substring(1);
            return current + (rest.isEmpty() ? 0 : Double.parseDouble(rest));
        }
        double value = Double.parseDouble(input);
        if (centerBlock && !input.contains(".")) {
            value += 0.5;
        }
        return value;
    }

    private void sendUsage(Player player) {
        Msg.send(player, "<gold>Instant Transmission:</gold> <yellow>/shunkan [mark]</yellow> <gray>or</gray> <yellow>/shunkan [x] [y] [z]</yellow>");
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        int index = Suggest.index(args);
        String typed = Suggest.current(args);
        if (index == 0) {
            List<String> options = new ArrayList<>(List.of("~"));
            if (source.getSender() instanceof Player player) {
                options.addAll(plugin.data().getMarkNames(player.getUniqueId()));
            }
            return Suggest.filter(options, typed);
        }
        if (index <= 2) {
            return Suggest.filter(List.of("~"), typed);
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "kamicore.shunkan";
    }
}
