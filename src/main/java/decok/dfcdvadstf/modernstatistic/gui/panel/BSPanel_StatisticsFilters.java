package decok.dfcdvadstf.modernstatistic.gui.panel;

import decok.dfcdvadstf.modernstatistic.ModernStatistic;
import decok.dfcdvadstf.modernstatistic.gui.screen.TBetterStatsScreen;
import decok.dfcdvadstf.modernstatistic.gui.screen.TBetterStatsScreen.CurrentTab;
import decok.dfcdvadstf.modernstatistic.gui.widget.TButtonWidget;
import decok.dfcdvadstf.modernstatistic.gui.widget.TCheckboxWidget;
import decok.dfcdvadstf.modernstatistic.gui.widget.TLabelElement;
import decok.dfcdvadstf.modernstatistic.gui.widget.TSelectEnumWidget;
import decok.dfcdvadstf.modernstatistic.gui.widget.TTextFieldWidget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

/**
 * Left sidebar filter panel — mimics BetterStats' {@code BSPanel_StatisticsFilters}.
 */
public class BSPanel_StatisticsFilters extends BSPanel {

    protected final TBetterStatsScreen screen;

    public BSPanel_StatisticsFilters(int x, int y, int width, int height, TBetterStatsScreen screen) {
        super(x, y, width, height);
        this.screen = screen;
        // Left sidebar is fixed - no scrolling needed
        setSmoothScroll(false);
        setScrollPadding(0);
    }

    public void init() {
        clearChildren();

        int sp = getScrollPadding();
        int nextY = getY() + sp;

        // --- Filters label ---
        TLabelElement lblFilters = new TLabelElement(getX() + sp, nextY,
                getWidth() - sp * 2, 20,
                I18n.format("betterstats.gui.filters"));
        lblFilters.setAlignment(TLabelElement.ALIGN_CENTER);
        addChild(lblFilters, false);
        nextY += 25;

        // --- Tab selector ---
        TSelectEnumWidget<CurrentTab> btnTab = new TSelectEnumWidget<>(
                getX() + sp, nextY, getWidth() - sp * 2, 20, CurrentTab.class);
        btnTab.setSelected(screen.getCurrentTab(), false);
        btnTab.setLabelProvider(val -> ((CurrentTab) val).getLocalizedName());
        btnTab.setOnSelectionChange(val -> {
            CurrentTab tab = (CurrentTab) val;
            screen.setCurrentTab(tab);
            screen.refreshStats();
        });
        addChild(btnTab, false);
        nextY += 25;

        // --- Search bar ---
        TTextFieldWidget txtSearch = new TTextFieldWidget(
                getX() + sp, nextY, getWidth() - sp * 2, 20);
        txtSearch.setText(screen.getSearchTerm());
        txtSearch.setTextChangedListener(text -> {
            screen.setSearchTerm(text);
            screen.refreshStats();
        });
        addChild(txtSearch, false);
        nextY += 25;

        // --- Show empty stats ---
        TCheckboxWidget chkEmpty = new TCheckboxWidget(
                getX() + sp, nextY, getWidth() - sp * 2, 20,
                I18n.format("betterstats.gui.show_empty_stats"),
                ModernStatistic.config.showEmptyStats);
        chkEmpty.setCheckedChangeListener(checked -> {
            ModernStatistic.config.showEmptyStats = checked;
            ModernStatistic.config.save();
            screen.refreshStats();
        });
        addChild(chkEmpty, false);
        nextY += 30;

        // --- Close button (at bottom) ---
        int btnY = getEndY() - sp - 20;
        TButtonWidget btnClose = new TButtonWidget(
                getX() + sp, btnY, getWidth() - sp * 2, 20,
                I18n.format("gui.done"),
                btn -> Minecraft.getMinecraft().displayGuiScreen(screen.getParent()));
        addChild(btnClose, false);
    }
}
