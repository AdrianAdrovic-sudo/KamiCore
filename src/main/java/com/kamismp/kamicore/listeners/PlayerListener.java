package com.kamismp.kamicore.listeners;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
}
