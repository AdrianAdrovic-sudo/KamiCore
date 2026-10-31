package com.kamismp.kamicore.listeners;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

/**
 * Reacts to things that happen in the game (logging out, dying, getting hit...).
 */
public final class PlayerListener implements Listener {

    private final KamiCore plugin;

    public PlayerListener(KamiCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.nimbus().dismount(player, false);
        plugin.gravity().fail(player, null);
        plugin.kiAttacks().cancelCharge(player, null);
        plugin.superSaiyan().revert(player, null);
        plugin.teleportRequests().clearPlayer(player.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.superSaiyan().cleanupOnJoin(player);
        plugin.wishes().refresh(player);
        plugin.dragonBalls().purgeStale(player);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        plugin.nimbus().dismount(player, false);
        plugin.gravity().fail(player, "<red>The gravity was too much... training failed.");
        plugin.kiAttacks().cancelCharge(player, null);
        plugin.superSaiyan().revert(player, null);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (plugin.superSaiyan().tryDodge(player)) {
            event.setCancelled(true); // Ultra Instinct
            return;
        }
        if (plugin.kiAttacks().isCharging(player) && plugin.getConfig().getBoolean("ki-attacks.cancel-on-hit", true)) {
            plugin.kiAttacks().cancelCharge(player, "<red>You got hit and lost your focus! Your attack fizzles out.");
        }
        if (plugin.nimbus().isRiding(player) && plugin.getConfig().getBoolean("nimbus.dismount-on-hit", true)) {
            plugin.nimbus().dismount(player, false);
            Msg.send(player, "<red>You got knocked off the Flying Nimbus!");
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() != GameMode.SURVIVAL && event.getNewGameMode() != GameMode.ADVENTURE) {
            plugin.nimbus().forget(event.getPlayer());
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        plugin.nimbus().reapply(event.getPlayer());
    }
    /** Super Saiyan: double-tapping jump in the air starts ki flight instead of normal flying. */
    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!plugin.superSaiyan().isTransformed(player)) {
            return;
        }
        if (plugin.superSaiyan().isKiFlying(player)) {
            // Double-tap again while flying: switch to hovering in place
            plugin.superSaiyan().stopFlight(player);
            return;
        }
        if (event.isFlying() && !player.isOnGround()) {
            event.setCancelled(true);
            plugin.superSaiyan().startFlight(player);
        }
    }

    /** Without an elytra the server tries to stop gliding every tick; keep ki flyers in the air. */
    @EventHandler
    public void onGlideToggle(EntityToggleGlideEvent event) {
        if (!event.isGliding() && event.getEntity() instanceof Player player && plugin.superSaiyan().isKiFlying(player)) {
            event.setCancelled(true);
        }
    }

    /** Transformed players take no fall damage and don't get hurt crashing into walls. */
    @EventHandler(ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !plugin.superSaiyan().isTransformed(player)) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL || event.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
            event.setCancelled(true);
        }
    }
}
