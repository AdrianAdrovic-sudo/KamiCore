package com.kamismp.kamicore.dragonballs;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Cooldowns;
import com.kamismp.kamicore.util.Msg;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Summoning Shenron and granting the 4 wishes.
 * The two timed wishes (Immortal Body, Unlimited Power) are saved in data.yml,
 * so they keep working after a restart.
 */
public final class Wishes {

    private static final String IMMORTAL_KEY = "immortal-until";
    private static final String UNLIMITED_KEY = "unlimited-power-until";

    private final KamiCore plugin;
    private final NamespacedKey immortalModifier;

    public Wishes(KamiCore plugin) {
        this.plugin = plugin;
        this.immortalModifier = new NamespacedKey(plugin, "shenron_immortal");
    }

    // ================================================================= summoning

    /** /shenron: checks the balls, plays the summoning, opens the wish menu. */
    public void summon(Player player) {
        DragonBalls balls = plugin.dragonBalls();
        balls.purgeStale(player);
        int held = balls.heldBy(player).size();
        if (held < DragonBalls.COUNT) {
            Msg.send(player, "<red>You only have <yellow>" + held + "/7</yellow> Dragon Balls. Find them with <yellow>/scouter dragonballs</yellow>.");
            return;
        }

        World world = player.getWorld();
        Location loc = player.getLocation();
        for (int i = 0; i < 3; i++) {
            world.strikeLightningEffect(loc.clone().add((i - 1) * 4, 0, (i % 2) * 4 - 2));
        }
        world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.6f);
        for (Player nearby : world.getNearbyPlayers(loc, 60)) {
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20 * 5, 0, false, false, false));
        }
        playDragonSpiral(loc);
        Bukkit.getServer().sendMessage(MiniMessage.miniMessage().deserialize(
                "<dark_green><b>The sky turns dark...</b></dark_green> <yellow>" + player.getName()
                        + "</yellow> <green>has summoned <b>SHENRON</b>, the Eternal Dragon!"));
        Msg.raw(player, "<dark_green><b>SHENRON:</b></dark_green> <green>\"I am the Eternal Dragon. Speak your wish... I shall grant one.\"");

        player.openInventory(new WishMenu(this).getInventory());
    }

    /** A green dragon-like spiral of particles rising into the sky for 3 seconds. */
    private void playDragonSpiral(Location base) {
        Particle.DustOptions green = new Particle.DustOptions(Color.fromRGB(40, 200, 60), 2.5f);
        Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(255, 200, 40), 1.5f);
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick++ > 60) {
                    cancel();
                    return;
                }
                for (int i = 0; i < 3; i++) {
                    double t = tick * 0.5 + i * 0.15;
                    double height = t * 1.2;
                    double radius = 3.0 - Math.min(2.0, height / 20);
                    Location point = base.clone().add(Math.cos(t) * radius, height, Math.sin(t) * radius);
                    base.getWorld().spawnParticle(Particle.DUST, point, 6, 0.3, 0.3, 0.3, 0, green);
                    base.getWorld().spawnParticle(Particle.DUST, point, 1, 0.1, 0.1, 0.1, 0, gold);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** A wish was clicked in the menu. */
    public void grant(Player player, int slot) {
        DragonBalls balls = plugin.dragonBalls();
        if (balls.heldBy(player).size() < DragonBalls.COUNT) {
            player.closeInventory();
            Msg.send(player, "<red>You no longer hold all 7 Dragon Balls!");
            return;
        }

        String wishName;
        switch (slot) {
            case WishMenu.SLOT_GEAR -> {
                giveGear(player);
                wishName = "Legendary Gear";
            }
            case WishMenu.SLOT_RICHES -> {
                give(player, new ItemStack(Material.DIAMOND, richesDiamonds()));
                give(player, new ItemStack(Material.NETHERITE_INGOT, richesNetherite()));
                player.giveExpLevels(richesLevels());
                wishName = "Riches";
            }
            case WishMenu.SLOT_IMMORTAL -> {
                long until = System.currentTimeMillis() + immortalDays() * 86_400_000L;
                plugin.data().setPlayerLong(player.getUniqueId(), IMMORTAL_KEY, until);
                applyImmortal(player);
                AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
                if (max != null) {
                    player.setHealth(max.getValue());
                }
                wishName = "an Immortal Body";
            }
            case WishMenu.SLOT_POWER -> {
                long until = System.currentTimeMillis() + unlimitedMinutes() * 60_000L;
                plugin.data().setPlayerLong(player.getUniqueId(), UNLIMITED_KEY, until);
                wishName = "Unlimited Power";
            }
            default -> {
                return; // clicked a glass pane
            }
        }

        balls.takeAll(player);
        player.closeInventory();
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 120, 0.6, 1.0, 0.6, 0.4);
        Msg.raw(player, "<dark_green><b>SHENRON:</b></dark_green> <green>\"Your wish has been granted. Farewell!\"");
        Bukkit.getServer().sendMessage(MiniMessage.miniMessage().deserialize(
                "<yellow>" + player.getName() + "</yellow> <green>wished for <b>" + wishName + "</b>!"));
        balls.wishGranted();
    }

    // ================================================================= the wishes

    private void giveGear(Player player) {
        List<Material> armor = List.of(Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
                Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS);
        for (Material piece : armor) {
            ItemStack item = enchanted(piece, Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));
            if (piece == Material.NETHERITE_BOOTS) {
                ItemMeta meta = item.getItemMeta();
                meta.addEnchant(Enchantment.FEATHER_FALLING, 4, false);
                item.setItemMeta(meta);
            }
            give(player, item);
        }
        give(player, enchanted(Material.NETHERITE_SWORD,
                Map.of(Enchantment.SHARPNESS, 5, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1, Enchantment.LOOTING, 3)));
    }

    private static ItemStack enchanted(Material material, Map<Enchantment, Integer> enchants) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        enchants.forEach((enchant, level) -> meta.addEnchant(enchant, level, false));
        meta.displayName(MiniMessage.miniMessage().deserialize("<!italic><light_purple>Shenron's Gift"));
        item.setItemMeta(meta);
        return item;
    }

    private static void give(Player player, ItemStack item) {
        for (ItemStack rest : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }
    }

    // ========================================================= timed wishes

    /** Used by Cooldowns: true while the Unlimited Power wish is active. */
    public boolean hasUnlimitedPower(Player player) {
        return plugin.data().getPlayerLong(player.getUniqueId(), UNLIMITED_KEY) > System.currentTimeMillis();
    }

    public long immortalUntil(Player player) {
        return plugin.data().getPlayerLong(player.getUniqueId(), IMMORTAL_KEY);
    }

    public long unlimitedUntil(Player player) {
        return plugin.data().getPlayerLong(player.getUniqueId(), UNLIMITED_KEY);
    }

    /** Adds the extra hearts if the wish is active, removes them if it ran out. Run on join and every minute. */
    public void refresh(Player player) {
        long until = immortalUntil(player);
        if (until > System.currentTimeMillis()) {
            applyImmortal(player);
        } else {
            AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
            if (max != null && max.getModifier(immortalModifier) != null) {
                max.removeModifier(immortalModifier);
                if (player.getHealth() > max.getValue()) {
                    player.setHealth(max.getValue());
                }
                Msg.send(player, "<gray>Shenron's gift of an Immortal Body has faded.");
            }
            if (until != 0) {
                plugin.data().setPlayerLong(player.getUniqueId(), IMMORTAL_KEY, 0);
            }
        }
        long power = unlimitedUntil(player);
        if (power != 0 && power <= System.currentTimeMillis()) {
            plugin.data().setPlayerLong(player.getUniqueId(), UNLIMITED_KEY, 0);
            Msg.send(player, "<gray>Your Unlimited Power wish has run out. Cooldowns are back.");
        }
    }

    private void applyImmortal(Player player) {
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        if (max == null || max.getModifier(immortalModifier) != null) {
            return;
        }
        max.addModifier(new AttributeModifier(immortalModifier, immortalHearts() * 2.0,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
    }

    /** Lines for the Scouter / help. */
    public String timeLeft(long until) {
        return Cooldowns.format(Math.max(0, until - System.currentTimeMillis()));
    }

    // ============================================================ config values

    public int richesDiamonds() { return plugin.getConfig().getInt("wishes.riches.diamonds", 64); }
    public int richesNetherite() { return plugin.getConfig().getInt("wishes.riches.netherite-ingots", 16); }
    public int richesLevels() { return plugin.getConfig().getInt("wishes.riches.xp-levels", 50); }
    public int immortalHearts() { return plugin.getConfig().getInt("wishes.immortal-body.extra-hearts", 10); }
    public int immortalDays() { return plugin.getConfig().getInt("wishes.immortal-body.days", 7); }
    public int unlimitedMinutes() { return plugin.getConfig().getInt("wishes.unlimited-power.minutes", 60); }
}
