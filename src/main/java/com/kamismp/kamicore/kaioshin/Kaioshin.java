package com.kamismp.kamicore.kaioshin;

import com.kamismp.kamicore.KamiCore;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/**
 * State for the Kaioshin (admin) powers:
 *  - sealed players (muted), saved in data.yml so a relog doesn't unmute them
 *  - frozen players (Time Skip)
 *  - Kaioshin in Divine mode (invulnerable + flight)
 *
 * Chat runs on another thread, so the seal list uses a thread-safe map.
 */
public final class Kaioshin {

    public static final String PERMISSION = "kamicore.kaioshin";
    private static final String SEAL_KEY = "sealed-until";

    private final KamiCore plugin;
    private final Map<UUID, Long> sealedUntil = new ConcurrentHashMap<>();
    private final Set<UUID> frozen = ConcurrentHashMap.newKeySet();
    private final Set<UUID> divine = ConcurrentHashMap.newKeySet();

    public Kaioshin(KamiCore plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------- sealing

    /** Seal (mute) a player. minutes <= 0 means forever. */
    public void seal(Player player, long minutes) {
        long until = minutes <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + minutes * 60_000L;
        sealedUntil.put(player.getUniqueId(), until);
        plugin.data().setPlayerLong(player.getUniqueId(), SEAL_KEY, until);
    }

    public void unseal(UUID player) {
        sealedUntil.remove(player);
        plugin.data().setPlayerLong(player, SEAL_KEY, 0);
    }

    /** Safe to call from the chat thread. */
    public boolean isSealed(UUID player) {
        Long until = sealedUntil.get(player);
        if (until == null) {
            return false;
        }
        if (until <= System.currentTimeMillis()) {
            sealedUntil.remove(player);
            return false;
        }
        return true;
    }

    public long sealedUntil(UUID player) {
        return sealedUntil.getOrDefault(player, 0L);
    }

    /** Load a player's seal from data.yml when they join. */
    public void loadSeal(Player player) {
        long until = plugin.data().getPlayerLong(player.getUniqueId(), SEAL_KEY);
        if (until > System.currentTimeMillis()) {
            sealedUntil.put(player.getUniqueId(), until);
        } else if (until != 0) {
            plugin.data().setPlayerLong(player.getUniqueId(), SEAL_KEY, 0);
        }
    }

    // ----------------------------------------------------------- time skip

    /** Toggles freezing. Returns true if the player is now frozen. */
    public boolean toggleFrozen(UUID player) {
        if (frozen.remove(player)) {
            return false;
        }
        frozen.add(player);
        return true;
    }

    public boolean isFrozen(UUID player) {
        return frozen.contains(player);
    }

    // -------------------------------------------------------------- divine

    /** Toggles Divine mode. Returns true if it's now on. */
    public boolean toggleDivine(Player player) {
        if (divine.remove(player.getUniqueId())) {
            player.setInvulnerable(false);
            if (!plugin.superSaiyan().isTransformed(player) && !plugin.nimbus().isRiding(player)
                    && player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
            return false;
        }
        divine.add(player.getUniqueId());
        player.setInvulnerable(true);
        player.setAllowFlight(true);
        return true;
    }

    public boolean isDivine(Player player) {
        return divine.contains(player.getUniqueId());
    }

    /** Divine mode doesn't survive logging out. */
    public void onQuit(Player player) {
        if (divine.remove(player.getUniqueId())) {
            player.setInvulnerable(false);
        }
    }
}
