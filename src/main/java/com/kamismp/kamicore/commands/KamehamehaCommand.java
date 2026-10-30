package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * /kamehameha - start charging. The longer you charge, the stronger the beam.
 * Fire it by: releasing Shift (if you were holding it), typing /kamehameha again,
 * or holding a full charge too long (it fires by itself).
 *
 * Charge levels are defined in config.yml under kamehameha.levels.
 */
public final class KamehamehaCommand implements BasicCommand {

    private static final String COOLDOWN_KEY = "kamehameha";
    private static final String[] CHANT = {"KA...", "KA... ME...", "KA... ME... HA...", "KA... ME... HA... ME..."};
    private static final Color BEAM_COLOR = Color.fromRGB(90, 190, 255);

    /** One charge level read from config. */
    private record Level(int number, double chargeSeconds, double damage, double range,
                         double radius, double knockback, long cooldown) {
    }

    /** A player who is charging right now. */
    private static final class Charge {
        BukkitTask task;
        int ticks;
        int levelIndex;
        boolean sawSneak;
    }

    private final KamiCore plugin;
    private final Map<UUID, Charge> charging = new HashMap<>();

    public KamehamehaCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            Msg.playersOnly(source.getSender());
            return;
        }

        // Second use while charging = FIRE
        if (isCharging(player)) {
            fire(player);
            return;
        }

        List<Level> levels = loadLevels();
        if (levels.isEmpty()) {
            Msg.send(player, "<red>No Kamehameha levels are set up in config.yml.");
            return;
        }
        if (plugin.gravity().isTraining(player)) {
            Msg.send(player, "<red>You can't focus your ki under this much gravity!");
            return;
        }
        if (!plugin.cooldowns().checkReady(player, COOLDOWN_KEY, "Kamehameha")) {
            return;
        }
        startCharging(player, levels);
    }

    public boolean isCharging(Player player) {
        return charging.containsKey(player.getUniqueId());
    }

    private void startCharging(Player player, List<Level> levels) {
        Level max = levels.get(levels.size() - 1);
        double maxHoldSeconds = plugin.getConfig().getDouble("kamehameha.max-hold", 4);

        Charge charge = new Charge();
        charging.put(player.getUniqueId(), charge);

        // You stand your ground while charging
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 1, false, false, false));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 1.2f);
        Msg.send(player, "<aqua>You cup your hands and start charging...</aqua> <gray>Release <yellow>Shift</yellow> or type <yellow>/kamehameha</yellow> again to fire.");

        charge.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancelCharge(player, null);
                    return;
                }

                charge.ticks += 2;
                double seconds = charge.ticks / 20.0;

                // Reached a new level?
                int newIndex = charge.levelIndex;
                for (int i = 0; i < levels.size(); i++) {
                    if (seconds >= levels.get(i).chargeSeconds()) {
                        newIndex = i;
                    }
                }
                if (newIndex > charge.levelIndex) {
                    charge.levelIndex = newIndex;
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.8f + newIndex * 0.3f);
                }

                // Release Shift = fire
                if (player.isSneaking()) {
                    charge.sawSneak = true;
                } else if (charge.sawSneak) {
                    fire(player);
                    return;
                }

                // Held a full charge too long = it fires by itself
                if (seconds >= max.chargeSeconds() + maxHoldSeconds) {
                    fire(player);
                    return;
                }

                drawChargeBall(player, charge.levelIndex, levels.size());
                Msg.actionBar(player, chargeBar(charge.levelIndex, levels.size()));
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /** Releases the beam. */
    private void fire(Player player) {
        Charge charge = charging.remove(player.getUniqueId());
        if (charge == null) {
            return;
        }
        charge.task.cancel();
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        List<Level> levels = loadLevels();
        if (levels.isEmpty()) {
            return;
        }
        Level level = levels.get(Math.min(charge.levelIndex, levels.size() - 1));
        boolean isMax = level.number() == levels.size();

        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Location start = eye.clone().add(direction.clone().multiply(0.8)).add(0, -0.3, 0);

        // Find the first living thing (or block) in the beam's path
        double beamWidth = 0.3 + level.number() * 0.15;
        RayTraceResult hit = world.rayTrace(start, direction, level.range(), FluidCollisionMode.NEVER, true, beamWidth,
                entity -> entity instanceof LivingEntity && !(entity instanceof ArmorStand) && !entity.equals(player));
        Location end = hit != null
                ? hit.getHitPosition().toLocation(world)
                : start.clone().add(direction.clone().multiply(level.range()));

        drawBeam(start, end, level.number());
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.3f - level.number() * 0.1f);

        // Damage: full damage to the direct hit, 60% to everything in the blast radius
        int hits = 0;
        Entity direct = hit != null ? hit.getHitEntity() : null;
        if (direct instanceof LivingEntity target) {
            hitEntity(target, player, level.damage(), direction, level.knockback());
            hits++;
        }
        if (level.radius() > 0) {
            for (LivingEntity nearby : world.getNearbyLivingEntities(end, level.radius())) {
                if (nearby.equals(player) || nearby.equals(direct) || nearby instanceof ArmorStand) {
                    continue;
                }
                if (nearby.getLocation().distance(end) <= level.radius() + 1) {
                    hitEntity(nearby, player, level.damage() * 0.6, direction, level.knockback() * 0.6);
                    hits++;
                }
            }
            world.spawnParticle(level.radius() >= 3 ? Particle.EXPLOSION_EMITTER : Particle.EXPLOSION, end, 1, 0, 0, 0, 0);
            world.playSound(end, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        }

        plugin.cooldowns().start(player, COOLDOWN_KEY, level.cooldown());
        String shout = isMax ? "<aqua><b>HAAAAAAAAAAAA!!!</b></aqua>" : "<aqua><b>HAAAA!</b></aqua>";
        Msg.send(player, shout + " <gray>Kamehameha level <yellow>" + level.number() + "</yellow>"
                + (hits > 0 ? " hit <yellow>" + hits + "</yellow> target" + (hits == 1 ? "" : "s") + "." : " missed."));
    }

    private void hitEntity(LivingEntity target, Player attacker, double damage, Vector direction, double knockback) {
        target.damage(damage, attacker);
        if (knockback > 0) {
            Vector push = direction.clone().multiply(knockback);
            push.setY(Math.max(0.25, push.getY()));
            target.setVelocity(push);
        }
    }

    /** Stops charging without firing (no cooldown). */
    public void cancelCharge(Player player, String message) {
        Charge charge = charging.remove(player.getUniqueId());
        if (charge == null) {
            return;
        }
        charge.task.cancel();
        if (player.isOnline()) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            if (message != null) {
                Msg.send(player, message);
            }
        }
    }

    public void cancelAll() {
        for (Charge charge : charging.values()) {
            charge.task.cancel();
        }
        charging.clear();
    }

    // ------------------------------------------------------------- visuals

    private void drawChargeBall(Player player, int levelIndex, int levelCount) {
        Location eye = player.getEyeLocation();
        Location hands = eye.clone().add(eye.getDirection().multiply(0.7)).add(0, -0.5, 0);
        float size = 0.8f + levelIndex * 0.5f;
        int count = 4 + levelIndex * 4;
        double spread = 0.08 + levelIndex * 0.06;
        player.getWorld().spawnParticle(Particle.DUST, hands, count, spread, spread, spread, 0.0,
                new Particle.DustOptions(BEAM_COLOR, size));
        if (levelIndex == levelCount - 1) {
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hands, 3, 0.2, 0.2, 0.2, 0.05);
        }
    }

    private void drawBeam(Location start, Location end, int levelNumber) {
        World world = start.getWorld();
        Vector step = end.toVector().subtract(start.toVector());
        double length = step.length();
        if (length < 0.1) {
            return;
        }
        step.normalize().multiply(0.4);
        float size = 1.0f + levelNumber * 0.5f;
        double spread = 0.05 + levelNumber * 0.06;
        Particle.DustOptions blue = new Particle.DustOptions(BEAM_COLOR, size);

        Location point = start.clone();
        for (double travelled = 0; travelled < length; travelled += 0.4) {
            world.spawnParticle(Particle.DUST, point, 3, spread, spread, spread, 0.0, blue);
            world.spawnParticle(Particle.END_ROD, point, 1, 0.02, 0.02, 0.02, 0.0);
            point.add(step);
        }
    }

    private static String chargeBar(int levelIndex, int levelCount) {
        int filled = (int) Math.round((levelIndex + 1) * 10.0 / levelCount);
        String chant = CHANT[Math.min(levelIndex, CHANT.length - 1)];
        return "<aqua><b>" + chant + "</b></aqua> <blue>" + "█".repeat(filled) + "</blue><dark_gray>"
                + "█".repeat(10 - filled) + "</dark_gray> <gray>Lv " + (levelIndex + 1) + "/" + levelCount;
    }

    // -------------------------------------------------------------- config

    /** Reads kamehameha.levels from config.yml, sorted by charge time. */
    private List<Level> loadLevels() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("kamehameha.levels");
        List<Level> levels = new ArrayList<>();
        if (section == null) {
            return levels;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            levels.add(new Level(0,
                    s.getDouble("charge", 0),
                    s.getDouble("damage", 4),
                    s.getDouble("range", 20),
                    s.getDouble("radius", 0),
                    s.getDouble("knockback", 0.5),
                    s.getLong("cooldown", 30)));
        }
        levels.sort(Comparator.comparingDouble(Level::chargeSeconds));

        // Number them 1, 2, 3... after sorting
        List<Level> numbered = new ArrayList<>();
        for (int i = 0; i < levels.size(); i++) {
            Level l = levels.get(i);
            numbered.add(new Level(i + 1, l.chargeSeconds(), l.damage(), l.range(), l.radius(), l.knockback(), l.cooldown()));
        }
        return numbered;
    }

    @Override
    public String permission() {
        return "kamicore.kamehameha";
    }
}
