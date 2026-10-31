package com.kamismp.kamicore.dragonballs;

import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Shenron's wish menu: a chest window with one item per wish.
 * Implementing InventoryHolder lets the listener recognise this menu.
 */
public final class WishMenu implements InventoryHolder {

    public static final int SLOT_GEAR = 10;
    public static final int SLOT_RICHES = 12;
    public static final int SLOT_IMMORTAL = 14;
    public static final int SLOT_POWER = 16;

    private final Inventory inventory;

    public WishMenu(Wishes wishes) {
        MiniMessage mm = MiniMessage.miniMessage();
        this.inventory = Bukkit.createInventory(this, 27, mm.deserialize("<dark_green><b>Shenron:</b></dark_green> <black>Speak your wish!"));

        ItemStack filler = icon(Material.GREEN_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(SLOT_GEAR, icon(Material.NETHERITE_CHESTPLATE, "<light_purple><b>Legendary Gear", List.of(
                "<gray>Full netherite armor and a sword,",
                "<gray>with the best enchantments.")));
        inventory.setItem(SLOT_RICHES, icon(Material.DIAMOND, "<aqua><b>Riches", List.of(
                "<gray>" + wishes.richesDiamonds() + " diamonds, " + wishes.richesNetherite() + " netherite ingots",
                "<gray>and " + wishes.richesLevels() + " XP levels.")));
        inventory.setItem(SLOT_IMMORTAL, icon(Material.TOTEM_OF_UNDYING, "<gold><b>Immortal Body", List.of(
                "<gray>+" + wishes.immortalHearts() + " max hearts for " + wishes.immortalDays() + " days.")));
        inventory.setItem(SLOT_POWER, icon(Material.NETHER_STAR, "<red><b>Unlimited Power", List.of(
                "<gray>No cooldowns on any ability",
                "<gray>for " + wishes.unlimitedMinutes() + " minutes.")));
    }

    private static ItemStack icon(Material material, String name, List<String> lore) {
        MiniMessage mm = MiniMessage.miniMessage();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.deserialize("<!italic>" + name));
        meta.lore(lore.stream().map(line -> mm.deserialize("<!italic>" + line)).toList());
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
