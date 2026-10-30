package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import com.kamismp.kamicore.util.Suggest;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

/**
 * /scouter          - scan every player online: power level, coordinates, distance
 * /scouter <player> - full reading of one player
 */
public final class ScouterCommand implements BasicCommand {

    private static final String COOLDOWN_KEY = "scouter";

    private final KamiCore plugin;

    public ScouterCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            Msg.playersOnly(source.getSender());
            return;
        }
        if (!plugin.cooldowns().checkReady(player, COOLDOWN_KEY, "Scouter")) {
            return;
        }

        if (args.length == 0) {
            scanAll(player);
        } else {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                Msg.send(player, "<red>Scouter can't find <yellow>" + args[0] + "</yellow>. They might be hiding their ki (offline).");
                return;
            }
            scanOne(player, target);
        }
        plugin.cooldowns().start(player, COOLDOWN_KEY, plugin.getConfig().getLong("scouter.cooldown", 15));
    }

    /** Short list of everyone online, strongest first. */
    private void scanAll(Player viewer) {
        List<Player> targets = new ArrayList<>(Bukkit.getOnlinePlayers());
        targets.remove(viewer);
        if (targets.isEmpty()) {
            Msg.send(viewer, "<gray>*beep* No other power levels detected.");
            return;
        }
        targets.sort(Comparator.comparingLong((Player p) -> powerLevel(p)).reversed());

        beep(viewer);
        Msg.send(viewer, "<green><b>SCOUTER</b></green> <gray>detected <yellow>" + targets.size() + "</yellow> power levels:");
        for (Player target : targets) {
            Msg.raw(viewer, " <dark_gray>-</dark_gray> <click:run_command:'/scouter " + target.getName() + "'>"
                    + "<hover:show_text:'Click for a full reading'><yellow>" + target.getName() + "</yellow></hover></click> "
                    + "<gold>" + formatPower(powerLevel(target)) + "</gold> <dark_gray>|</dark_gray> " + where(viewer, target));
        }
    }

    /** Full reading of one player. */
    private void scanOne(Player viewer, Player target) {
        long power = powerLevel(target);

        AttributeInstance maxAttr = target.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxAttr != null ? maxAttr.getValue() : 20.0;

        beep(viewer);
        Msg.send(viewer, "<green><b>SCOUTER READING</b></green> <dark_gray>»</dark_gray> <yellow>" + target.getName());
        line(viewer, "Power level", "<gold><b>" + formatPower(power) + "</b>");
        line(viewer, "Health", "<red>" + oneDecimal(target.getHealth() / 2) + "</red><gray>/" + oneDecimal(maxHealth / 2) + " ❤");
        line(viewer, "Armor", "<white>" + Math.round(attribute(target, Attribute.ARMOR)) + "</white> <gray>(toughness "
                + Math.round(attribute(target, Attribute.ARMOR_TOUGHNESS)) + ")");
        line(viewer, "Hunger", "<white>" + target.getFoodLevel() + "/20");
        line(viewer, "XP level", "<green>" + target.getLevel());

        int kaioken = plugin.kaioken().multiplier(target);
        if (kaioken > 1) {
            line(viewer, "Status", "<red><b>KAIOKEN x" + kaioken + "</b>");
        }
        String gravity = plugin.gravity().activeLevel(target);
        if (gravity != null) {
            line(viewer, "Status", "<dark_red>Training in " + gravity.toUpperCase(Locale.ROOT) + " gravity");
        }
        if (plugin.nimbus().isRiding(target)) {
            line(viewer, "Status", "<yellow>Riding the Flying Nimbus");
        }
        line(viewer, "Location", where(viewer, target));

        if (power > 9000) {
            Msg.raw(viewer, "<red><b>   IT'S OVER 9000!!!</b>");
            viewer.playSound(viewer.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.8f);
        }

        if (!target.equals(viewer) && plugin.getConfig().getBoolean("scouter.notify-target", true)) {
            Msg.send(target, "<gray>You feel someone's scouter reading your power level...");
        }
    }

    /**
     * The power level formula. Made-up, but fun:
     * health, armor and XP level add up, and Kaioken multiplies everything.
     */
    public long powerLevel(Player player) {
        double base = player.getHealth() * 25
                + attribute(player, Attribute.ARMOR) * 60
                + attribute(player, Attribute.ARMOR_TOUGHNESS) * 40
                + player.getLevel() * 75;
        return Math.round(base * plugin.kaioken().multiplier(player));
    }

    /** Coordinates (if allowed in config) + distance or world name. */
    private String where(Player viewer, Player target) {
        Location loc = target.getLocation();
        StringBuilder sb = new StringBuilder();
        if (plugin.getConfig().getBoolean("scouter.show-coordinates", true)) {
            sb.append("<aqua>").append(loc.getBlockX()).append(", ").append(loc.getBlockY()).append(", ").append(loc.getBlockZ()).append("</aqua> ");
        }
        if (target.equals(viewer)) {
            sb.append("<gray>(you)");
        } else if (loc.getWorld().equals(viewer.getWorld())) {
            sb.append("<gray>(").append(Math.round(loc.distance(viewer.getLocation()))).append(" blocks away)");
        } else {
            sb.append("<gray>(in ").append(loc.getWorld().getName()).append(")");
        }
        return sb.toString();
    }

    private static double attribute(Player player, Attribute attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        return instance != null ? instance.getValue() : 0;
    }

    private static void line(Player viewer, String label, String value) {
        Msg.raw(viewer, "   <gray>" + label + ":</gray> " + value);
    }

    private static void beep(Player viewer) {
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 2.0f);
    }

    private static String formatPower(long power) {
        return String.format(Locale.ROOT, "%,d", power);
    }

    private static String oneDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (Suggest.index(args) != 0) {
            return List.of();
        }
        List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return Suggest.filter(names, Suggest.current(args));
    }

    @Override
    public String permission() {
        return "kamicore.scouter";
    }
}
