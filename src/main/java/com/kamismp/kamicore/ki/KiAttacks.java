package com.kamismp.kamicore.ki;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.saiyan.SaiyanForm;
import com.kamismp.kamicore.util.Msg;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
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
 * The engine behind every ki attack: charging, firing, damage, visuals and craters.
 *
 * How charging works (same for every attack):
 *  - type the command to start charging
 *  - the longer you charge, the higher the level (levels come from config.yml)
 *  - fire by releasing Shift (if you held it), typing the command again,
 *    or holding a full charge for too long
 */
public final class KiAttacks {

    /** One charge level read from config.yml. */
    public record Level(int number, double chargeSeconds, double damage, double range, double radius,
                        double knockback, double crater, long cooldown) {
    }

    /**
     * How much a player's state boosts their ki attacks right now.
     * Super Saiyan forms and Kaioken multiply together.
     *  damage = damage multiplier, size = beams/balls/craters/blasts, range = how far it travels,
     *  aura   = extra outer color layer from the Super Saiyan form (null in base form)
     */
    public record Power(double damage, double size, double range, Color aura, boolean kaioken) {
        static final Power NORMAL = new Power(1, 1, 1, null, false);

        float particle(float base) {
            return (float) Math.min(4.0, base * size);
        }
    }

    private static final Color KAIOKEN_RED = Color.fromRGB(255, 40, 30);

    /** A player who is charging right now. */
    private static final class Charge {
        KiStyle style;
        List<Level> levels;
        BukkitTask task;
        double ticks;
        int levelIndex;
        boolean sawSneak;
    }

    private final KamiCore plugin;
    private final Destruction destruction;
    private final Map<UUID, Charge> charging = new HashMap<>();
    private final Set<BukkitTask> flyingBombs = new HashSet<>();

    public KiAttacks(KamiCore plugin) {
        this.plugin = plugin;
        this.destruction = new Destruction(plugin);
    }

    // =================================================================== using

    /** Called by the command: start charging, or fire if already charging this attack. */
    public void use(Player player, KiStyle style) {
        Charge current = charging.get(player.getUniqueId());
        if (current != null) {
            if (current.style == style) {
                fire(player);
            } else {
                Msg.send(player, "<red>You're already charging a " + current.style.displayName() + "!");
            }
            return;
        }

        List<Level> levels = loadLevels(style);
        if (levels.isEmpty()) {
            Msg.send(player, "<red>No levels for " + style.displayName() + " are set up in config.yml.");
            return;
        }
        if (plugin.gravity().isTraining(player)) {
            Msg.send(player, "<red>You can't focus your ki under this much gravity!");
            return;
        }
        if (!plugin.cooldowns().checkReady(player, style.id(), style.displayName())) {
            return;
        }
        startCharging(player, style, levels);
    }

    public boolean isCharging(Player player) {
        return charging.containsKey(player.getUniqueId());
    }

