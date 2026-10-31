package com.kamismp.kamicore.ki;

import org.bukkit.Color;

/**
 * Every ki attack in the plugin. The numbers (damage, range, craters...) live in
 * config.yml under the attack's id; this enum only holds how it looks and behaves.
 */
public enum KiStyle {

    KAMEHAMEHA("kamehameha", "Kamehameha", Type.BEAM,
            Color.fromRGB(90, 190, 255), Color.fromRGB(220, 245, 255),
            new String[]{"KA...", "KA... ME...", "KA... ME... HA...", "KA... ME... HA... ME..."},
            "HAAAAAAAAAA!!!"),

    GALICK_GUN("galickgun", "Galick Gun", Type.BEAM,
            Color.fromRGB(170, 60, 255), Color.fromRGB(230, 190, 255),
            new String[]{"GALICK...", "GALICK...", "GALICK... GUN...", "GALICK... GUN..."},
            "GALICK GUN! FIRE!!!"),

    FINAL_FLASH("finalflash", "Final Flash", Type.BEAM,
            Color.fromRGB(255, 225, 60), Color.fromRGB(255, 255, 210),
            new String[]{"Gathering ki...", "FINAL...", "FINAL...", "FINAL... FLASH..."},
            "FINAL FLAAAAASH!!!"),

    SPECIAL_BEAM_CANNON("specialbeamcannon", "Special Beam Cannon", Type.PIERCE,
            Color.fromRGB(255, 240, 120), Color.fromRGB(190, 80, 255),
            new String[]{"Two fingers to the forehead...", "Focusing...", "Focusing... focusing...", "Almost there..."},
            "SPECIAL BEAM CANNON!!!"),

    SPIRIT_BOMB("spiritbomb", "Spirit Bomb", Type.BOMB,
            Color.fromRGB(130, 210, 255), Color.fromRGB(255, 255, 255),
            new String[]{"Raise your hands...", "Gathering energy...", "Everyone, lend me your energy!", "The Spirit Bomb is complete!"},
            "SPIRIT BOMB!!!");

    /** How the attack travels. */
    public enum Type {
        /** Straight beam that stops at the first thing it hits. */
        BEAM,
        /** Thin drilling beam that goes through mobs, players AND blocks. */
        PIERCE,
        /** Giant ball that flies slowly and explodes on impact. */
        BOMB
    }

    private final String id;
    private final String displayName;
    private final Type type;
    private final Color mainColor;
    private final Color coreColor;
    private final String[] chant;
    private final String shout;

    KiStyle(String id, String displayName, Type type, Color mainColor, Color coreColor, String[] chant, String shout) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.mainColor = mainColor;
        this.coreColor = coreColor;
        this.chant = chant;
        this.shout = shout;
    }

    /** Used for the command name, config section, cooldown key and permission. */
    public String id() { return id; }
    public String displayName() { return displayName; }
    public Type type() { return type; }
    public Color mainColor() { return mainColor; }
    public Color coreColor() { return coreColor; }
    public String shout() { return shout; }

    public String chant(int levelIndex) {
        return chant[Math.min(levelIndex, chant.length - 1)];
    }

    public String permission() {
        return "kamicore." + id;
    }
}
