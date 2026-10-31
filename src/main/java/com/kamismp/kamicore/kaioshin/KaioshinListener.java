package com.kamismp.kamicore.kaioshin;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Cooldowns;
import com.kamismp.kamicore.util.Msg;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.List;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Enforces the Kaioshin powers: sealed players can't chat, frozen players can't move.
 */
public final class KaioshinListener implements Listener {

    private final KamiCore plugin;

    public KaioshinListener(KamiCore plugin) {
        this.plugin = plugin;
    }

    /** Sealed players can't talk. (Runs on the chat thread.) */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.kaioshin().isSealed(player.getUniqueId())) {
            event.setCancelled(true);
            Msg.send(player, sealedMessage(player));
        }
    }

    /** ...and can't use private message commands either. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!plugin.kaioshin().isSealed(player.getUniqueId())) {
            return;
        }
        String label = event.getMessage().substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        if (label.contains(":")) {
            label = label.substring(label.indexOf(':') + 1);
        }
        List<String> muted = plugin.getConfig().getStringList("kaioshin.sealed-commands");
        if (muted.contains(label)) {
            event.setCancelled(true);
            Msg.send(player, sealedMessage(player));
        }
    }

    private String sealedMessage(Player player) {
        long until = plugin.kaioshin().sealedUntil(player.getUniqueId());
        String time = until == Long.MAX_VALUE ? "forever" : "for " + Cooldowns.format(until - System.currentTimeMillis());
        return "<dark_purple>You are sealed in the Mafuba jar</dark_purple> <gray>and cannot speak " + time + ".";
    }

    /** Time Skip: frozen players can look around but not move. */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.kaioshin().isFrozen(event.getPlayer().getUniqueId())) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            Location back = from.clone();
            back.setYaw(to.getYaw());
            back.setPitch(to.getPitch());
            event.setTo(back);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.kaioshin().loadSeal(event.getPlayer());
        if (plugin.kaioshin().isFrozen(event.getPlayer().getUniqueId())) {
            Msg.send(event.getPlayer(), "<dark_aqua>Time is still stopped for you.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.kaioshin().onQuit(event.getPlayer());
    }
}