    private void startCharging(Player player, KiStyle style, List<Level> levels) {
        Level max = levels.get(levels.size() - 1);
        double maxHold = plugin.getConfig().getDouble("ki-attacks.max-hold", 4);

        Charge charge = new Charge();
        charge.style = style;
        charge.levels = levels;
        charging.put(player.getUniqueId(), charge);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 1, false, false, false));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 1.2f);
        Msg.send(player, "<aqua>You start charging a <b>" + style.displayName() + "</b>...</aqua> <gray>Release <yellow>Shift</yellow> or type <yellow>/"
                + style.id() + "</yellow> again to fire.");
        if (style == KiStyle.SPIRIT_BOMB) {
            Msg.send(player, "<gray>Friends nearby can <yellow>hold Shift</yellow> to lend you their energy and charge it faster!");
        }

        charge.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancelCharge(player, null);
                    return;
                }

                // Spirit Bomb: every friend crouching nearby speeds up the charge by 50%
                int helpers = style == KiStyle.SPIRIT_BOMB ? countHelpers(player) : 0;
                charge.ticks += 2 * (1 + 0.5 * helpers);
                double seconds = charge.ticks / 20.0;

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

                if (player.isSneaking()) {
                    charge.sawSneak = true;
                } else if (charge.sawSneak) {
                    fire(player);
                    return;
                }
                if (seconds >= max.chargeSeconds() + maxHold) {
                    fire(player);
                    return;
                }

                drawCharge(player, style, charge.levelIndex, levels.size(), power(player));
                String extra = helpers > 0 ? " <green>+" + helpers + " helping" : "";
                Msg.actionBar(player, chargeBar(style, charge.levelIndex, levels.size()) + extra);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private int countHelpers(Player player) {
        int helpers = 0;
        for (Player other : player.getWorld().getNearbyPlayers(player.getLocation(), 15)) {
            if (!other.equals(player) && other.isSneaking()) {
                helpers++;
            }
        }
        return helpers;
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
        for (BukkitTask bomb : flyingBombs) {
            bomb.cancel();
        }
        flyingBombs.clear();
    }

    /** Works out the current boost from Super Saiyan + Kaioken. */
    public Power power(Player player) {
        double ssj = plugin.superSaiyan().kiMultiplier(player);
        double kaioken = plugin.kaioken().kiMultiplier(player);
        double max = Math.max(1, plugin.getConfig().getDouble("ki-attacks.max-multiplier", 4.0));
        double total = Math.min(max, ssj * kaioken);
        if (total <= 1.0001) {
            return Power.NORMAL;
        }
        SaiyanForm form = plugin.superSaiyan().current(player);
        return new Power(total, 1 + (total - 1) * 0.6, 1 + (total - 1) * 0.3,
                form != null ? form.auraColor() : null, kaioken > 1);
    }

    // ================================================================== firing

    private void fire(Player player) {
        Charge charge = charging.remove(player.getUniqueId());
        if (charge == null) {
            return;
        }
        charge.task.cancel();
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        Level base = charge.levels.get(Math.min(charge.levelIndex, charge.levels.size() - 1));
        boolean isMax = base.number() == charge.levels.size();
        KiStyle style = charge.style;

        // Super Saiyan / Kaioken make the attack stronger, bigger and longer
        Power power = power(player);
        Level level = new Level(base.number(), base.chargeSeconds(), base.damage() * power.damage(), base.range() * power.range(),
                base.radius() * power.size(), base.knockback() * (1 + (power.size() - 1) * 0.5), base.crater() * power.size(),
                base.cooldown());

        plugin.cooldowns().start(player, style.id(), base.cooldown());
        String shout = isMax ? "<b>" + style.shout() + "</b>" : style.shout();
        String boost = power.damage() > 1 ? " <gold>x" + String.format(java.util.Locale.ROOT, "%.1f", power.damage()) + " power</gold>" : "";
        Msg.send(player, "<aqua>" + shout + "</aqua> <gray>(" + style.displayName() + " level <yellow>" + level.number() + "</yellow>)" + boost);

        switch (style.type()) {
            case BEAM -> fireBeam(player, style, level, power);
            case PIERCE -> firePiercing(player, style, level, power);
            case BOMB -> fireBomb(player, style, level, power);
        }
    }

    /** Kamehameha, Galick Gun, Final Flash: a beam that stops at the first thing it hits. */
    private void fireBeam(Player player, KiStyle style, Level level, Power power) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Location start = eye.clone().add(direction.clone().multiply(0.8)).add(0, -0.3, 0);

        double width = (0.3 + level.number() * 0.15) * power.size();
        RayTraceResult hit = world.rayTrace(start, direction, level.range(), FluidCollisionMode.NEVER, true, width,
                entity -> isTarget(entity, player));
        Location end = hit != null
                ? hit.getHitPosition().toLocation(world)
                : start.clone().add(direction.clone().multiply(level.range()));

        drawBeam(start, end, style, level.number(), power);
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.3f - level.number() * 0.1f);

        Entity direct = hit != null ? hit.getHitEntity() : null;
        double damage = level.damage();
        if (direct instanceof LivingEntity target) {
            hurt(target, player, damage, direction.clone().multiply(level.knockback()));
        }
        if (hit != null) {
            impact(player, end, level, damage, direct);
        }
    }

    /** Special Beam Cannon: drills through everything in a straight line. */
    private void firePiercing(Player player, KiStyle style, Level level, Power power) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Location point = eye.clone().add(direction.clone().multiply(0.6));
        Vector step = direction.clone().multiply(0.5);
        double damage = level.damage();
        double hitRadius = 1.0 * power.size();
        double tunnel = Math.max(0, level.crater());

        Set<UUID> alreadyHit = new HashSet<>();
        Particle.DustOptions core = new Particle.DustOptions(style.coreColor(), power.particle(1.0f));
        Particle.DustOptions spiral = new Particle.DustOptions(style.mainColor(), power.particle(0.9f));
        Particle.DustOptions aura = auraDust(power, 1.0f);

        int hits = 0;
        int steps = (int) (level.range() / 0.5);
        for (int i = 0; i < steps; i++) {
            point.add(step);
            if (point.getY() < world.getMinHeight() || point.getY() > world.getMaxHeight()) {
                break;
            }

            // Drill through blocks; a protected block (obsidian, bedrock...) stops the beam
            Block block = point.getBlock();
            if (!block.isPassable()) {
                if (tunnel > 0) {
                    destruction.crater(point, tunnel);
                }
                if (!block.isPassable()) {
                    break;
                }
            }

            for (LivingEntity target : world.getNearbyLivingEntities(point, hitRadius)) {
                if (isTarget(target, player) && alreadyHit.add(target.getUniqueId())) {
                    hurt(target, player, damage, direction.clone().multiply(level.knockback()));
                    hits++;
                }
            }

            // Straight core + a spiral wrapped around it
            world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, core);
            double angle = i * 0.9;
            Vector offset = perpendicular(direction, angle).multiply(0.35 * power.size());
            world.spawnParticle(Particle.DUST, point.clone().add(offset), 1, 0, 0, 0, 0, spiral);
            if (aura != null && i % 2 == 0) {
                Vector auraOffset = perpendicular(direction, angle + Math.PI).multiply(0.5 * power.size());
                world.spawnParticle(Particle.DUST, point.clone().add(auraOffset), 1, 0, 0, 0, 0, aura);
            }
        }

        world.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0f, 2.0f);
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.8f);
        if (hits > 0) {
            Msg.send(player, "<gray>Pierced <yellow>" + hits + "</yellow> target" + (hits == 1 ? "" : "s") + ".");
        }
    }

    /** Spirit Bomb: a huge ball that flies slowly and explodes on impact. */
    private void fireBomb(Player player, KiStyle style, Level level, Power power) {
        World world = player.getWorld();
        double ballRadius = Math.min(6, bombRadius(level.number() - 1) * power.size());
        Location ball = player.getLocation().add(0, 3 + ballRadius, 0);
        Vector velocity = player.getEyeLocation().getDirection().normalize()
                .multiply(plugin.getConfig().getDouble("spiritbomb.speed", 0.6));
        double damage = level.damage();
        world.playSound(ball, Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.6f);

        BukkitTask[] self = new BukkitTask[1];
        self[0] = new BukkitRunnable() {
            double travelled = 0;

            @Override
            public void run() {
                ball.add(velocity);
                travelled += velocity.length();
                drawSphere(world, ball, ballRadius, 30 + (int) (ballRadius * 15), style.mainColor(), style.coreColor(), power);

                boolean hitBlock = !ball.getBlock().isPassable();
                boolean hitEntity = false;
                for (LivingEntity target : world.getNearbyLivingEntities(ball, ballRadius)) {
                    if (isTarget(target, player)) {
                        hitEntity = true;
                        break;
                    }
                }
                if (hitBlock || hitEntity || travelled >= level.range()
                        || ball.getY() < world.getMinHeight() || ball.getY() > world.getMaxHeight()) {
                    cancel();
                    flyingBombs.remove(self[0]);
                    impact(player, ball, level, damage, null);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        flyingBombs.add(self[0]);
    }

    /** Explosion at the end of an attack: area damage, knockback and crater. */
    private void impact(Player attacker, Location center, Level level, double damage, Entity alreadyHit) {
        World world = center.getWorld();
        if (level.radius() > 0) {
            for (LivingEntity target : world.getNearbyLivingEntities(center, level.radius())) {
                if (!isTarget(target, attacker) || target.equals(alreadyHit)
                        || target.getLocation().distance(center) > level.radius() + 1) {
                    continue;
                }
                Vector away = target.getLocation().toVector().subtract(center.toVector());
                if (away.lengthSquared() < 0.01) {
                    away = new Vector(0, 1, 0);
                }
                hurt(target, attacker, damage * 0.6, away.normalize().multiply(level.knockback()));
            }
        }
        if (level.crater() > 0) {
            destruction.crater(center, level.crater());
        }
        if (level.radius() > 0 || level.crater() > 0) {
            boolean big = level.radius() >= 3 || level.crater() >= 3;
            world.spawnParticle(big ? Particle.EXPLOSION_EMITTER : Particle.EXPLOSION, center, big ? 2 : 1, 0.5, 0.5, 0.5, 0);
            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, big ? 2.0f : 1.0f, big ? 0.7f : 1.0f);
        }
    }

    private void hurt(LivingEntity target, Player attacker, double damage, Vector knockback) {
        target.damage(damage, attacker);
        if (knockback.lengthSquared() > 0) {
            knockback.setY(Math.max(0.25, knockback.getY()));
            target.setVelocity(knockback);
        }
    }

    private static boolean isTarget(Entity entity, Player attacker) {
        return entity instanceof LivingEntity && !(entity instanceof ArmorStand) && !entity.equals(attacker);
    }

    // ================================================================= visuals

    private void drawCharge(Player player, KiStyle style, int levelIndex, int levelCount, Power power) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection();

        switch (style.type()) {
            case BEAM -> {
                Location hands = eye.clone().add(look.multiply(0.7)).add(0, -0.5, 0);
                double spread = (0.08 + levelIndex * 0.06) * power.size();
                world.spawnParticle(Particle.DUST, hands, 4 + levelIndex * 4, spread, spread, spread, 0.0,
                        new Particle.DustOptions(style.mainColor(), power.particle(0.8f + levelIndex * 0.5f)));
                Particle.DustOptions aura = auraDust(power, 1.0f + levelIndex * 0.3f);
                if (aura != null) {
                    world.spawnParticle(Particle.DUST, hands, 3 + levelIndex * 2, spread * 1.6, spread * 1.6, spread * 1.6, 0.0, aura);
                }
                if (levelIndex == levelCount - 1) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, hands, 3, 0.2, 0.2, 0.2, 0.05);
                }
            }
            case PIERCE -> {
                Location forehead = eye.clone().add(look.multiply(0.35)).add(0, 0.15, 0);
                world.spawnParticle(Particle.DUST, forehead, 2 + levelIndex * 2, 0.03, 0.03, 0.03, 0.0,
                        new Particle.DustOptions(style.coreColor(), power.particle(0.6f + levelIndex * 0.2f)));
                world.spawnParticle(Particle.ELECTRIC_SPARK, forehead, 1 + levelIndex, 0.1, 0.1, 0.1, 0.02);
            }
            case BOMB -> {
                double radius = Math.min(6, bombRadius(levelIndex) * power.size());
                Location above = player.getLocation().add(0, 3 + radius, 0);
                drawSphere(world, above, radius, 20 + (int) (radius * 12), style.mainColor(), style.coreColor(), power);
                // Energy flowing up into the ball
                world.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 3, 3, 1, 3, 0.05);
            }
        }
    }

    private static double bombRadius(int levelIndex) {
        return 0.6 + levelIndex * 0.6;
    }

    private void drawBeam(Location start, Location end, KiStyle style, int levelNumber, Power power) {
        World world = start.getWorld();
        Vector step = end.toVector().subtract(start.toVector());
        double length = step.length();
        if (length < 0.1) {
            return;
        }
        step.normalize().multiply(0.4);
        double spread = (0.05 + levelNumber * 0.07) * power.size();
        Particle.DustOptions outer = new Particle.DustOptions(style.mainColor(), power.particle(1.0f + levelNumber * 0.5f));
        Particle.DustOptions inner = new Particle.DustOptions(style.coreColor(), power.particle(0.8f + levelNumber * 0.2f));
        Particle.DustOptions aura = auraDust(power, 1.2f + levelNumber * 0.3f);
        Particle.DustOptions kaioken = power.kaioken() ? new Particle.DustOptions(KAIOKEN_RED, power.particle(1.0f)) : null;

        Location point = start.clone();
        int i = 0;
        for (double travelled = 0; travelled < length; travelled += 0.4, i++) {
            world.spawnParticle(Particle.DUST, point, 3 + (int) power.size(), spread, spread, spread, 0.0, outer);
            world.spawnParticle(Particle.DUST, point, 1, 0.01, 0.01, 0.01, 0.0, inner);
            // Super Saiyan aura and Kaioken red wrap around the outside of the beam
            if (aura != null && i % 2 == 0) {
                world.spawnParticle(Particle.DUST, point, 2, spread * 1.8, spread * 1.8, spread * 1.8, 0.0, aura);
            }
            if (kaioken != null && i % 3 == 0) {
                world.spawnParticle(Particle.DUST, point, 1, spread * 2.2, spread * 2.2, spread * 2.2, 0.0, kaioken);
            }
            point.add(step);
        }
    }

    private static void drawSphere(World world, Location center, double radius, int count, Color main, Color core, Power power) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Particle.DustOptions shell = new Particle.DustOptions(main, 1.6f);
        Particle.DustOptions aura = auraDust(power, 1.4f);
        Particle.DustOptions inside = new Particle.DustOptions(core, 1.2f);
        for (int i = 0; i < count; i++) {
            Vector v = new Vector(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
            if (v.lengthSquared() < 0.0001) {
                continue;
            }
            v.normalize().multiply(radius);
            world.spawnParticle(Particle.DUST, center.clone().add(v), 1, 0, 0, 0, 0, shell);
            if (aura != null && i % 3 == 0) {
                world.spawnParticle(Particle.DUST, center.clone().add(v.multiply(1.15)), 1, 0, 0, 0, 0, aura);
            }
        }
        world.spawnParticle(Particle.DUST, center, Math.max(3, count / 4), radius * 0.4, radius * 0.4, radius * 0.4, 0, inside);
    }

    /** Particle settings for the Super Saiyan aura layer, or null in base form. */
    private static Particle.DustOptions auraDust(Power power, float size) {
        return power.aura() == null ? null : new Particle.DustOptions(power.aura(), power.particle(size));
    }

    /** A unit vector at a right angle to the direction, rotated by the angle (for the spiral). */
    private static Vector perpendicular(Vector direction, double angle) {
        Vector up = Math.abs(direction.getY()) > 0.95 ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        Vector a = direction.clone().crossProduct(up).normalize();
        Vector b = direction.clone().crossProduct(a).normalize();
        return a.multiply(Math.cos(angle)).add(b.multiply(Math.sin(angle)));
    }

    private static String chargeBar(KiStyle style, int levelIndex, int levelCount) {
        int filled = (int) Math.round((levelIndex + 1) * 10.0 / levelCount);
        return "<aqua><b>" + style.chant(levelIndex) + "</b></aqua> <blue>" + "█".repeat(filled) + "</blue><dark_gray>"
                + "█".repeat(10 - filled) + "</dark_gray> <gray>Lv " + (levelIndex + 1) + "/" + levelCount;
    }

    // ================================================================== config

    /** Reads <attack>.levels from config.yml, sorted by charge time. */
    public List<Level> loadLevels(KiStyle style) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(style.id() + ".levels");
        List<Level> raw = new ArrayList<>();
        if (section == null) {
            return raw;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            raw.add(new Level(0, s.getDouble("charge", 0), s.getDouble("damage", 4), s.getDouble("range", 20),
                    s.getDouble("radius", 0), s.getDouble("knockback", 0.5), s.getDouble("crater", 0), s.getLong("cooldown", 30)));
        }
        raw.sort(Comparator.comparingDouble(Level::chargeSeconds));
        List<Level> numbered = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            Level l = raw.get(i);
            numbered.add(new Level(i + 1, l.chargeSeconds(), l.damage(), l.range(), l.radius(), l.knockback(), l.crater(), l.cooldown()));
        }
        return numbered;
    }
}
