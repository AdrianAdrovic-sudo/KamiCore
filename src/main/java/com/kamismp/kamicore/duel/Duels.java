package com.kamismp.kamicore.duel;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * One-on-one duels inside a square arena.
 *
 * Both players get their own private world border (only they can see it),
 * so the arena has a real red wall. The listener also stops them from walking
 * or teleporting out, in case someone glitches through.
 * The duel ends when one of them dies, quits, or time runs out (draw).
 */
public final class Duels {

    /** A running duel. */
    public static final class Duel {
        final UUID first;
        final UUID second;
        final World world;
        final double centerX;
        final double centerZ;
        final double size;
        final Location returnFirst;
        final Location returnSecond;
        boolean fighting;
        BukkitTask task;

        Duel(UUID first, UUID second, World world, double centerX, double centerZ, double size,
             Location returnFirst, Location returnSecond) {
            this.first = first;
            this.second = second;
            this.world = world;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.size = size;
            this.returnFirst = returnFirst;
            this.returnSecond = returnSecond;
        }

        public UUID opponentOf(UUID player) {
            return player.equals(first) ? second : first;
        }

        public boolean isFighting() {
            return fighting;
        }

        /** Is this location inside the arena walls? */
        public boolean contains(Location loc) {
            double half = size / 2 - 0.3;
            return loc.getWorld() != null && loc.getWorld().equals(world)
                    && Math.abs(loc.getX() - centerX) <= half && Math.abs(loc.getZ() - centerZ) <= half;
        }

        Location returnLocationOf(UUID player) {
            return player.equals(first) ? returnFirst : returnSecond;
        }
    }

    private final KamiCore plugin;
    private final Map<UUID, Duel> byPlayer = new HashMap<>();
    /** target -> (challenger -> time of request) */
    private final Map<UUID, LinkedHashMap<UUID, Long>> requests = new HashMap<>();
    /** Losers who died: where they should respawn. */
    private final Map<UUID, Location> respawnAt = new HashMap<>();

    public Duels(KamiCore plugin) {
        this.plugin = plugin;
    }

    public Duel duelOf(Player player) {
        return byPlayer.get(player.getUniqueId());
    }

    public boolean isInDuel(Player player) {
        return byPlayer.containsKey(player.getUniqueId());
    }

    // ================================================================ requests

