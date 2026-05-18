package decok.dfcdvadstf.modernstatistic.gui.panel.stats;

import decok.dfcdvadstf.modernstatistic.gui.panel.BSPanel;
import decok.dfcdvadstf.modernstatistic.gui.screen.TBetterStatsScreen;

import net.minecraft.item.ItemFood;

/**
 * Balanced Diet tab — shows only food items.
 * <p>Items that have been eaten get a gold border.</p>
 */
public class BSStatPanel_BalancedDiet extends BSStatPanel_Items {

    public BSStatPanel_BalancedDiet(BSPanel parentToFill, TBetterStatsScreen screen) {
        super(parentToFill, screen);
    }

    @Override
    protected void buildEntries() {
        super.buildEntries();
        // Filter to food items only
        entries.entrySet().removeIf(e -> !(e.getValue().item instanceof ItemFood));
    }

    @Override
    protected int getStatValue(net.minecraft.stats.StatBase[] array, int id) {
        return super.getStatValue(array, id);
    }
}
