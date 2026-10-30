package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * /nimbus - toggle riding the Flying Nimbus (survival flight).
 * Run it again to jump off. You get Slow Falling for a few seconds so you don't splat.
 */
public final class NimbusCommand implements BasicCommand {

    private static final String COOLDOWN_KEY = "nimbus";
    private static final float DEFAULT_FLY_SPEED = 0.1f;

    private final KamiCore plugin;
    private final Set<UUID> riders = new HashSet<>();

    public NimbusCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            Msg.playersOnly(source.getSender());
            return;
        }

        if (isRiding(player)) {
            dismount(player, true);
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            Msg.send(player, "<gray>You can already fly in this game mode.");
            return;
        }
        if (plugin.gravity().isTraining(player)) {
            Msg.send(player, "<red>Nimbus can't reach you inside the gravity chamber!");
            return;
        }
        if (!plugin.cooldowns().checkReady(player, COOLDOWN_KEY, "Flying Nimbus")) {
            return;
        }

        mount(player);
        plugin.cooldowns().start(player, COOLDOWN_KEY, plugin.getConfig().getLong("nimbus.cooldown", 10));
    }

    public boolean isRiding(Player player) {
        return riders.contains(player.getUniqueId());
    }

    private void mount(Player player) {
        riders.add(player.getUniqueId());
        float speed = (float) plugin.getConfig().getDouble("nimbus.fly-speed", DEFAULT_FLY_SPEED);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(Math.max(0f, Math.min(1f, speed)));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0f, 0.8f);
        Msg.send(player, "<yellow>Kintoun!</yellow> <gold>The Flying Nimbus picks you up.</gold> <gray>Use <yellow>/nimbus</yellow> again to jump off.");
    }

    /** Takes the player off the nimbus. */
    public void dismount(Player player, boolean announce) {
        if (!riders.remove(player.getUniqueId())) {
            return;
        }
        if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        player.setFlySpeed(DEFAULT_FLY_SPEED);
        // 8 seconds of slow falling so jumping off in the sky is safe
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 8, 0, false, false, true));
        if (announce) {
            Msg.send(player, "<gray>You hop off the Flying Nimbus.");
        }
    }

    /** Forget a rider without touching their flight (e.g. they switched to creative). */
    public void forget(Player player) {
        riders.remove(player.getUniqueId());
    }

    /** Some servers reset flight when changing worlds - turn it back on. */
    public void reapply(Player player) {
        if (isRiding(player)) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }
    }

    public void dismountAll() {
        for (UUID id : Set.copyOf(riders)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                dismount(player, false);
            }
        }
        riders.clear();
    }

    /** Little cloud under everyone riding the nimbus. */
    public void startParticleTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID id : riders) {
                Player player = Bukkit.getPlayer(id);
                if (player == null || !player.isFlying()) {
                    continue;
                }
                Location feet = player.getLocation();
                player.getWorld().spawnParticle(Particle.CLOUD, feet.getX(), feet.getY() - 0.2, feet.getZ(), 6, 0.4, 0.05, 0.4, 0.0);
            }
        }, 20L, 4L);
    }

    @Override
    public String permission() {
        return "kamicore.nimbus";
    }
}
