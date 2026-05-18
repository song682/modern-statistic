package decok.dfcdvadstf.modernstatistic.gui.panel.stats;

import decok.dfcdvadstf.modernstatistic.gui.panel.BSPanel;
import decok.dfcdvadstf.modernstatistic.gui.screen.TBetterStatsScreen;
import decok.dfcdvadstf.modernstatistic.gui.widget.TScrollBarWidget;

/**
 * Abstract base for stat content panels — mimics BetterStats' {@code BSStatPanel}.
 * <p>Each subclass fills this panel with stat-specific widgets.</p>
 */
public abstract class BSStatPanel extends BSPanel {

    public static final int COLOR_NORMAL_HOVERED = 0x50FFFFFF;
    public static final int COLOR_NORMAL_FOCUSED = 0xFF0055FF;
    public static final int COLOR_GOLD_FOCUSED = 0xFFFFFF00;

    protected final TBetterStatsScreen screen;

    public BSStatPanel(BSPanel parentToFill, TBetterStatsScreen screen) {
        this(parentToFill.getX(), parentToFill.getY(),
                parentToFill.getWidth(), parentToFill.getHeight(), screen);
        // Add self to parent (scrollbar added via onParentChanged)
        parentToFill.addChild(this, false);
    }

    public BSStatPanel(int x, int y, int width, int height, TBetterStatsScreen screen) {
        super(x, y, width - 8, height); // Reduce width by 8 for scrollbar (mirrors original BetterStats)
        this.screen = screen;
        setScrollPadding(10);
        setSmoothScroll(true);

        // Internal scrollbar — target is THIS panel (mirrors original BetterStats)
        // Use inherited TPanelElement.scrollBar field, NOT a shadow field
        this.scrollBar = new TScrollBarWidget(getEndX(), getY(), 8, getHeight(), this);
    }

    @Override
    public void onParentChanged() {
        super.onParentChanged();
        // Remove scrollbar from previous parent
        if (this.scrollBar.getParent() != null && this.scrollBar.getParent() != getParent()) {
            this.scrollBar.getParent().removeChild(this.scrollBar);
        }
        // Add scrollbar to new parent (same level as this panel, avoids scissor clipping)
        if (getParent() != null) {
            getParent().addChild(this.scrollBar, false);
            this.scrollBar.refreshKnobSize();
        }
    }

    public TScrollBarWidget getVerticalScrollBar() {
        return this.scrollBar;
    }

    // ==================== Abstract ====================

    /**
     * Build and populate the stat widgets.
     */
    public abstract void init();

    // ==================== Helpers ====================

    /**
     * Get the Y position for the next child (auto-layout).
     */
    protected int getChildBottomY() {
        if (getChildCount() == 0) return getY() + getScrollPadding();
        TScrollBarWidget sb = getVerticalScrollBar();
        int maxBottom = getY() + getScrollPadding();
        for (decok.dfcdvadstf.modernstatistic.gui.TElement child : getChildren()) {
            if (child == sb) continue;
            if (child.isVisible() && child.getEndY() > maxBottom) {
                maxBottom = child.getEndY();
            }
        }
        return maxBottom + 2;
    }

    /**
     * Show "no results" placeholder.
     */
    protected void showNoResults() {
        int sp = getScrollPadding();
        decok.dfcdvadstf.modernstatistic.gui.widget.TLabelElement lbl =
                new decok.dfcdvadstf.modernstatistic.gui.widget.TLabelElement(
                        getX() + sp, getY() + sp,
                        getWidth() - sp * 2, getHeight() - sp * 2);
        lbl.setAlignment(decok.dfcdvadstf.modernstatistic.gui.widget.TLabelElement.ALIGN_CENTER);
        lbl.setText(net.minecraft.client.resources.I18n.format("betterstats.gui.no_stats_yet"));
        addChild(lbl, false);
    }

    // ==================== Rendering ====================

    @Override
    protected void renderSelf(int mouseX, int mouseY, float partialTicks) {
        // Apply smooth scrolling (handled by TPanelElement)
        super.renderSelf(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void postRenderSelf(int mouseX, int mouseY, float partialTicks) {
        super.postRenderSelf(mouseX, mouseY, partialTicks);
        // No outline on stat panels themselves
    }
}
