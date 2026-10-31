package com.kamismp.kamicore.duel;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import java.util.List;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Keeps duels fair: nobody leaves the arena, outsiders can't interfere,
 * and the fight ends properly on death or logout.
 */
public final class DuelListener implements Listener {

    private final KamiCore plugin;

    public DuelListener(KamiCore plugin) {
        this.plugin = plugin;
    }

    private Duels duels() {
        return plugin.duels();
    }

    /** Walking (or flying) into the wall just stops you. */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Duels.Duel duel = duels().duelOf(event.getPlayer());
        Location to = event.getTo();
        if (duel != null && !duel.contains(to) && duel.contains(event.getFrom())) {
            Location back = event.getFrom().clone();
            back.setYaw(to.getYaw());
            back.setPitch(to.getPitch());
            event.setTo(back);
        }
    }

    /** No teleporting out of the arena (ender pearls inside the arena are fine). */
    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Duels.Duel duel = duels().duelOf(event.getPlayer());
        if (duel != null && !duel.contains(event.getTo())) {
            event.setCancelled(true);
            Msg.send(event.getPlayer(), "<red>You can't escape the arena! <gray>(/duel forfeit to give up)");
        }
    }

    /** Teleport and escape commands are blocked during a duel. */
    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!duels().isInDuel(event.getPlayer())) {
            return;
        }
        String label = event.getMessage().substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        if (label.contains(":")) {
            label = label.substring(label.indexOf(':') + 1); // "kamicore:shunkan" -> "shunkan"
        }
        List<String> blocked = plugin.getConfig().getStringList("duel.blocked-commands");
        if (blocked.contains(label)) {
            event.setCancelled(true);
            Msg.send(event.getPlayer(), "<red>You can't use /" + label + " during a duel.");
        }
    }

    /** Only the two duelists can hurt each other, and only after "FIGHT!". */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = attackingPlayer(event.getDamager());
        Player victim = event.getEntity() instanceof Player p ? p : null;

        if (victim != null) {
            Duels.Duel duel = duels().duelOf(victim);
            if (duel != null && attacker != null) {
                boolean isOpponent = duel.opponentOf(victim.getUniqueId()).equals(attacker.getUniqueId());
                if (!isOpponent || !duel.isFighting()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (attacker != null && duels().isInDuel(attacker)) {
            Duels.Duel duel = duels().duelOf(attacker);
            boolean hittingOpponent = victim != null && duel.opponentOf(attacker.getUniqueId()).equals(victim.getUniqueId());
            if (victim != null && !hittingOpponent) {
                event.setCancelled(true); // duelists can't attack players outside the duel
            }
        }
    }

    private static Player attackingPlayer(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    /** The loser keeps their stuff (if enabled), and the duel ends. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player loser = event.getPlayer();
        if (!duels().isInDuel(loser)) {
            return;
        }
        if (plugin.getConfig().getBoolean("duel.keep-inventory", true)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
        duels().onDeath(loser);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location back = duels().takeRespawn(event.getPlayer());
        if (back != null) {
            event.setRespawnLocation(back);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        duels().onQuit(event.getPlayer());
    }
}
