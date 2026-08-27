package decok.dfcdvadstf.modernstatistic.gui.tabbed.tab;

import decok.dfcdvadstf.catframe.ui.components.tab.TabBar;
import decok.dfcdvadstf.modernstatistic.ModernStatistic;

/**
 * TabBar for ModernStatistic
 */
public class ModernStatisticTabBar extends TabBar {
    public ModernStatisticTabBar() {
        super("modern_statistic");
        // Opaque black background matching the stats screen style,
        // and set as transparent when the tabbedLayoutClear is set as true
        if (ModernStatistic.config.tabbedLayoutClear) {
            setBackgroundColor(0x00000000);
        } else {
            setBackgroundColor(0xFF000000);
        }
    }
}
