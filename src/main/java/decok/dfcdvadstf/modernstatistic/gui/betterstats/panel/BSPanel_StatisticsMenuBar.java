package decok.dfcdvadstf.modernstatistic.gui.betterstats.panel;

import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.modernstatistic.gui.TBetterStatsScreen;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.overlay.DropDownPanel;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.widget.TButtonWidget;
import net.minecraft.client.resources.I18n;

/**
 * Simplified top menu bar for the BetterStats screen.
 * <p>The About entry opens a drop-down menu (source code / credits links),
 * replacing the former {@code GuiAboutModernStatistic} screen.</p>
 * <p>顶部菜单栏：About 入口改为下拉菜单（源代码 / 致谢链接），
 * 替代原先的 {@code GuiAboutModernStatistic} 界面。</p>
 */
public class BSPanel_StatisticsMenuBar extends BSPanel {

    /** ModernStatistic source code repository / ModernStatistic 源代码仓库 */
    private static final String SOURCE_URL = "https://github.com/song682/modern-statistic";
    /** Original BetterStats repository (credits) / 原版 BetterStats 仓库（致谢） */
    private static final String CREDIT_URL = "https://github.com/TheCSDev/mc-better-stats";

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

        // About: drop-down menu with source code / credits links, both going
        // through the wiki-style link confirmation flow.
        // About：下拉菜单提供源代码 / 致谢链接，均走 Wiki 式链接确认流程。
        TButtonWidget btnAbout = new TButtonWidget(btnX, btnY, 80, BTN_HEIGHT,
                I18n.format("betterstats.gui.menu_bar.about"),
                btn -> {
                    new DropDownPanel(btn)
                            .addItem(Text.translatable("betterstats.gui.menu_bar.about.source"),
                                    () -> screen.showWikiConfirm(SOURCE_URL))
                            .addItem(Text.translatable("betterstats.gui.menu_bar.about.credits"),
                                    () -> screen.showWikiConfirm(CREDIT_URL))
                            .show();
                });
        addChild(btnAbout, false);
    }
}
