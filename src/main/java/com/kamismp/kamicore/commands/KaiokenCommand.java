package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Levels;
import com.kamismp.kamicore.util.Msg;
import com.kamismp.kamicore.util.Suggest;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * /kaioken [level] - e.g. /kaioken x10
 * Each level gives more Strength and Speed, but when it ends your body
 * pays: you lose a % of your max health (more for higher levels) plus Weakness.
 * Levels are defined in config.yml under kaioken.levels.
 */
public final class KaiokenCommand implements BasicCommand {

    private static final String COOLDOWN_KEY = "kaioken";
    private static final String LEVELS_PATH = "kaioken.levels";

    /** Who is using Kaioken right now, and at what level. */
    private record Active(String level, int multiplier, double kiMultiplier, BukkitTask task) {
    }

    private final KamiCore plugin;
    private final Map<UUID, Active> active = new HashMap<>();

    public KaiokenCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            Msg.playersOnly(source.getSender());
            return;
        }
        if (active.containsKey(player.getUniqueId())) {
            Msg.send(player, "<red>Kaioken is already active! Don't push your body any further!");
            return;
        }

        List<String> levelNames = Levels.names(plugin.getConfig(), LEVELS_PATH);
        if (levelNames.isEmpty()) {
            Msg.send(player, "<red>No Kaioken levels are set up in config.yml.");
            return;
        }
        String levelName = args.length >= 1
                ? args[0].toLowerCase(Locale.ROOT)
                : plugin.getConfig().getString("kaioken.default-level", levelNames.get(0));
        ConfigurationSection level = Levels.get(plugin.getConfig(), LEVELS_PATH, levelName);
        if (level == null) {
            Msg.send(player, "<red>Unknown Kaioken level. Choose: <yellow>" + Levels.list(levelNames) + "</yellow>");
            return;
        }
        if (!plugin.cooldowns().checkReady(player, COOLDOWN_KEY, "Kaioken")) {
            return;
        }

        int multiplier = Levels.number(levelName, 2);
        int duration = Math.max(1, level.getInt("duration", 60));
        int strength = Math.max(1, level.getInt("strength", 1));
        int speed = Math.max(1, level.getInt("speed", 1));
        double healthCost = clamp(level.getDouble("health-cost", 0.2), 0, 1);
        int strain = level.getInt("strain", 20);
        double kiMultiplier = Math.max(1.0, level.getDouble("ki-multiplier", 1.0));
        int ticks = duration * 20;

        // Amplifier 0 = level I, so subtract 1
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks, strength - 1, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, speed - 1, false, false, true));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.6f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.2f);

        plugin.cooldowns().start(player, COOLDOWN_KEY, level.getLong("cooldown", 86400));
        Msg.send(player, "<red><b>KAIOKEN... TIMES " + spoken(multiplier) + "!</b></red> <gold>Your power multiplies!</gold> <gray>("
                + duration + "s, costs " + Math.round(healthCost * 100) + "% of your health when it ends)");

        // The aura gets bigger and darker red for higher levels
        float auraSize = 1.2f + Math.min(multiplier, 20) * 0.08f;
        Particle.DustOptions aura = new Particle.DustOptions(Color.fromRGB(255, Math.max(0, 60 - multiplier * 3), 30), auraSize);
        int particleCount = 12 + Math.min(multiplier, 20) * 2;

        BukkitTask task = new BukkitRunnable() {
            int halfSecondsLeft = duration * 2;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    active.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                if (halfSecondsLeft <= 0) {
                    active.remove(player.getUniqueId());
                    cancel();
                    applyStrain(player, multiplier, healthCost, strain);
                    return;
                }
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), particleCount, 0.4, 0.9, 0.4, 0.0, aura);
                halfSecondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 10L);

        active.put(player.getUniqueId(), new Active(levelName, multiplier, kiMultiplier, task));
    }

    /** The price of Kaioken: lose a part of your max health, plus Weakness and Slowness. */
    private void applyStrain(Player player, int multiplier, double healthCost, int strainSeconds) {
        AttributeInstance maxAttr = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxAttr != null ? maxAttr.getValue() : 20.0;
        double minimumLeft = Math.max(0.5, plugin.getConfig().getDouble("kaioken.minimum-health-left", 1.0));

        double before = player.getHealth();
        double after = Math.max(Math.min(before, minimumLeft), before - maxHealth * healthCost);
        player.setHealth(Math.min(after, maxHealth));

        if (strainSeconds > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, strainSeconds * 20, 0, false, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, strainSeconds * 20, 0, false, false, true));
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.7f);

        double heartsLost = (before - after) / 2.0;
        Msg.send(player, "<gray>Kaioken x" + multiplier + " fades... <red>your body tears from the strain! <dark_red>(-"
                + String.format(Locale.ROOT, "%.1f", heartsLost) + " ❤)");
    }

    /** 1 if not using Kaioken, otherwise the multiplier (2, 3, 10, 20...). Used by the Scouter. */
    public int multiplier(Player player) {
        Active a = active.get(player.getUniqueId());
        return a == null ? 1 : a.multiplier();
    }

    /** How much stronger ki attacks are while this Kaioken is active (1.0 = normal). */
    public double kiMultiplier(Player player) {
        Active a = active.get(player.getUniqueId());
        return a == null ? 1.0 : a.kiMultiplier();
    }

    public void cancelAll() {
        for (Active a : active.values()) {
            a.task().cancel();
        }
        active.clear();
    }

    private static String spoken(int number) {
        return switch (number) {
            case 2 -> "TWO";
            case 3 -> "THREE";
            case 4 -> "FOUR";
            case 10 -> "TEN";
            case 20 -> "TWENTY";
            default -> String.valueOf(number);
        };
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (Suggest.index(args) == 0) {
            return Suggest.filter(Levels.names(plugin.getConfig(), LEVELS_PATH), Suggest.current(args));
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "kamicore.kaioken";
    }
}
