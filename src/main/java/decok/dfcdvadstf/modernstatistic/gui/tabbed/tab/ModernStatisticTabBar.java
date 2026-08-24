package decok.dfcdvadstf.modernstatistic.gui.tabbed.tab;

import decok.dfcdvadstf.catframe.ui.components.tab.TabBar;

/**
 * <p>
 * TabBar for ModernStatistic — isolates our tabs from other mods' tabs
 * via a unique bar ID, preventing Tab ID conflicts, and provides custom
 * background for the statistics screen nav bar.
 * </p>
 *
 * <p>
 * ModernStatistic 的 TabBar——通过唯一 barId 隔离我们的标签页，
 * 避免与其他模组的 Tab ID 冲突，并提供统计界面导航栏背景。
 * </p>
 */
public class ModernStatisticTabBar extends TabBar {
    public ModernStatisticTabBar() {
        super("modern_statistic");
        // Opaque black background matching the stats screen style
        // 不透明黑色背景，匹配统计界面风格
        setBackgroundColor(0xFF000000);
    }
}
