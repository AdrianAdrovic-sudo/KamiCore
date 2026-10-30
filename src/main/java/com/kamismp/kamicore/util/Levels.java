package com.kamismp.kamicore.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Reads "levels" sections from config.yml, for example:
 *
 * kaioken:
 *   levels:
 *     x2: {...}
 *     x10: {...}
 *
 * The order in the file is kept, so the first level is the weakest.
 */
public final class Levels {

    private Levels() {
    }

    /** All level names in the order they appear in config.yml. */
    public static List<String> names(FileConfiguration config, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return List.of();
        }
        return new ArrayList<>(section.getKeys(false));
    }

    /** The settings of one level, or null if that level does not exist. */
    public static ConfigurationSection get(FileConfiguration config, String path, String levelName) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return null;
        }
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase(levelName)) {
                return section.getConfigurationSection(key);
            }
        }
        return null;
    }

    /** Turns "x10" or "100x" into 10 / 100. Returns the fallback if there are no digits. */
    public static int number(String levelName, int fallback) {
        String digits = levelName.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** "x2, x3, x10, x20" - used in error messages. */
    public static String list(List<String> names) {
        return String.join(", ", names).toLowerCase(Locale.ROOT);
    }
}
