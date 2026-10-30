package com.kamismp.kamicore.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Visual and sound effects shared by several commands.
 */
public final class Effects {

    private Effects() {
    }

    /**
     * Instant Transmission: plays an effect where the player was,
     * teleports them instantly, then plays an effect where they arrive.
     */
    public static void instantTransmission(Player player, Location target) {
        flash(player.getLocation());

        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
        player.setFallDistance(0);

        player.teleportAsync(target).thenAccept(success -> {
            if (success) {
                flash(player.getLocation());
            } else {
                Msg.send(player, "<red>Your Instant Transmission was interrupted!");
            }
        });
    }

    private static void flash(Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        Location center = loc.clone().add(0, 1, 0);
        world.spawnParticle(Particle.END_ROD, center, 40, 0.3, 0.8, 0.3, 0.05);
        world.spawnParticle(Particle.FIREWORK, center, 20, 0.2, 0.5, 0.2, 0.1);
        world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.6f);
    }
}
