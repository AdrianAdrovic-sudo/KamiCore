package com.kamismp.kamicore.dragonballs;

import com.kamismp.kamicore.KamiCore;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Everything the Dragon Balls need to react to:
 * collecting them, keeping them safe as items, the wish menu, and
 * stopping them from being used as normal magma cream.
 */
public final class DragonBallListener implements Listener {

    private final KamiCore plugin;

    public DragonBallListener(KamiCore plugin) {
        this.plugin = plugin;
    }

    private DragonBalls balls() {
        return plugin.dragonBalls();
    }

    /** Right-clicking a ball in the world collects it. */
    @EventHandler(ignoreCancelled = true)
    public void onClickBall(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (event.getHand() != EquipmentSlot.HAND || !(clicked instanceof Interaction) || !balls().isBallEntity(clicked)) {
            return;
        }
        event.setCancelled(true);
        balls().collect(event.getPlayer(), clicked);
    }

    /** When a chunk loads, remove ball entities that should no longer exist (collected or from an old cycle). */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (balls().isBallEntity(entity) && !balls().isValidBallEntity(entity)) {
                entity.remove();
            }
        }
    }

    /** A dropped ball can't burn, despawn or be destroyed, and it glows so it's easy to spot. */
    @EventHandler(ignoreCancelled = true)
    public void onBallDropped(ItemSpawnEvent event) {
        Item item = event.getEntity();
        if (balls().isBall(item.getItemStack())) {
            item.setUnlimitedLifetime(true);
            item.setInvulnerable(true);
            item.setGlowing(true);
        }
    }

    /** If a ball item is lost anyway (e.g. falls into the void), it flies to a new hiding place. */
    @EventHandler
    public void onBallRemoved(EntityRemoveEvent event) {
        if (!(event.getEntity() instanceof Item item) || !balls().isBall(item.getItemStack())) {
            return;
        }
        switch (event.getCause()) {
            case DEATH, DESPAWN, OUT_OF_WORLD, EXPLODE -> balls().ballItemLost(item.getItemStack());
            default -> {
                // picked up, put in a hopper, chunk unloaded... the ball still exists
            }
        }
    }

    /** Dragon Balls can't be crafted into magma blocks. */
    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (balls().isBall(item)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    /** Wish menu clicks, and no brewing with Dragon Balls. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();

        if (top.getHolder() instanceof WishMenu) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player && event.getRawSlot() < top.getSize()) {
                plugin.wishes().grant(player, event.getRawSlot());
            }
            return;
        }

        if (top.getType() == InventoryType.BREWING) {
            boolean ballInvolved = balls().isBall(event.getCurrentItem()) || balls().isBall(event.getCursor());
            if (event.getHotbarButton() >= 0) {
                ballInvolved |= balls().isBall(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()));
            }
            if (ballInvolved) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof WishMenu
                || (top.getType() == InventoryType.BREWING && balls().isBall(event.getOldCursor()))) {
            event.setCancelled(true);
        }
    }

    /** Hoppers can't feed Dragon Balls into brewing stands. */
    @EventHandler(ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent event) {
        if (event.getDestination().getType() == InventoryType.BREWING && balls().isBall(event.getItem())) {
            event.setCancelled(true);
        }
    }
}
