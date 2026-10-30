package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import com.kamismp.kamicore.util.Suggest;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * /gravity      - start gravity chamber training (heavy slowness for a while)
 * /gravity stop - give up early (no XP)
 * Survive until the timer ends to earn XP. Dying or logging out = no XP.
 */
public final class GravityCommand implements BasicCommand {

    private static final String COOLDOWN_KEY = "gravity";

    private final KamiCore plugin;
    private final Map<UUID, BukkitTask> sessions = new HashMap<>();

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
        if (!plugin.cooldowns().checkReady(player, COOLDOWN_KEY, "Gravity chamber")) {
            return;
        }

        start(player);
    }

    public boolean isTraining(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    private void start(Player player) {
        int duration = Math.max(5, plugin.getConfig().getInt("gravity.duration", 60));
        int xpReward = plugin.getConfig().getInt("gravity.xp-reward", 150);

        // No flying in the chamber
        plugin.nimbus().dismount(player, true);

        int ticks = (duration + 1) * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 2, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, ticks, 1, false, false, true));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 0.5f);

        plugin.cooldowns().start(player, COOLDOWN_KEY, plugin.getConfig().getLong("gravity.cooldown", 1800));
        Msg.send(player, "<dark_red><b>100x GRAVITY!</b></dark_red> <gray>Survive <yellow>" + duration
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
                Msg.actionBar(player, "<dark_red>Gravity 100x</dark_red> <dark_gray>|</dark_gray> <yellow>" + secondsLeft + "s</yellow> <gray>left");
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        sessions.put(player.getUniqueId(), task);
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
        BukkitTask task = sessions.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private void clearEffects(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
    }

    public void cancelAll() {
        for (BukkitTask task : sessions.values()) {
            task.cancel();
        }
        sessions.clear();
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (Suggest.index(args) == 0) {
            return Suggest.filter(List.of("stop"), Suggest.current(args));
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "kamicore.gravity";
    }
}
