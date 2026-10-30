package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * /senzu - eat a Senzu Bean: full health, full hunger, removes poison/wither/hunger.
 * Very long cooldown.
 */
public final class SenzuCommand implements BasicCommand {

    private static final String COOLDOWN_KEY = "senzu";

    private final KamiCore plugin;

    public SenzuCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            Msg.playersOnly(source.getSender());
            return;
        }

        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;

        boolean fullHealth = player.getHealth() >= maxHealth;
        boolean fullFood = player.getFoodLevel() >= 20;
        if (fullHealth && fullFood) {
            Msg.send(player, "<gray>You're already at full power. Save the Senzu Bean for when you need it!");
            return;
        }
        if (!plugin.cooldowns().checkReady(player, COOLDOWN_KEY, "Senzu Bean")) {
            return;
        }

        player.setHealth(maxHealth);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.WITHER);
        player.removePotionEffect(PotionEffectType.HUNGER);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 8, 0.4, 0.4, 0.4, 0.0);

        plugin.cooldowns().start(player, COOLDOWN_KEY, plugin.getConfig().getLong("senzu.cooldown", 43200));
        Msg.send(player, "<green>*crunch*</green> <gold>The Senzu Bean restores all your strength!");
    }

    @Override
    public String permission() {
        return "kamicore.senzu";
    }
}
