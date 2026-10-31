package com.kamismp.kamicore.data;

import com.kamismp.kamicore.KamiCore;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Saves player data to plugins/KamiCore/data.yml.
 *
 * Layout of the file:
 * players:
 *   <uuid>:
 *     marks:
 *       base: {world: world, x: 10.5, y: 64.0, z: -3.5, yaw: 90.0, pitch: 0.0}
 *     cooldowns:
 *       senzu: 1767225600000   (time in ms when it is ready again)
 */
public final class DataStore {

    private final KamiCore plugin;
    private final File file;
    private YamlConfiguration yaml = new YamlConfiguration();

    public DataStore(KamiCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        if (file.exists()) {
            yaml = YamlConfiguration.loadConfiguration(file);
        }
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", e);
        }
    }

    private String base(UUID player) {
        return "players." + player;
    }

    // ------------------------------------------------------------ ki marks

    public Set<String> getMarkNames(UUID player) {
        ConfigurationSection section = yaml.getConfigurationSection(base(player) + ".marks");
        if (section == null) {
            return Collections.emptySet();
        }
        return new TreeSet<>(section.getKeys(false));
    }

    public boolean hasMark(UUID player, String name) {
        return yaml.isConfigurationSection(base(player) + ".marks." + name);
    }

    /** Returns the saved location, or null if the mark (or its world) does not exist. */
    public Location getMark(UUID player, String name) {
        ConfigurationSection s = yaml.getConfigurationSection(base(player) + ".marks." + name);
        if (s == null) {
            return null;
        }
        World world = Bukkit.getWorld(s.getString("world", "world"));
        if (world == null) {
            return null;
        }
        return new Location(world,
                s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
    }

    public void setMark(UUID player, String name, Location loc) {
        String path = base(player) + ".marks." + name;
        yaml.set(path + ".world", loc.getWorld().getName());
        yaml.set(path + ".x", loc.getX());
        yaml.set(path + ".y", loc.getY());
        yaml.set(path + ".z", loc.getZ());
        yaml.set(path + ".yaw", (double) loc.getYaw());
        yaml.set(path + ".pitch", (double) loc.getPitch());
        save();
    }

    public void deleteMark(UUID player, String name) {
        yaml.set(base(player) + ".marks." + name, null);
        save();
    }

    // ---------------------------------------------- timed wishes and others

    /** A number saved for a player, e.g. "immortal-until" (0 if not set). */
    public long getPlayerLong(UUID player, String key) {
        return yaml.getLong(base(player) + "." + key, 0L);
    }

    public void setPlayerLong(UUID player, String key, long value) {
        yaml.set(base(player) + "." + key, value == 0 ? null : value);
        save();
    }

    // ----------------------------------------------------------- cooldowns

    public long getCooldown(UUID player, String ability) {
        return yaml.getLong(base(player) + ".cooldowns." + ability, 0L);
    }

    public void setCooldown(UUID player, String ability, long readyAt) {
        yaml.set(base(player) + ".cooldowns." + ability, readyAt);
        save();
    }
}
