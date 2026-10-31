package com.kamismp.kamicore.saiyan;

import java.util.Locale;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;

/**
 * One transformation, read from config.yml (supersaiyan.forms.<id>).
 */
public record SaiyanForm(
        String id,
        String name,
        NamedTextColor glowColor,
        Color auraColor,
        int minLevel,
        int xpDrain,
        int strength,
        int speed,
        int resistance,
        int jump,
        int regeneration,
        double extraHearts,
        boolean flight,
        boolean lightning,
        double kiMultiplier,
        double powerMultiplier,
        double dodgeChance) {

    public static SaiyanForm fromConfig(String id, ConfigurationSection s) {
        NamedTextColor glow = NamedTextColor.NAMES.value(s.getString("color", "gold").toLowerCase(Locale.ROOT));
        return new SaiyanForm(
                id,
                s.getString("name", id),
                glow != null ? glow : NamedTextColor.GOLD,
                parseHex(s.getString("aura", "#FFD700")),
                s.getInt("min-level", 0),
                Math.max(0, s.getInt("xp-drain", 2)),
                s.getInt("strength", 0),
                s.getInt("speed", 0),
                s.getInt("resistance", 0),
                s.getInt("jump", 0),
                s.getInt("regeneration", 0),
                s.getDouble("extra-hearts", 0),
                s.getBoolean("flight", true),
                s.getBoolean("lightning", false),
                s.getDouble("ki-multiplier", 1.0),
                s.getDouble("power-multiplier", 1.0),
                s.getDouble("dodge-chance", 0));
    }

    /** "#FFD700" -> Color. Falls back to gold if the text is not a valid color. */
    private static Color parseHex(String hex) {
        try {
            return Color.fromRGB(Integer.parseInt(hex.replace("#", ""), 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return Color.fromRGB(255, 215, 0);
        }
    }

    /** MiniMessage color tag for chat, e.g. "<gold>". */
    public String chatColor() {
        String name = NamedTextColor.NAMES.key(glowColor);
        return "<" + (name != null ? name : "gold") + ">";
    }
}
