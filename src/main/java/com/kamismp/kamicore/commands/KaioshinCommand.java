package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.kaioshin.Kaioshin;
import com.kamismp.kamicore.util.Effects;
import com.kamismp.kamicore.util.Msg;
import com.kamismp.kamicore.util.Suggest;
import io.papermc.paper.ban.BanListType;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * The powers of the Kaioshin (the server's admins). One class, one command per power.
 *
 * /hakai <player> [reason]      - erase (permanent ban)
 * /revive <player>              - bring back (unban)
 * /banish <player> [reason]     - kick
 * /seal <player> [minutes]      - Mafuba seal (mute); 0 = forever
 * /unseal <player>              - unmute
 * /timeskip <player>            - freeze / unfreeze
 * /kaikai <player> [here]       - teleport to a player, or bring them to you
 * /kaiheal [player]             - full heal
 * /telepathy <message>          - announcement to everyone
 * /divine                       - god mode: invulnerable + flight
 * /kaioshin                     - list of powers
 */
public final class KaioshinCommand implements BasicCommand {

    public enum Power { HAKAI, REVIVE, BANISH, SEAL, UNSEAL, TIMESKIP, KAIKAI, KAIHEAL, TELEPATHY, DIVINE, HELP }

    private static final List<PotionEffectType> BAD_EFFECTS = List.of(PotionEffectType.POISON, PotionEffectType.WITHER,
            PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.HUNGER,
            PotionEffectType.MINING_FATIGUE, PotionEffectType.DARKNESS, PotionEffectType.NAUSEA);

    private final KamiCore plugin;
    private final Power power;

    public KaioshinCommand(KamiCore plugin, Power power) {
        this.plugin = plugin;
        this.power = power;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        switch (power) {
            case HAKAI -> hakai(sender, args);
            case REVIVE -> revive(sender, args);
            case BANISH -> banish(sender, args);
            case SEAL -> seal(sender, args);
            case UNSEAL -> unseal(sender, args);
            case TIMESKIP -> timeskip(sender, args);
            case KAIKAI -> kaikai(sender, args);
            case KAIHEAL -> kaiheal(sender, args);
            case TELEPATHY -> telepathy(sender, args);
            case DIVINE -> divine(sender);
            case HELP -> help(sender);
        }
    }

    // ================================================================ powers

    /** Hakai: the God of Destruction's technique. Erases the player (permanent ban). */
    private void hakai(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Msg.send(sender, "<red>Usage: /hakai [player] [reason]");
            return;
        }
        String reason = args.length > 1 ? join(args, 1) : plugin.getConfig().getString("kaioshin.hakai-reason", "Erased by Hakai.");
        Player online = Bukkit.getPlayerExact(args[0]);

        if (online != null) {
            if (online.equals(sender)) {
                Msg.send(sender, "<red>Even a god can't Hakai themselves.");
                return;
            }
            // The erasing effect, then the ban a moment later
            Location loc = online.getLocation().add(0, 1, 0);
            online.getWorld().spawnParticle(Particle.DUST, loc, 120, 0.4, 0.9, 0.4, 0, new Particle.DustOptions(Color.fromRGB(150, 40, 220), 1.8f));
            online.getWorld().spawnParticle(Particle.SQUID_INK, loc, 40, 0.3, 0.8, 0.3, 0.05);
            online.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 0.8f, 1.6f);
            broadcast("<dark_purple><b>HAKAI!</b></dark_purple> <light_purple>" + sender.getName() + "</light_purple> <gray>erased</gray> <yellow>"
                    + online.getName() + "</yellow> <gray>from existence.");
            String source = sender.getName();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (online.isOnline()) {
                    online.ban("Hakai: " + reason, (Date) null, source, true);
                }
            }, 25L);
            return;
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(args[0]);
        if (offline == null) {
            Msg.send(sender, "<red>No player called <yellow>" + args[0] + "</yellow> has ever played here.");
            return;
        }
        offline.ban("Hakai: " + reason, (Date) null, sender.getName());
        broadcast("<dark_purple><b>HAKAI!</b></dark_purple> <light_purple>" + sender.getName() + "</light_purple> <gray>erased</gray> <yellow>"
                + offline.getName() + "</yellow> <gray>from existence.");
    }

    /** Revive: undo a Hakai (unban). */
    private void revive(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Msg.send(sender, "<red>Usage: /revive [player]");
            return;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(args[0]);
        if (offline == null || !offline.isBanned()) {
            Msg.send(sender, "<gray><yellow>" + args[0] + "</yellow> isn't erased.");
            return;
        }
        Bukkit.getBanList(BanListType.PROFILE).pardon(offline.getPlayerProfile());
        broadcast("<green><b>REVIVED!</b></green> <light_purple>" + sender.getName() + "</light_purple> <gray>brought</gray> <yellow>"
                + offline.getName() + "</yellow> <gray>back to the world of the living.");
    }

    /** Banish: kick from the server. */
    private void banish(CommandSender sender, String[] args) {
        Player target = onlineTarget(sender, args, "/banish [player] [reason]");
        if (target == null) {
            return;
        }
        String reason = args.length > 1 ? join(args, 1) : "Banished by a Kaioshin.";
        target.getWorld().strikeLightningEffect(target.getLocation());
        target.kick(MiniMessage.miniMessage().deserialize("<dark_purple><b>BANISHED</b></dark_purple>\n<gray>" + reason));
        broadcast("<light_purple>" + sender.getName() + "</light_purple> <gray>banished</gray> <yellow>" + target.getName()
                + "</yellow> <gray>from the realm.");
    }

    /** Seal: Mafuba, the Evil Containment Wave. Mutes the player. */
    private void seal(CommandSender sender, String[] args) {
        Player target = onlineTarget(sender, args, "/seal [player] [minutes, 0 = forever]");
        if (target == null) {
            return;
        }
        long minutes = plugin.getConfig().getLong("kaioshin.default-seal-minutes", 10);
        if (args.length > 1) {
            try {
                minutes = Long.parseLong(args[1]);
            } catch (NumberFormatException e) {
                Msg.send(sender, "<red>Minutes must be a number (0 = forever).");
                return;
            }
        }
        plugin.kaioshin().seal(target, minutes);
        String time = minutes <= 0 ? "forever" : minutes + " minute" + (minutes == 1 ? "" : "s");
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 60, 0.4, 0.8, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(80, 220, 120), 1.5f));
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.6f, 1.8f);
        Msg.send(target, "<dark_purple><b>MAFUBA!</b></dark_purple> <gray>You have been sealed and cannot speak for <yellow>" + time + "</yellow>.");
        Msg.send(sender, "<green>Sealed <yellow>" + target.getName() + "</yellow> for " + time + ".");
    }

    private void unseal(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Msg.send(sender, "<red>Usage: /unseal [player]");
            return;
        }
        Player online = Bukkit.getPlayerExact(args[0]);
        OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayerIfCached(args[0]);
        if (target == null) {
            Msg.send(sender, "<red>Unknown player.");
            return;
        }
        plugin.kaioshin().unseal(target.getUniqueId());
        Msg.send(sender, "<green>Unsealed <yellow>" + target.getName() + "</yellow>.");
        if (online != null) {
            Msg.send(online, "<green>The seal breaks. You can speak again.");
        }
    }

    /** Time Skip: freeze (or unfreeze) a player. */
    private void timeskip(CommandSender sender, String[] args) {
        Player target = onlineTarget(sender, args, "/timeskip [player]");
        if (target == null) {
            return;
        }
        boolean frozen = plugin.kaioshin().toggleFrozen(target.getUniqueId());
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, frozen ? 0.5f : 1.5f);
        if (frozen) {
            Msg.send(target, "<dark_aqua><b>TIME SKIP!</b></dark_aqua> <gray>Time has stopped for you.");
            Msg.send(sender, "<green>Froze <yellow>" + target.getName() + "</yellow>. Use /timeskip again to release them.");
        } else {
            Msg.send(target, "<aqua>Time flows again.");
            Msg.send(sender, "<green>Released <yellow>" + target.getName() + "</yellow>.");
        }
    }

    /** Kai Kai: Kibito's teleport. Go to a player, or bring them to you with "here". */
    private void kaikai(CommandSender sender, String[] args) {
        if (!(sender instanceof Player kai)) {
            Msg.playersOnly(sender);
            return;
        }
        Player target = onlineTarget(sender, args, "/kaikai [player] [here]");
        if (target == null) {
            return;
        }
        boolean bring = args.length > 1 && args[1].equalsIgnoreCase("here");
        if (bring) {
            Effects.instantTransmission(target, kai.getLocation());
            Msg.send(target, "<light_purple>A Kaioshin has summoned you with Kai Kai.");
            Msg.send(kai, "<green>Brought <yellow>" + target.getName() + "</yellow> to you.");
        } else {
            Effects.instantTransmission(kai, target.getLocation());
            Msg.send(kai, "<green>Kai Kai! You appear next to <yellow>" + target.getName() + "</yellow>.");
        }
    }

    /** Kaioshin healing: full health, food and no bad effects. */
    private void kaiheal(CommandSender sender, String[] args) {
        Player target;
        if (args.length == 0) {
            if (!(sender instanceof Player self)) {
                Msg.send(sender, "<red>Usage: /kaiheal [player]");
                return;
            }
            target = self;
        } else {
            target = onlineTarget(sender, args, "/kaiheal [player]");
            if (target == null) {
                return;
            }
        }
        AttributeInstance max = target.getAttribute(Attribute.MAX_HEALTH);
        target.setHealth(max != null ? max.getValue() : 20.0);
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.setFireTicks(0);
        BAD_EFFECTS.forEach(target::removePotionEffect);
        target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 2, 0), 10, 0.5, 0.3, 0.5, 0);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f, 1.6f);
        Msg.send(target, "<light_purple>A warm divine light restores your body.");
        if (!target.equals(sender)) {
            Msg.send(sender, "<green>Healed <yellow>" + target.getName() + "</yellow>.");
        }
    }

    /** Telepathy: like King Kai, speak to everyone at once. */
    private void telepathy(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Msg.send(sender, "<red>Usage: /telepathy [message]");
            return;
        }
        String message = join(args, 0).replace("<", "‹"); // stop MiniMessage tags in announcements
        broadcast("<light_purple><b>[Telepathy]</b></light_purple> <gray>" + sender.getName() + ":</gray> <white>" + message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.8f);
        }
    }

    /** Divine: invulnerable + flight. */
    private void divine(CommandSender sender) {
        if (!(sender instanceof Player kai)) {
            Msg.playersOnly(sender);
            return;
        }
        boolean on = plugin.kaioshin().toggleDivine(kai);
        Msg.send(kai, on
                ? "<light_purple><b>DIVINE</b></light_purple> <gray>mode on: you're invulnerable and can fly."
                : "<gray>Divine mode off. You're mortal again.");
    }

    private void help(CommandSender sender) {
        Msg.raw(sender, "<light_purple><b>Powers of the Kaioshin</b>");
        line(sender, "/hakai [player] [reason]", "erase from existence (ban)");
        line(sender, "/revive [player]", "bring back (unban)");
        line(sender, "/banish [player] [reason]", "send away (kick)");
        line(sender, "/seal [player] [minutes]", "Mafuba seal (mute), 0 = forever");
        line(sender, "/unseal [player]", "break the seal (unmute)");
        line(sender, "/timeskip [player]", "stop time for a player (freeze)");
        line(sender, "/kaikai [player] [here]", "teleport to a player / bring them");
        line(sender, "/kaiheal [player]", "full heal");
        line(sender, "/telepathy [message]", "speak to everyone (announcement)");
        line(sender, "/divine", "invulnerable + flight");
        line(sender, "/dragonballs status|scatter|give", "Dragon Ball tools");
    }

    // ================================================================ helpers

    private Player onlineTarget(CommandSender sender, String[] args, String usage) {
        if (args.length == 0) {
            Msg.send(sender, "<red>Usage: " + usage);
            return null;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            Msg.send(sender, "<red><yellow>" + args[0] + "</yellow> is not online.");
        }
        return target;
    }

    private static String join(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }

    private static void line(CommandSender sender, String command, String description) {
        Msg.raw(sender, " <light_purple>" + command + "</light_purple> <dark_gray>-</dark_gray> <gray>" + description);
    }

    private static void broadcast(String message) {
        Bukkit.getServer().sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (power == Power.HELP || power == Power.DIVINE || power == Power.TELEPATHY) {
            return List.of();
        }
        int index = Suggest.index(args);
        if (index == 0) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return Suggest.filter(names, Suggest.current(args));
        }
        if (index == 1 && power == Power.KAIKAI) {
            return Suggest.filter(List.of("here"), Suggest.current(args));
        }
        return List.of();
    }

    @Override
    public String permission() {
        return Kaioshin.PERMISSION;
    }
}
