package decok.dfcdvadstf.modernstatistic.gui.panel.stats;

import java.util.List;

import decok.dfcdvadstf.modernstatistic.gui.TElement;
import decok.dfcdvadstf.modernstatistic.gui.panel.BSPanel;
import decok.dfcdvadstf.modernstatistic.gui.screen.TBetterStatsScreen;
import decok.dfcdvadstf.modernstatistic.gui.widget.TLabelElement;

import net.minecraft.client.resources.I18n;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;

/**
 * General statistics panel — list of misc stats (play time, distance walked, etc.).
 */
public class BSStatPanel_General extends BSStatPanel {

    public BSStatPanel_General(BSPanel parentToFill, TBetterStatsScreen screen) {
        super(parentToFill, screen);
    }

    @Override
    public void init() {
        clearChildren();

        @SuppressWarnings("unchecked")
        List<StatBase> stats = StatList.generalStats;
        if (stats.isEmpty()) {
            showNoResults();
            return;
        }

        boolean showEmpty = screen.isShowEmptyStats();
        int sp = getScrollPadding();
        int entryHeight = 12;
        int added = 0;

        for (StatBase stat : stats) {
            int value = screen.getStatFileWriter().writeStat(stat);
            if (!showEmpty && value == 0) continue;

            int x = getX() + sp;
            int y = getChildBottomY();
            int w = getWidth() - sp * 2;

            TLabelElement label = new TLabelElement(x, y, w / 2 + 20, entryHeight,
                    stat.func_150951_e().getUnformattedText());
            label.setAlignment(TLabelElement.ALIGN_LEFT);
            addChild(label, false);

            TLabelElement valueLabel = new TLabelElement(x + w / 2 + 20, y, w / 2 - 20, entryHeight,
                    stat.func_75968_a(value));
            valueLabel.setAlignment(TLabelElement.ALIGN_RIGHT);
            addChild(valueLabel, false);

            added++;
        }

        if (added == 0) {
            showNoResults();
        }
    }
}
