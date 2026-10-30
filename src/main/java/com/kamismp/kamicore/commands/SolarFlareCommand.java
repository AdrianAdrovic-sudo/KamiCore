package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * /solarflare - Tien's Solar Flare (Taiyoken).
 * Blinds every player nearby who can see you. Mobs lose track of you.
 */
public final class SolarFlareCommand implements BasicCommand {

    private static final String COOLDOWN_KEY = "solarflare";

    private final KamiCore plugin;

    public SolarFlareCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player caster)) {
            Msg.playersOnly(source.getSender());
            return;
        }
        if (!plugin.cooldowns().checkReady(caster, COOLDOWN_KEY, "Solar Flare")) {
            return;
        }

        double radius = plugin.getConfig().getDouble("solarflare.radius", 10);
        int blindTicks = plugin.getConfig().getInt("solarflare.blind-seconds", 5) * 20;
        boolean needSight = plugin.getConfig().getBoolean("solarflare.require-line-of-sight", true);
        boolean mustFace = plugin.getConfig().getBoolean("solarflare.must-face-caster", true);
        boolean affectMobs = plugin.getConfig().getBoolean("solarflare.affect-mobs", true);

        Location eye = caster.getEyeLocation();
        World world = caster.getWorld();

        // The flash
        world.spawnParticle(Particle.FLASH, eye, 3, 0.1, 0.1, 0.1, 0.0, Color.WHITE);
        world.spawnParticle(Particle.END_ROD, eye, 80, 0.3, 0.3, 0.3, 0.6);
        world.playSound(eye, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 1.6f);
        world.playSound(eye, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 2.0f);

        int blinded = 0;
        for (LivingEntity entity : world.getNearbyLivingEntities(eye, radius)) {
            if (entity.equals(caster) || entity.getLocation().distance(caster.getLocation()) > radius) {
                continue;
            }

            if (entity instanceof Player target) {
                if (target.getGameMode() == GameMode.SPECTATOR || target.getGameMode() == GameMode.CREATIVE) {
                    continue;
                }
                if (needSight && !target.hasLineOfSight(caster)) {
                    continue;
                }
                if (mustFace && !isLookingAt(target, eye)) {
                    continue;
                }
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindTicks, 0, false, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, blindTicks / 2, 0, false, false, true));
                Msg.send(target, "<yellow>" + caster.getName() + "</yellow> <white><b>SOLAR FLARE!</b></white> <gray>Your eyes!");
                blinded++;
            } else if (affectMobs && entity instanceof Mob mob) {
                // Mobs forget who they were chasing and stumble around
                mob.setTarget(null);
                mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, blindTicks, 1, false, false, false));
            }
        }

        plugin.cooldowns().start(caster, COOLDOWN_KEY, plugin.getConfig().getLong("solarflare.cooldown", 300));
        Msg.send(caster, "<white><b>SOLAR FLARE!</b></white> <gray>You blinded <yellow>" + blinded + "</yellow> "
                + (blinded == 1 ? "player" : "players") + ".");
    }

    /** True if the target is roughly facing the given point (within about 70 degrees). */
    private static boolean isLookingAt(Player target, Location point) {
        Location targetEye = target.getEyeLocation();
        Vector toPoint = point.toVector().subtract(targetEye.toVector());
        if (toPoint.lengthSquared() < 0.01) {
            return true;
        }
        return targetEye.getDirection().dot(toPoint.normalize()) > 0.35;
    }

    @Override
    public String permission() {
        return "kamicore.solarflare";
    }
}
