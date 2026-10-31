package com.kamismp.kamicore.dragonballs;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Cooldowns;
import com.kamismp.kamicore.util.Msg;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

/**
 * The 7 Dragon Balls.
 *
 * Life cycle:
 *  1. SCATTERED: each ball floats somewhere in the world (a glowing item display
 *     + an invisible "interaction" box you can right-click).
 *  2. Players collect them into their inventory (they can be stolen, dropped, stored).
 *  3. Whoever holds all 7 uses /shenron and makes a wish.
 *  4. The balls turn to stone (INERT) for a day, then scatter again.
 *
 * Every scatter starts a new "cycle" id. Balls from an old cycle are worthless,
 * which stops duplicated or leftover balls from being used.
 * State is saved in plugins/KamiCore/dragonballs.yml.
 */
public final class DragonBalls {

    public static final int COUNT = 7;

    private final KamiCore plugin;
    private final File file;
    private YamlConfiguration yaml = new YamlConfiguration();
    private final NamespacedKey starKey;
    private final NamespacedKey cycleKey;
    private BukkitTask task;

    public DragonBalls(KamiCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "dragonballs.yml");
        this.starKey = new NamespacedKey(plugin, "dragonball_star");
        this.cycleKey = new NamespacedKey(plugin, "dragonball_cycle");
    }

    // =========================================================== saving state

    public void load() {
        if (file.exists()) {
            yaml = YamlConfiguration.loadConfiguration(file);
        }
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save dragonballs.yml", e);
        }
    }

    public String cycle() {
        return yaml.getString("cycle", "");
    }

    public boolean isInert() {
        return "INERT".equals(yaml.getString("state"));
    }

    /** When the balls come back (0 if they are active). */
    public long returnsAt() {
        return yaml.getLong("next-scatter", 0);
    }

    private String status(int star) {
        return yaml.getString("balls." + star + ".status", "NONE");
    }

    private void setStatus(int star, String status) {
        yaml.set("balls." + star + ".status", status);
    }

    private Location storedLocation(int star) {
        ConfigurationSection s = yaml.getConfigurationSection("balls." + star);
        if (s == null || !s.contains("world")) {
            return null;
        }
        World world = Bukkit.getWorld(s.getString("world", ""));
        return world == null ? null : new Location(world, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"));
    }

    // ============================================================ the loop

    /** Called on startup. Scatters the balls the first time, and checks the timer every minute. */
    public void start() {
        if (cycle().isEmpty()) {
            scatterAll(false);
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (isInert() && System.currentTimeMillis() >= returnsAt()) {
                scatterAll(true);
            }
            sparkle();
        }, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        save();
    }

    /** Small orange sparkles around scattered balls when someone is close. */
    private void sparkle() {
        Particle.DustOptions orange = new Particle.DustOptions(Color.fromRGB(255, 150, 20), 1.2f);
        for (int star = 1; star <= COUNT; star++) {
            if (!"SCATTERED".equals(status(star))) {
                continue;
            }
            Location loc = storedLocation(star);
            if (loc == null || !loc.isChunkLoaded() || loc.getWorld().getNearbyPlayers(loc, 24).isEmpty()) {
                continue;
            }
            loc.getWorld().spawnParticle(Particle.DUST, loc, 6, 0.3, 0.3, 0.3, 0, orange);
            loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0.2, 0.4, 0.2, 0.01);
        }
    }

    // ============================================================ scattering

    /** Starts a new cycle and hides all 7 balls around the world. */
    public void scatterAll(boolean announce) {
        yaml.set("cycle", UUID.randomUUID().toString());
        yaml.set("state", "ACTIVE");
        yaml.set("next-scatter", null);
        for (int star = 1; star <= COUNT; star++) {
            placeBall(star, 0);
        }
        save();
        if (announce) {
            broadcast("<gold>The <b>Dragon Balls</b> have scattered across the world!</gold> <gray>Use <yellow>/scouter dragonballs</yellow> to track them.");
        }
    }

    /** Picks a random spot on the surface and spawns the ball there. Retries if it lands in water/lava. */
    private void placeBall(int star, int attempt) {
        World world = Bukkit.getWorld(plugin.getConfig().getString("dragonballs.world", "world"));
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        double radius = plugin.getConfig().getDouble("dragonballs.radius", 1500);
        double minDistance = plugin.getConfig().getDouble("dragonballs.min-distance", 150);
        Location center = world.getSpawnLocation();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        double angle = random.nextDouble() * Math.PI * 2;
        double distance = minDistance + random.nextDouble() * Math.max(1, radius - minDistance);
        int x = (int) (center.getX() + Math.cos(angle) * distance);
        int z = (int) (center.getZ() + Math.sin(angle) * distance);

        setStatus(star, "PLACING");
        String cycleAtStart = cycle();
        World finalWorld = world;

        // Load the chunk without freezing the server, then place the ball
        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            if (!cycle().equals(cycleAtStart)) {
                return; // a new scatter started meanwhile
            }
            int y = finalWorld.getHighestBlockYAt(x, z);
            Block top = finalWorld.getBlockAt(x, y, z);
            Location spot = new Location(finalWorld, x + 0.5, y + 1.6, z + 0.5);
            boolean badSpot = top.isLiquid() || !finalWorld.getWorldBorder().isInside(spot);
            if (badSpot && attempt < 20) {
                placeBall(star, attempt + 1);
                return;
            }
            spawnBallEntities(star, spot);
            yaml.set("balls." + star + ".status", "SCATTERED");
            yaml.set("balls." + star + ".world", finalWorld.getName());
            yaml.set("balls." + star + ".x", spot.getX());
            yaml.set("balls." + star + ".y", spot.getY());
            yaml.set("balls." + star + ".z", spot.getZ());
            save();
        });
    }

    private void spawnBallEntities(int star, Location spot) {
        World world = spot.getWorld();
        ItemDisplay display = world.spawn(spot, ItemDisplay.class, d -> {
            d.setItemStack(createBall(star));
            d.setBillboard(Display.Billboard.CENTER);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setGlowing(true);
            d.setGlowColorOverride(Color.fromRGB(255, 140, 0));
            d.setPersistent(true);
            tag(d.getPersistentDataContainer(), star);
        });
        world.spawn(spot.clone().add(0, -0.45, 0), Interaction.class, i -> {
            i.setInteractionWidth(0.9f);
            i.setInteractionHeight(0.9f);
            i.setResponsive(true);
            i.setPersistent(true);
            tag(i.getPersistentDataContainer(), star);
        });
        if (display.isDead()) {
            plugin.getLogger().warning("Dragon Ball " + star + " could not be spawned.");
        }
    }

    private void tag(PersistentDataContainer pdc, int star) {
        pdc.set(starKey, PersistentDataType.INTEGER, star);
        pdc.set(cycleKey, PersistentDataType.STRING, cycle());
    }

    // ============================================================ collecting

    /** Is this entity (display or interaction) part of a Dragon Ball? */
    public boolean isBallEntity(Entity entity) {
        return entity.getPersistentDataContainer().has(starKey, PersistentDataType.INTEGER);
    }

    /** Should this ball entity still exist? (used when chunks load) */
    public boolean isValidBallEntity(Entity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        Integer star = pdc.get(starKey, PersistentDataType.INTEGER);
        String entityCycle = pdc.get(cycleKey, PersistentDataType.STRING);
        if (star == null || !cycle().equals(entityCycle) || !"SCATTERED".equals(status(star))) {
            return false;
        }
        Location stored = storedLocation(star);
        return stored != null && stored.getWorld().equals(entity.getWorld())
                && stored.distanceSquared(entity.getLocation()) < 4;
    }

    /** A player right-clicked a ball in the world. */
    public void collect(Player player, Entity clicked) {
        Integer star = clicked.getPersistentDataContainer().get(starKey, PersistentDataType.INTEGER);
        if (star == null) {
            return;
        }
        removeBallEntitiesNear(clicked.getLocation(), star);
        if (!isValidBallEntity(clicked)) {
            Msg.send(player, "<gray>This Dragon Ball crumbles to stone in your hands...");
            return;
        }

        setStatus(star, "HELD");
        save();
        give(player, createBall(star));
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.6f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);

        int held = heldBy(player).size();
        broadcast("<yellow>" + player.getName() + "</yellow> <gold>found the " + stars(star) + " Dragon Ball!</gold> <gray>(they hold " + held + "/7)");
        if (held == COUNT) {
            Msg.send(player, "<green><b>You have all 7 Dragon Balls!</b></green> <gray>Type <yellow>/shenron</yellow> to summon the Eternal Dragon.");
        }
    }

    private void removeBallEntitiesNear(Location location, int star) {
        for (Entity entity : location.getWorld().getNearbyEntities(location, 2, 2, 2)) {
            Integer entityStar = entity.getPersistentDataContainer().get(starKey, PersistentDataType.INTEGER);
            if (entityStar != null && entityStar == star) {
                entity.remove();
            }
        }
    }

    /** A ball item fell into the void or was destroyed: hide it somewhere new. */
    public void ballItemLost(ItemStack item) {
        Integer star = starOf(item);
        if (star == null || !isCurrent(item) || isInert()) {
            return;
        }
        placeBall(star, 0);
        broadcast("<gold>The " + stars(star) + " Dragon Ball flew off to a new hiding place!");
    }

    // ================================================================= items

    /** The Dragon Ball item: an orange orb with a glow and the number of stars in its name. */
    public ItemStack createBall(int star) {
        ItemStack item = new ItemStack(Material.MAGMA_CREAM);
        ItemMeta meta = item.getItemMeta();
        MiniMessage mm = MiniMessage.miniMessage();
        meta.displayName(mm.deserialize("<!italic><gradient:#ffc400:#ff7b00><b>Dragon Ball " + stars(star) + "</b></gradient>"));
        meta.lore(List.of(
                mm.deserialize("<!italic><gray>A shining orange orb with <gold>" + star + "</gold> red star" + (star == 1 ? "" : "s") + "."),
                mm.deserialize("<!italic><gray>Gather all 7 and use <yellow>/shenron</yellow>"),
                mm.deserialize("<!italic><gray>to summon the Eternal Dragon.")));
        meta.setEnchantmentGlintOverride(true);
        meta.setMaxStackSize(1);
        tag(meta.getPersistentDataContainer(), star);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isBall(ItemStack item) {
        return starOf(item) != null;
    }

    public Integer starOf(ItemStack item) {
        if (item == null || item.getType() != Material.MAGMA_CREAM || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(starKey, PersistentDataType.INTEGER);
    }

    public boolean isCurrent(ItemStack item) {
        return item != null && item.hasItemMeta()
                && cycle().equals(item.getItemMeta().getPersistentDataContainer().get(cycleKey, PersistentDataType.STRING));
    }

    /** The star numbers of the valid balls this player carries. */
    public TreeSet<Integer> heldBy(Player player) {
        TreeSet<Integer> stars = new TreeSet<>();
        for (ItemStack item : player.getInventory().getContents()) {
            Integer star = starOf(item);
            if (star != null && isCurrent(item)) {
                stars.add(star);
            }
        }
        return stars;
    }

    /** Removes balls from an old cycle (they turned to stone). */
    public void purgeStale(Player player) {
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isBall(contents[i]) && !isCurrent(contents[i])) {
                player.getInventory().setItem(i, null);
                removed++;
            }
        }
        if (removed > 0) {
            Msg.send(player, "<gray>" + removed + " old Dragon Ball" + (removed == 1 ? "" : "s") + " in your bag turned to stone and crumbled.");
        }
    }

    /** Takes one of each star out of the player's inventory. */
    public void takeAll(Player player) {
        boolean[] taken = new boolean[COUNT + 1];
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            Integer star = starOf(contents[i]);
            if (star != null && star >= 1 && star <= COUNT && isCurrent(contents[i]) && !taken[star]) {
                taken[star] = true;
                player.getInventory().setItem(i, null);
            }
        }
    }

    private void give(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack rest : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }
    }

    // ================================================================ wishing

    /** After a wish: the balls turn to stone and come back later. */
    public void wishGranted() {
        long returnAfter = plugin.getConfig().getLong("dragonballs.return-after", 86400);
        yaml.set("state", "INERT");
        yaml.set("next-scatter", System.currentTimeMillis() + returnAfter * 1000L);
        for (int star = 1; star <= COUNT; star++) {
            setStatus(star, "INERT");
        }
        yaml.set("cycle", UUID.randomUUID().toString()); // every leftover ball is now worthless
        save();
        broadcast("<gray>The Dragon Balls rise into the sky and scatter as stone... <gold>They will return in "
                + Cooldowns.format(returnAfter * 1000L) + ".");
    }

    // ================================================================ radar & admin

    /** Locations of all balls lying in the given world. */
    public List<Location> scatteredIn(World world) {
        List<Location> result = new ArrayList<>();
        for (int star = 1; star <= COUNT; star++) {
            Location loc = storedLocation(star);
            if ("SCATTERED".equals(status(star)) && loc != null && loc.getWorld().equals(world)) {
                result.add(loc);
            }
        }
        return result;
    }

    public int countWithStatus(String wanted) {
        int count = 0;
        for (int star = 1; star <= COUNT; star++) {
            if (wanted.equals(status(star))) {
                count++;
            }
        }
        return count;
    }

    /** One line per ball for /dragonballs status. */
    public List<String> statusLines() {
        List<String> lines = new ArrayList<>();
        for (int star = 1; star <= COUNT; star++) {
            String status = status(star);
            Location loc = storedLocation(star);
            String where = "SCATTERED".equals(status) && loc != null
                    ? " <aqua>" + loc.getWorld().getName() + " " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                    : "";
            lines.add(" <gold>" + stars(star) + "</gold> <gray>" + status.toLowerCase() + where);
        }
        return lines;
    }

    /** Admin: puts all 7 current balls into a player's inventory (for testing). */
    public void giveAll(Player player) {
        if (isInert()) {
            scatterAll(false);
        }
        for (int star = 1; star <= COUNT; star++) {
            Location loc = storedLocation(star);
            if (loc != null && loc.isChunkLoaded()) {
                removeBallEntitiesNear(loc, star);
            }
            setStatus(star, "HELD");
            give(player, createBall(star));
        }
        save();
    }

    public static String stars(int count) {
        return "★".repeat(Math.max(1, count));
    }

    private static void broadcast(String message) {
        Component prefixed = MiniMessage.miniMessage().deserialize(message);
        Bukkit.getServer().sendMessage(prefixed);
    }
}
