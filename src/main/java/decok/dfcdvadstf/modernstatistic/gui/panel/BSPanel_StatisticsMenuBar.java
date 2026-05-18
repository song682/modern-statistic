package decok.dfcdvadstf.modernstatistic.gui.panel;

import decok.dfcdvadstf.modernstatistic.gui.TElement;
import decok.dfcdvadstf.modernstatistic.gui.screen.GuiAboutModernStatistic;
import decok.dfcdvadstf.modernstatistic.gui.screen.TBetterStatsScreen;
import decok.dfcdvadstf.modernstatistic.gui.widget.TButtonWidget;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/**
 * Simplified top menu bar for the BetterStats screen.
 * <p>Uses flat buttons instead of dropdown menus (no TCDCommons TMenuBarPanel in 1.7.10).</p>
 */
public class BSPanel_StatisticsMenuBar extends BSPanel {

    private static final int BTN_HEIGHT = 14;
    private static final int BTN_SPACING = 4;

    protected final TBetterStatsScreen screen;

    public BSPanel_StatisticsMenuBar(int x, int y, int width, int height, TBetterStatsScreen screen) {
        super(x, y, width, height);
        this.screen = screen;
        setScrollPadding(2);
    }

    public void init() {
        clearChildren();

        int btnX = getX() + getScrollPadding();
        int btnY = getY() + 1;

        // View: vanilla stats
        TButtonWidget btnView = new TButtonWidget(btnX, btnY, 100, BTN_HEIGHT,
                I18n.format("betterstats.gui.menu_bar.view"),
                btn -> screen.switchToVanillaStats());
        addChild(btnView, false);
        btnX += 100 + BTN_SPACING;

        // About: shows GuiAboutModernStatistic
        TButtonWidget btnAbout = new TButtonWidget(btnX, btnY, 80, BTN_HEIGHT,
                I18n.format("betterstats.gui.menu_bar.about"),
                btn -> screen.showAboutScreen());
        addChild(btnAbout, false);
    }
}
