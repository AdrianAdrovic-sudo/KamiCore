package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * /kaioken - Strength + Speed with a red aura for a while.
 * When it ends your body strains: Weakness + Slowness.
 * One-day cooldown by default.
 */
public final class KaiokenCommand implements BasicCommand {

    private static final String COOLDOWN_KEY = "kaioken";
    private static final Particle.DustOptions RED_AURA = new Particle.DustOptions(Color.fromRGB(255, 30, 30), 1.6f);

    private final KamiCore plugin;
    private final Map<UUID, BukkitTask> active = new HashMap<>();

    public KaiokenCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            Msg.playersOnly(source.getSender());
            return;
        }
        if (active.containsKey(player.getUniqueId())) {
            Msg.send(player, "<red>Kaioken is already active! Don't push your body any further!");
            return;
        }
        if (!plugin.cooldowns().checkReady(player, COOLDOWN_KEY, "Kaioken")) {
            return;
        }

        int duration = Math.max(1, plugin.getConfig().getInt("kaioken.duration", 60));
        int strength = Math.max(1, plugin.getConfig().getInt("kaioken.strength-level", 2));
        int speed = Math.max(1, plugin.getConfig().getInt("kaioken.speed-level", 2));
        int ticks = duration * 20;

        // In code, amplifier 0 = level I, 1 = level II, so we subtract 1
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks, strength - 1, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, speed - 1, false, false, true));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.6f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.2f);

        plugin.cooldowns().start(player, COOLDOWN_KEY, plugin.getConfig().getLong("kaioken.cooldown", 86400));
        Msg.send(player, "<red><b>KAIOKEN!</b></red> <gold>Your power multiplies!</gold> <gray>(" + duration + "s)");

        // Red aura every half second; when time is up, apply the strain
        BukkitTask task = new BukkitRunnable() {
            int halfSecondsLeft = duration * 2;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    active.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                if (halfSecondsLeft <= 0) {
                    active.remove(player.getUniqueId());
                    cancel();
                    applyStrain(player);
                    return;
                }
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 15, 0.4, 0.8, 0.4, 0.0, RED_AURA);
                halfSecondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 10L);

        active.put(player.getUniqueId(), task);
    }

    private void applyStrain(Player player) {
        int strain = plugin.getConfig().getInt("kaioken.strain-duration", 20);
        if (strain > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, strain * 20, 0, false, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, strain * 20, 0, false, false, true));
        }
        Msg.send(player, "<gray>Kaioken fades... <red>your body aches from the strain.");
    }

    public void cancelAll() {
        for (BukkitTask task : active.values()) {
            task.cancel();
        }
        active.clear();
    }

    @Override
    public String permission() {
        return "kamicore.kaioken";
    }
}
