package decok.dfcdvadstf.modernstatistic;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks item pickup and drop statistics for display in the stats UI.
 * This class uses Forge's event system to listen for item pickup and drop events.
 */
public class ItemStatsTracker {
    
    // Maps item ID to pickup count
    private static final Map<Integer, Integer> pickupStats = new HashMap<>();
    
    // Maps item ID to drop count  
    private static final Map<Integer, Integer> dropStats = new HashMap<>();
    
    /**
     * Called when a player picks up an item
     */
    @SubscribeEvent
    public void onItemPickup(EntityItemPickupEvent event) {
        if (event.entityPlayer == null || event.item == null) return;
        
        EntityItem entityItem = event.item;
        if (entityItem == null || entityItem.getEntityItem() == null) return;
        
        Item item = entityItem.getEntityItem().getItem();
        int itemId = Item.getIdFromItem(item);
        int amount = entityItem.getEntityItem().stackSize;
        
        synchronized (pickupStats) {
            pickupStats.put(itemId, pickupStats.getOrDefault(itemId, 0) + amount);
        }
    }
    
    /**
     * Called when a player drops an item
     */
    @SubscribeEvent  
    public void onItemDrop(ItemTossEvent event) {
        if (event.player == null || event.entityItem == null) return;
        
        EntityItem entityItem = event.entityItem;
        if (entityItem == null || entityItem.getEntityItem() == null) return;
        
        Item item = entityItem.getEntityItem().getItem();
        int itemId = Item.getIdFromItem(item);
        int amount = entityItem.getEntityItem().stackSize;
        
        synchronized (dropStats) {
            dropStats.put(itemId, dropStats.getOrDefault(itemId, 0) + amount);
        }
    }
    
    /**
     * Get the pickup count for a specific item ID
     */
    public static int getPickupCount(int itemId) {
        synchronized (pickupStats) {
            return pickupStats.getOrDefault(itemId, 0);
        }
    }
    
    /**
     * Get the drop count for a specific item ID  
     */
    public static int getDropCount(int itemId) {
        synchronized (dropStats) {
            return dropStats.getOrDefault(itemId, 0);
        }
    }
    
    /**
     * Check if an item has any pickup statistics
     */
    public static boolean hasPickupStats(int itemId) {
        synchronized (pickupStats) {
            return pickupStats.containsKey(itemId) && pickupStats.get(itemId) > 0;
        }
    }
    
    /**
     * Check if an item has any drop statistics
     */
    public static boolean hasDropStats(int itemId) {
        synchronized (dropStats) {
            return dropStats.containsKey(itemId) && dropStats.get(itemId) > 0;
        }
    }
    
    /**
     * Clear all statistics (useful for testing)
     */
    public static void clearStats() {
        synchronized (pickupStats) {
            pickupStats.clear();
        }
        synchronized (dropStats) {
            dropStats.clear();
        }
    }
}
