package decok.dfcdvadstf.modernstatistic.gui.panel;

import java.util.function.Predicate;

import decok.dfcdvadstf.modernstatistic.ModernStatistic;
import decok.dfcdvadstf.modernstatistic.gui.screen.TBetterStatsScreen;
import decok.dfcdvadstf.modernstatistic.gui.screen.TBetterStatsScreen.CurrentTab;
import decok.dfcdvadstf.modernstatistic.gui.panel.stats.*;
import decok.dfcdvadstf.modernstatistic.gui.widget.TScrollBarWidget;

/**
 * Main statistics panel layout — mimics BetterStats' {@code BSPanel_Statistics}.
 * <p>Left sidebar (filters) + right content area (stat panels).</p>
 */
public class BSPanel_Statistics extends BSPanel {

    private static final int MENU_BAR_HEIGHT = 16;

    protected final TBetterStatsScreen screen;

    protected BSPanel_StatisticsMenuBar panelMenuBar;
    protected BSPanel_StatisticsFilters panelLeftMenu;
    protected BSPanel panelRightMenu;
    protected TScrollBarWidget scrollLeft;

    public BSPanel_Statistics(int x, int y, int width, int height, TBetterStatsScreen screen) {
        super(x, y, width, height);
        this.screen = screen;
        setVisible(false);
        setScrollPadding(0);
    }

    public void init(TBetterStatsScreen bss) {
        clearChildren();

        int mcpX = 10;
        int mcpY = 10 + MENU_BAR_HEIGHT;
        int mcpW = width / 7 * 2;  // ~2/7 of screen width
        int mcpH = height - 20 - MENU_BAR_HEIGHT;
        int scpX = mcpX + mcpW + 10;
        int scpY = mcpY;
        int scpW = width - scpX - 10;
        int scpH = mcpH;

        // --- Menu bar ---
        panelMenuBar = new BSPanel_StatisticsMenuBar(
                mcpX, 4, Math.abs(mcpX - (scpX + scpW)), MENU_BAR_HEIGHT, screen);
        panelMenuBar.init();
        addChild(panelMenuBar, false);

        // --- Left sidebar ---
        panelLeftMenu = new BSPanel_StatisticsFilters(mcpX, mcpY, mcpW, mcpH, screen);
        panelLeftMenu.init();
        addChild(panelLeftMenu, false);

        // --- Right content area ---
        panelRightMenu = new BSPanel(scpX + 4, scpY, scpW - 4, scpH);
        panelRightMenu.setScrollPadding(0);
        addChild(panelRightMenu, false);

        // --- Scrollbar for left sidebar ---
        scrollLeft = new TScrollBarWidget(
                panelLeftMenu.getEndX() - 1, panelLeftMenu.getY(),
                8, panelLeftMenu.getHeight(), panelLeftMenu);
        addChild(scrollLeft, false);

        // --- Init stats ---
        refreshStatsPanel();
    }

    /** Re-create the stat content panel based on current tab. */
    public void refreshStatsPanel() {
        if (panelRightMenu == null) return;
        panelRightMenu.clearChildren();

        CurrentTab tab = screen.getCurrentTab();
        BSStatPanel statPanel = null;

        switch (tab) {
            case General:
                statPanel = new BSStatPanel_General(panelRightMenu, screen);
                break;
            case Items:
                statPanel = new BSStatPanel_Items(panelRightMenu, screen);
                break;
            case Entities:
                statPanel = new BSStatPanel_Mobs(panelRightMenu, screen);
                break;
            case FoodStuffs:
                if (ModernStatistic.config.enableBalancedDietTab) {
                    statPanel = new BSStatPanel_BalancedDiet(panelRightMenu, screen);
                }
                break;
            case MonstersHunted:
                if (ModernStatistic.config.enableMonsterHunterTab) {
                    statPanel = new BSStatPanel_MonsterHunter(panelRightMenu, screen);
                }
                break;
        }

        if (statPanel != null) {
            statPanel.init();
        }
    }
}
