package com.kamismp.kamicore.ki;

import com.kamismp.kamicore.KamiCore;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Terrain destruction for ki attacks. Settings in config.yml under "destruction".
 * Blocks are removed permanently. Containers (chests, shulkers...) and very strong
 * blocks (obsidian, bedrock...) are protected by default.
 */
public final class Destruction {

    private static final double MAX_RADIUS = 10;

    private final KamiCore plugin;

    public Destruction(KamiCore plugin) {
        this.plugin = plugin;
    }

    private boolean enabledIn(World world) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("destruction.enabled", true)) {
            return false;
        }
        List<String> disabledWorlds = config.getStringList("destruction.disabled-worlds");
        return !disabledWorlds.contains(world.getName());
    }

    /** Blows a roughly round crater around the center. Returns how many blocks were destroyed. */
    public int crater(Location center, double radius) {
        World world = center.getWorld();
        if (world == null || radius <= 0 || !enabledIn(world)) {
            return 0;
        }
        radius = Math.min(radius, MAX_RADIUS);
        int r = (int) Math.ceil(radius);
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        int destroyed = 0;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    // A little randomness so craters look natural, not like perfect spheres
                    double edge = radius * (0.8 + random.nextDouble() * 0.3);
                    if (x * x + y * y + z * z > edge * edge) {
                        continue;
                    }
                    if (destroy(world.getBlockAt(cx + x, cy + y, cz + z))) {
                        destroyed++;
                    }
                }
            }
        }
        return destroyed;
    }

    /** Removes one block if it's allowed to be destroyed. */
    public boolean destroy(Block block) {
        if (block.isEmpty() || !enabledIn(block.getWorld())) {
            return false;
        }
        FileConfiguration config = plugin.getConfig();
        Material type = block.getType();
        if (type.getBlastResistance() > config.getDouble("destruction.max-blast-resistance", 1000)) {
            return false; // obsidian, bedrock, barriers, portals...
        }
        if (block.getY() <= block.getWorld().getMinHeight()) {
            return false; // never open the bottom of the world
        }
        if (config.getBoolean("destruction.protect-containers", true) && block.getState(false) instanceof Container) {
            return false; // chests, barrels, shulker boxes, furnaces...
        }
        if (config.getBoolean("destruction.drop-items", false)) {
            block.breakNaturally();
        } else {
            block.setType(Material.AIR, true);
        }
        return true;
    }
}
