package com.kamismp.kamicore.util;

import com.kamismp.kamicore.KamiCore;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers pending /kisense requests.
 * For every target player we keep: who asked -> when they asked.
 */
public final class TeleportRequests {

    private final KamiCore plugin;
    private final Map<UUID, LinkedHashMap<UUID, Long>> requestsByTarget = new HashMap<>();

    public TeleportRequests(KamiCore plugin) {
        this.plugin = plugin;
    }

    private long timeoutMillis() {
        return plugin.getConfig().getLong("kisense.request-timeout", 60) * 1000L;
    }

    public void add(UUID requester, UUID target) {
        LinkedHashMap<UUID, Long> map = requestsByTarget.computeIfAbsent(target, k -> new LinkedHashMap<>());
        map.remove(requester); // re-insert so it becomes the newest
        map.put(requester, System.currentTimeMillis());
    }

    /** Removes requests that are too old. */
    private void cleanExpired(UUID target) {
        LinkedHashMap<UUID, Long> map = requestsByTarget.get(target);
        if (map == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long timeout = timeoutMillis();
        map.values().removeIf(createdAt -> now - createdAt > timeout);
        if (map.isEmpty()) {
            requestsByTarget.remove(target);
        }
    }

    public boolean has(UUID requester, UUID target) {
        cleanExpired(target);
        LinkedHashMap<UUID, Long> map = requestsByTarget.get(target);
        return map != null && map.containsKey(requester);
    }

    /** The newest request sent to this target, or null if there is none. */
    public UUID newest(UUID target) {
        cleanExpired(target);
        LinkedHashMap<UUID, Long> map = requestsByTarget.get(target);
        if (map == null || map.isEmpty()) {
            return null;
        }
        UUID newest = null;
        for (UUID requester : map.keySet()) {
            newest = requester; // last one in insertion order
        }
        return newest;
    }

    public void remove(UUID requester, UUID target) {
        LinkedHashMap<UUID, Long> map = requestsByTarget.get(target);
        if (map != null) {
            map.remove(requester);
            if (map.isEmpty()) {
                requestsByTarget.remove(target);
            }
        }
    }

    /** Forget everything involving this player (used when they log out). */
    public void clearPlayer(UUID player) {
        requestsByTarget.remove(player);
        Iterator<Map.Entry<UUID, LinkedHashMap<UUID, Long>>> it = requestsByTarget.entrySet().iterator();
        while (it.hasNext()) {
            LinkedHashMap<UUID, Long> map = it.next().getValue();
            map.remove(player);
            if (map.isEmpty()) {
                it.remove();
            }
        }
    }
}
