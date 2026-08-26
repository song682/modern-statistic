package decok.dfcdvadstf.modernstatistic.gui.betterstats.panel.stats;

import java.util.ArrayList;
import java.util.List;

import decok.dfcdvadstf.modernstatistic.gui.betterstats.panel.BSPanel;
import decok.dfcdvadstf.modernstatistic.gui.TBetterStatsScreen;

import net.minecraft.client.resources.I18n;

/**
 * Block statistics panel — shows only blocks (from objectMineStats).
 * <p>Tooltip displays only the mined count for each block.</p>
 */
public class BSStatPanel_Blocks extends BSStatPanel_Items {

    public BSStatPanel_Blocks(BSPanel parentToFill, TBetterStatsScreen screen) {
        super(parentToFill, screen);
    }

    @Override
    protected void buildEntries() {
        super.buildEntries();
        // Filter to blocks only (isBlock == true)
        // 仅保留方块条目
        entries.entrySet().removeIf(e -> !e.getValue().isBlock);
    }

    /**
     * Override tooltip to show only the mined count for blocks.
     * <p>方块 tooltip 仅显示"开采次数"。</p>
     */
    @Override
    protected List<String> buildTooltipLines(ItemStatEntry entry) {
        List<String> lines = new ArrayList<>();
        String name = I18n.format(entry.item.getUnlocalizedName() + ".name").trim();
        lines.add(name);
        if (entry.mined > 0) {
            lines.add(I18n.format("stat_type.minecraft.mined") + ": " + entry.mined);
        }
        return lines;
    }
}
