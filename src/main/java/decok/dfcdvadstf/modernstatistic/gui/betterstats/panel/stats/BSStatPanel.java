package decok.dfcdvadstf.modernstatistic.gui.betterstats.panel.stats;

import decok.dfcdvadstf.catframe.ui.layouts.FrameLayout;
import decok.dfcdvadstf.catframe.ui.layouts.GridLayout;
import decok.dfcdvadstf.catframe.ui.layouts.ILayout;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.TElement;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.panel.BSPanel;
import decok.dfcdvadstf.modernstatistic.gui.TBetterStatsScreen;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.widget.TScrollBarWidget;

/**
 * Abstract base for stat content panels — mimics BetterStats' {@code BSStatPanel}.
 * <p>Each subclass fills this panel with stat-specific widgets.</p>
 */
public abstract class BSStatPanel extends BSPanel {

    public static final int COLOR_NORMAL_HOVERED = 0x50FFFFFF;
    public static final int COLOR_NORMAL_FOCUSED = 0xFF0055FF;
    public static final int COLOR_GOLD_FOCUSED = 0xFFFFFF00;

    protected final TBetterStatsScreen screen;

    /**
     * FrameLayout content container — subclasses add children via {@link #addToLayout(ILayout)}.
     * <p>FrameLayout 内容容器 — 子类通过 {@link #addToLayout(ILayout)} 添加子控件。</p>
     */
    protected FrameLayout contentLayout;

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
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
        // Move the slider alongside this element when this element is moved
        // (mirrors original BetterStats)
        // 面板移动时滚动条跟随移动（对照原版 BetterStats）
        if (this.scrollBar != null) {
            this.scrollBar.setPosition(getEndX(), getY());
        }
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

    // ==================== Layout Container ====================

    /**
     * Initialize the content layout if not yet created.
     * <p>初始化内容布局（如尚未创建）。</p>
     */
    protected void ensureContentLayout() {
        if (contentLayout == null) {
            int sp = getScrollPadding();
            contentLayout = new FrameLayout(getX() + sp, getY() + sp,
                    getWidth() - sp * 2, getHeight() - sp * 2);
        }
    }

    /**
     * Add a child element to the content layout (FrameLayout).
     * <p>向内容布局添加子元素。</p>
     */
    protected void addToLayout(ILayout child) {
        ensureContentLayout();
        contentLayout.addChild(child);
    }

    // ==================== GridLayout Helpers ====================

    /**
     * Create a GridLayout positioned at the content area origin (respects scrollPadding).
     * <p>Subclasses use this as a position calculator, NOT as a renderable child,
     * because GridLayout doesn't extend TElement and can't be hosted in TPanelElement's
     * scissor/scroll system. After positioning children via RowHelper, add them
     * to this panel's TElement tree with {@code addChild(widget, false)}.</p>
     */
    protected GridLayout createContentGrid() {
        int sp = getScrollPadding();
        return new GridLayout(getX() + sp, getY() + sp);
    }

    /**
     * Fluent helper: set column spacing, row spacing, then create a RowHelper.
     * Equivalent to {@code grid.columnSpacing(cs).rowSpacing(rs).createRowHelper(columns)}.
     */
    protected GridLayout.RowHelper createGridHelper(GridLayout grid, int columns, int spacing) {
        grid.columnSpacing(spacing).rowSpacing(spacing);
        return grid.createRowHelper(columns);
    }

    /**
     * Add a widget to the TElement tree after it's been positioned by a GridLayout RowHelper.
     * <p>The widget's position (set by GridLayout.recalculate) is preserved.</p>
     */
    protected void addPositionedWidget(TElement widget) {
        addChild(widget, false);
    }

    // ==================== Helpers ====================

    /**
     * Get the Y position for the next child (auto-layout).
     */
    protected int getChildBottomY() {
        if (getChildCount() == 0) return getY() + getScrollPadding();
        TScrollBarWidget sb = getVerticalScrollBar();
        int maxBottom = getY() + getScrollPadding();
        for (TElement child : getChildren()) {
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
        TElement lbl = new TElement(getX() + sp, getY() + sp,
                        getWidth() - sp * 2, getHeight() - sp * 2) {
            @Override
            protected void renderSelf(int mouseX, int mouseY, float partialTicks) {
                net.minecraft.client.gui.FontRenderer fr = getFontRenderer();
                String text = net.minecraft.client.resources.I18n.format("betterstats.gui.no_stats_yet");
                int tw = fr.getStringWidth(text);
                fr.drawStringWithShadow(text, x + (width - tw) / 2, y + height / 2 - 4, 0x808080);
            }
        };
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
