package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Levels;
import com.kamismp.kamicore.util.Msg;
import com.kamismp.kamicore.util.Suggest;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * /gravity [level] - e.g. /gravity 100x
 * /gravity stop    - give up early (no XP)
 * Higher gravity = heavier effects, more XP, longer cooldown.
 * Levels are defined in config.yml under gravity.levels.
 */
public final class GravityCommand implements BasicCommand {

    private static final String COOLDOWN_KEY = "gravity";
    private static final String LEVELS_PATH = "gravity.levels";

    private record Session(String level, BukkitTask task) {
    }

    private final KamiCore plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public GravityCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            Msg.playersOnly(source.getSender());
            return;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("stop")) {
            if (isTraining(player)) {
                fail(player, "<gray>You step out of the gravity chamber. No XP this time.");
            } else {
                Msg.send(player, "<gray>You're not training right now.");
            }
            return;
        }
        if (isTraining(player)) {
            Msg.send(player, "<yellow>You're already training! <gray>(/gravity stop to quit)");
            return;
        }

        List<String> levelNames = Levels.names(plugin.getConfig(), LEVELS_PATH);
        if (levelNames.isEmpty()) {
            Msg.send(player, "<red>No gravity levels are set up in config.yml.");
            return;
        }
        String levelName = args.length >= 1
                ? args[0].toLowerCase(Locale.ROOT)
                : plugin.getConfig().getString("gravity.default-level", levelNames.get(0));
        ConfigurationSection level = Levels.get(plugin.getConfig(), LEVELS_PATH, levelName);
        if (level == null) {
            Msg.send(player, "<red>Unknown gravity level. Choose: <yellow>" + Levels.list(levelNames) + "</yellow>");
            return;
        }
        if (!plugin.cooldowns().checkReady(player, COOLDOWN_KEY, "Gravity chamber")) {
            return;
        }

        start(player, levelName, level);
    }

    public boolean isTraining(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    /** e.g. "100x", or null if not training. Used by the Scouter. */
    public String activeLevel(Player player) {
        Session s = sessions.get(player.getUniqueId());
        return s == null ? null : s.level();
    }

    private void start(Player player, String levelName, ConfigurationSection level) {
        int duration = Math.max(5, level.getInt("duration", 60));
        int xpReward = level.getInt("xp", 150);
        int slowness = level.getInt("slowness", 3);
        int fatigue = level.getInt("mining-fatigue", 2);
        int hunger = level.getInt("hunger", 0);
        String label = levelName.toUpperCase(Locale.ROOT);

        // No flying or transforming in the chamber
        plugin.nimbus().dismount(player, true);
        plugin.superSaiyan().revert(player, "<gray>The gravity forces you back to your base form.");

        int ticks = (duration + 1) * 20;
        if (slowness > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, slowness - 1, false, false, true));
        }
        if (fatigue > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, ticks, fatigue - 1, false, false, true));
        }
        if (hunger > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, ticks, hunger - 1, false, false, true));
        }
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 0.5f);

        plugin.cooldowns().start(player, COOLDOWN_KEY, level.getLong("cooldown", 1800));
        Msg.send(player, "<dark_red><b>" + label + " GRAVITY!</b></dark_red> <gray>Survive <yellow>" + duration
                + "s</yellow> to earn <green>" + xpReward + " XP</green>.");

        BukkitTask task = new BukkitRunnable() {
            int secondsLeft = duration;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    sessions.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                if (secondsLeft <= 0) {
                    complete(player, xpReward);
                    return;
                }
                Msg.actionBar(player, "<dark_red>Gravity " + label + "</dark_red> <dark_gray>|</dark_gray> <yellow>" + secondsLeft + "s</yellow> <gray>left");
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        sessions.put(player.getUniqueId(), new Session(levelName, task));
    }

    private void complete(Player player, int xpReward) {
        stopTask(player);
        clearEffects(player);
        player.giveExp(xpReward);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        Msg.send(player, "<gold>Training complete!</gold> <green>+" + xpReward + " XP</green> <gray>Your power level rises.");
    }

    /** Ends training without a reward. */
    public void fail(Player player, String message) {
        if (!isTraining(player)) {
            return;
        }
        stopTask(player);
        clearEffects(player);
        if (message != null) {
            Msg.send(player, message);
        }
    }

    private void stopTask(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session != null) {
            session.task().cancel();
        }
    }

    private void clearEffects(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        player.removePotionEffect(PotionEffectType.HUNGER);
    }

    public void cancelAll() {
        for (Session session : sessions.values()) {
            session.task().cancel();
        }
        sessions.clear();
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (Suggest.index(args) == 0) {
            List<String> options = new ArrayList<>(Levels.names(plugin.getConfig(), LEVELS_PATH));
            options.add("stop");
            return Suggest.filter(options, Suggest.current(args));
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "kamicore.gravity";
    }
}