    public void challenge(Player challenger, Player target) {
        if (challenger.equals(target)) {
            Msg.send(challenger, "<red>You can't fight yourself... that's a training session.");
            return;
        }
        if (isInDuel(challenger) || isInDuel(target)) {
            Msg.send(challenger, "<red>One of you is already in a duel.");
            return;
        }
        if (hasRequest(challenger.getUniqueId(), target.getUniqueId())) {
            Msg.send(challenger, "<gray>You already challenged <yellow>" + target.getName() + "</yellow>.");
            return;
        }
        if (!plugin.cooldowns().checkReady(challenger, "duel", "Challenging")) {
            return;
        }

        requests.computeIfAbsent(target.getUniqueId(), k -> new LinkedHashMap<>()).put(challenger.getUniqueId(), System.currentTimeMillis());
        plugin.cooldowns().start(challenger, "duel", plugin.getConfig().getLong("duel.cooldown", 30));
        int size = arenaSize();
        long timeout = plugin.getConfig().getLong("duel.request-timeout", 60);

        Msg.send(challenger, "<gold>You challenged <yellow>" + target.getName() + "</yellow> to a duel!</gold> <gray>(expires in " + timeout + "s)");
        Msg.send(target, "<yellow>" + challenger.getName() + "</yellow> <gold>challenges you to a duel in a " + size + "×" + size + " arena!");
        String name = challenger.getName();
        Msg.raw(target, "   <click:run_command:'/duel accept " + name + "'><hover:show_text:'Fight!'><green><b>[ACCEPT]</b></green></hover></click>"
                + "   <click:run_command:'/duel deny " + name + "'><hover:show_text:'Not now'><red><b>[DENY]</b></red></hover></click>");
        target.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 1.6f);
    }

    private boolean hasRequest(UUID challenger, UUID target) {
        LinkedHashMap<UUID, Long> map = requests.get(target);
        if (map == null) {
            return false;
        }
        long timeout = plugin.getConfig().getLong("duel.request-timeout", 60) * 1000L;
        map.values().removeIf(time -> System.currentTimeMillis() - time > timeout);
        return map.containsKey(challenger);
    }

    /** Newest valid challenger of this target, or null. */
    public Player newestChallenger(Player target) {
        LinkedHashMap<UUID, Long> map = requests.get(target.getUniqueId());
        if (map == null) {
            return null;
        }
        Player newest = null;
        for (UUID id : List.copyOf(map.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && hasRequest(id, target.getUniqueId())) {
                newest = p;
            }
        }
        return newest;
    }

    public void accept(Player target, Player challenger) {
        if (challenger == null || !hasRequest(challenger.getUniqueId(), target.getUniqueId())) {
            Msg.send(target, "<red>You have no active challenge" + (challenger == null ? "" : " from " + challenger.getName()) + ".");
            return;
        }
        requests.get(target.getUniqueId()).remove(challenger.getUniqueId());
        if (isInDuel(challenger) || isInDuel(target)) {
            Msg.send(target, "<red>One of you is already in a duel.");
            return;
        }
        start(challenger, target);
    }

    public void deny(Player target, Player challenger) {
        if (challenger == null || !hasRequest(challenger.getUniqueId(), target.getUniqueId())) {
            Msg.send(target, "<red>You have no active challenge to deny.");
            return;
        }
        requests.get(target.getUniqueId()).remove(challenger.getUniqueId());
        Msg.send(target, "<gray>You declined the duel.");
        Msg.send(challenger, "<yellow>" + target.getName() + "</yellow> <gray>declined your duel.");
    }

    // ================================================================ the duel

    private int arenaSize() {
        return Math.max(10, Math.min(200, plugin.getConfig().getInt("duel.arena-size", 30)));
    }

    private void start(Player first, Player second) {
        World world = first.getWorld();
        Location center = first.getLocation();
        double size = arenaSize();
        double cx = center.getBlockX() + 0.5;
        double cz = center.getBlockZ() + 0.5;
        double offset = Math.max(3, size / 2 - 4);

        Location returnFirst = first.getLocation();
        Location returnSecond = second.getLocation();

        // Clean up anything that would get in the way
        for (Player p : List.of(first, second)) {
            plugin.kiAttacks().cancelCharge(p, null);
            plugin.gravity().fail(p, null);
            plugin.nimbus().dismount(p, false);
        }

        // Spawn points on opposite sides, facing each other
        first.teleport(surface(world, cx - offset, cz, -90));
        second.teleport(surface(world, cx + offset, cz, 90));

        Duel duel = new Duel(first.getUniqueId(), second.getUniqueId(), world, cx, cz, size, returnFirst, returnSecond);
        byPlayer.put(first.getUniqueId(), duel);
        byPlayer.put(second.getUniqueId(), duel);

        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(cx, cz);
        border.setSize(size);
        border.setWarningDistance(0);
        border.setDamageAmount(0);
        border.setDamageBuffer(0);
        for (Player p : List.of(first, second)) {
            p.setWorldBorder(border);
            if (plugin.getConfig().getBoolean("duel.heal-on-start", true)) {
                AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
                p.setHealth(max != null ? max.getValue() : 20.0);
                p.setFoodLevel(20);
                p.setSaturation(20f);
                p.setFireTicks(0);
            }
        }

        Bukkit.getServer().sendMessage(MiniMessage.miniMessage().deserialize(
                "<gold>⚔ <yellow>" + first.getName() + "</yellow> and <yellow>" + second.getName() + "</yellow> have entered the arena!"));

        int countdown = Math.max(0, plugin.getConfig().getInt("duel.countdown", 3));
        long maxTicks = plugin.getConfig().getLong("duel.max-minutes", 10) * 60L * 20L;
        MiniMessage mm = MiniMessage.miniMessage();

        duel.task = new BukkitRunnable() {
            int secondsToStart = countdown;
            long elapsed = 0;

            @Override
            public void run() {
                if (!duel.fighting) {
                    if (secondsToStart > 0) {
                        title(duel, mm.deserialize("<gold><b>" + secondsToStart + "</b>"), mm.deserialize("<gray>Get ready..."));
                        sound(duel, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f);
                        secondsToStart--;
                    } else {
                        duel.fighting = true;
                        title(duel, mm.deserialize("<red><b>FIGHT!</b>"), mm.deserialize("<gray>Only one walks out"));
                        sound(duel, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f);
                    }
                    return;
                }
                elapsed += 20;
                if (maxTicks > 0 && elapsed >= maxTicks) {
                    end(duel, null, "Time ran out");
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private static Location surface(World world, double x, double z, float yaw) {
        int y = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1;
        return new Location(world, x, y, z, yaw, 0);
    }

    private void title(Duel duel, net.kyori.adventure.text.Component main, net.kyori.adventure.text.Component sub) {
        Title title = Title.title(main, sub, Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(200)));
        for (UUID id : List.of(duel.first, duel.second)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.showTitle(title);
            }
        }
    }

    private void sound(Duel duel, Sound sound, float pitch) {
        for (UUID id : List.of(duel.first, duel.second)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.playSound(p.getLocation(), sound, 1.0f, pitch);
            }
        }
    }

    /**
     * Ends a duel. winner = null means a draw.
     * The loser (if they died) respawns where they were before the duel.
     */
    public void end(Duel duel, UUID winner, String reason) {
        if (byPlayer.remove(duel.first) == null) {
            return; // already ended
        }
        byPlayer.remove(duel.second);
        if (duel.task != null) {
            duel.task.cancel();
        }

        Player first = Bukkit.getPlayer(duel.first);
        Player second = Bukkit.getPlayer(duel.second);
        for (Player p : new Player[]{first, second}) {
            if (p != null) {
                p.setWorldBorder(null); // back to the normal world border
            }
        }

        String message;
        if (winner == null) {
            message = "<gray>⚔ The duel between <yellow>" + name(duel.first) + "</yellow> and <yellow>" + name(duel.second)
                    + "</yellow> ended in a draw. <dark_gray>(" + reason + ")";
        } else {
            UUID loser = duel.opponentOf(winner);
            message = "<gold>⚔ <yellow><b>" + name(winner) + "</b></yellow> defeated <yellow>" + name(loser) + "</yellow> in a duel!</gold> <dark_gray>(" + reason + ")";
            Player winnerPlayer = Bukkit.getPlayer(winner);
            if (winnerPlayer != null) {
                winnerPlayer.showTitle(Title.title(MiniMessage.miniMessage().deserialize("<gold><b>VICTORY</b>"),
                        MiniMessage.miniMessage().deserialize("<gray>You won the duel")));
                winnerPlayer.playSound(winnerPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }
        Bukkit.getServer().sendMessage(MiniMessage.miniMessage().deserialize(message));

        // Send the survivors home after a short moment
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID id : List.of(duel.first, duel.second)) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null && !p.isDead()) {
                        p.teleport(duel.returnLocationOf(id));
                    }
                }
            }
        }.runTaskLater(plugin, 60L);
    }

    /** Called when a duelist dies: the opponent wins. */
    public void onDeath(Player loser) {
        Duel duel = duelOf(loser);
        if (duel == null) {
            return;
        }
        respawnAt.put(loser.getUniqueId(), duel.returnLocationOf(loser.getUniqueId()));
        end(duel, duel.opponentOf(loser.getUniqueId()), "Knockout");
    }

    /** Called when a duelist logs out: they forfeit. */
    public void onQuit(Player quitter) {
        requests.remove(quitter.getUniqueId());
        Duel duel = duelOf(quitter);
        if (duel != null) {
            end(duel, duel.opponentOf(quitter.getUniqueId()), name(quitter.getUniqueId()) + " fled");
        }
    }

    public void forfeit(Player player) {
        Duel duel = duelOf(player);
        if (duel == null) {
            Msg.send(player, "<gray>You're not in a duel.");
            return;
        }
        end(duel, duel.opponentOf(player.getUniqueId()), player.getName() + " gave up");
    }

    /** Where a dead duel loser should respawn (and forget it), or null. */
    public Location takeRespawn(Player player) {
        return respawnAt.remove(player.getUniqueId());
    }

    public void endAll() {
        for (Duel duel : List.copyOf(byPlayer.values())) {
            end(duel, null, "Server stopping");
            for (UUID id : List.of(duel.first, duel.second)) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    p.teleport(duel.returnLocationOf(id));
                }
            }
        }
    }

    private static String name(UUID id) {
        Player p = Bukkit.getPlayer(id);
        if (p != null) {
            return p.getName();
        }
        String offline = Bukkit.getOfflinePlayer(id).getName();
        return offline != null ? offline : "someone";
    }
}
