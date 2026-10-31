package com.kamismp.kamicore.util;

import com.kamismp.kamicore.KamiCore;
import org.bukkit.entity.Player;

/**
 * Handles cooldowns. They are saved in data.yml, so a restart
 * does NOT reset a 24-hour Kaioken cooldown.
 */
public final class Cooldowns {

    public static final String BYPASS_PERMISSION = "kamicore.bypass.cooldown";

    private final KamiCore plugin;

    public Cooldowns(KamiCore plugin) {
        this.plugin = plugin;
    }

    /** Milliseconds left on a cooldown (0 = ready). */
    public long remaining(Player player, String ability) {
        if (player.hasPermission(BYPASS_PERMISSION) || plugin.wishes().hasUnlimitedPower(player)) {
            return 0;
        }
        long readyAt = plugin.data().getCooldown(player.getUniqueId(), ability);
        return Math.max(0, readyAt - System.currentTimeMillis());
    }

    /**
     * Returns true if the ability can be used. If not, tells the player how long to wait.
     */
    public boolean checkReady(Player player, String ability, String displayName) {
        long left = remaining(player, ability);
        if (left <= 0) {
            return true;
        }
        Msg.send(player, "<red>" + displayName + " is recharging. <gray>Ready in <yellow>" + format(left) + "</yellow>.");
        return false;
    }

    /** Starts a cooldown that lasts the given number of seconds. */
    public void start(Player player, String ability, long seconds) {
        if (seconds <= 0 || player.hasPermission(BYPASS_PERMISSION) || plugin.wishes().hasUnlimitedPower(player)) {
            return;
        }
        long readyAt = System.currentTimeMillis() + seconds * 1000L;
        plugin.data().setCooldown(player.getUniqueId(), ability, readyAt);
    }

    /** Turns milliseconds into text like "1h 5m 3s". */
    public static String format(long millis) {
        long totalSeconds = (millis + 999) / 1000; // round up
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
