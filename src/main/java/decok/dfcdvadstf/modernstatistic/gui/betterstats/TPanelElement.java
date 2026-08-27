package decok.dfcdvadstf.modernstatistic.gui.betterstats;

import decok.dfcdvadstf.modernstatistic.gui.betterstats.widget.TScrollBarWidget;

import org.lwjgl.opengl.GL11;

/**
 * A scrollable panel — mimics TCDCommons' {@code TPanelElement}.
 * <p>Children render within a scrollable viewport. Supports smooth scrolling.</p>
 * <p>Scrolling is implemented by physically moving the children's coordinates
 * (always stored in absolute screen space), matching TCDCommons and CatFrame's
 * {@code ScrollContainer} — no GL translate is involved, so hover detection,
 * clicks and tooltips never need coordinate conversion.</p>
 * <p>滚动通过物理移动子元素坐标实现（坐标始终为屏幕绝对坐标），与 TCDCommons
 * 及 CatFrame 的 {@code ScrollContainer} 一致——不使用 GL translate，因此悬停
 * 检测、点击与 tooltip 均无需任何坐标转换。</p>
 */
public class TPanelElement extends TElement {

    protected int scrollPadding = 5;
    protected boolean smoothScroll = false;
    protected double scrollY = 0;
    protected double targetScrollY = 0;
    protected int totalContentHeight = 0;

    /** Internal scrollbar widget, auto-created. */
    protected TScrollBarWidget scrollBar;

    // ==================== Constructors ====================

    public TPanelElement(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    // ==================== Getters / Setters ====================

    public int getScrollPadding() { return scrollPadding; }

    public void setScrollPadding(int padding) { this.scrollPadding = padding; }

    public void setSmoothScroll(boolean smooth) { this.smoothScroll = smooth; }

    public double getScrollY() { return scrollY; }

    /**
     * Set the scroll offset. Children are physically moved so their
     * coordinates always stay in absolute screen space.
     * <p>设置滚动偏移。子元素会被物理移动，使其坐标始终保持在屏幕绝对坐标。</p>
     */
    public void setScrollY(double value) {
        double clamped = Math.max(0, Math.min(value, getMaxScrollY()));
        double delta = clamped - scrollY;
        this.scrollY = clamped;
        this.targetScrollY = clamped;
        if (delta != 0) {
            moveChildren(0, (int) -Math.round(delta));
        }
        if (scrollBar != null) scrollBar.refreshKnobSize();
    }

    /**
     * Maximum scroll value based on total content height minus viewport.
     */
    public double getMaxScrollY() {
        return Math.max(0, totalContentHeight - (height - scrollPadding * 2));
    }

    /**
     * Update totalContentHeight by finding the bottom-most child.
     * <p>Child positions shift while scrolling, so the current scroll offset
     * is compensated to recover the un-scrolled bottom edge.</p>
     * <p>子元素位置随滚动变化，因此用当前滚动量补偿，还原未滚动时的底部。</p>
     */
    protected void updateContentHeight() {
        if (children.isEmpty()) {
            totalContentHeight = height;
            return;
        }

        int maxY = 0;
        for (TElement child : children) {
            if (child == scrollBar) continue;
            if (child.isVisible()) {
                int childBottom;

                // If child is also a panel with content, use its totalContentHeight
                if (child instanceof TPanelElement) {
                    TPanelElement childPanel = (TPanelElement) child;
                    // childPanel.totalContentHeight is relative to childPanel itself
                    // So we need: (childPanel.y - this.y) + childPanel.totalContentHeight
                    childBottom = (child.y - y) + childPanel.totalContentHeight + (int) scrollY;
                } else {
                    // Regular element: just use its bottom Y
                    childBottom = child.getEndY() + (int) scrollY - y;
                }

                if (childBottom > maxY) {
                    maxY = childBottom;
                }
            }
        }

        totalContentHeight = Math.max(height, maxY + scrollPadding);
    }

    // ==================== ScrollBar ====================

    /**
     * Create and attach a scrollbar. Called automatically or manually.
     */
    public void initScrollBar() {
        if (scrollBar != null) return;
        scrollBar = new TScrollBarWidget(
                getEndX() - 8, getY(), 8, getHeight(), this);
        // Add to the same parent as this panel
    }

    public TScrollBarWidget getScrollBar() { return scrollBar; }

    // ==================== Scrolling ====================

    /**
     * Recalculate total content height from children.
     * <p>Child positions shift while scrolling, so the current scroll offset
     * is compensated to recover the un-scrolled bottom edge.</p>
     * <p>子元素位置随滚动变化，因此用当前滚动量补偿，还原未滚动时的底部。</p>
     */
    public void recalculateContentHeight() {
        int maxBottom = 0;
        for (TElement child : children) {
            if (child == scrollBar) continue;
            if (child.visible) {
                int bottom = child.getEndY() + (int) scrollY + scrollPadding;
                if (bottom > maxBottom) maxBottom = bottom;
            }
        }
        totalContentHeight = maxBottom - getY();
        if (scrollBar != null) scrollBar.refreshKnobSize();
    }

    /**
     * Move all direct children by the given delta, keeping their coordinates
     * in sync with this panel's position or scroll offset. Nested panels move
     * their own children through their {@code move} override.
     * <p>将全部直接子元素移动给定增量，使其坐标与本面板位置/滚动偏移保持同步。
     * 嵌套面板通过自身的 {@code move} 覆写继续联动其子元素。</p>
     */
    protected void moveChildren(int dx, int dy) {
        if (dx == 0 && dy == 0) return;
        for (TElement child : children) {
            if (child == scrollBar) continue;
            child.move(dx, dy);
        }
    }

    /**
     * Move this panel (and, through {@link #moveChildren}, its children)
     * by the given delta, preserving the current scroll state.
     * <p>将本面板（并经由 {@link #moveChildren} 联动其子元素）移动给定增量，
     * 保持当前滚动状态。</p>
     */
    @Override
    public void move(int dx, int dy) {
        super.move(dx, dy);
        moveChildren(dx, dy);
    }

    /**
     * Handle scroll wheel for this panel.
     */
    @Override
    public boolean handleMouseScroll(int mouseX, int mouseY, int delta) {
        if (!visible) return false;

        // Check children first (e.g. nested panels)
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).handleMouseScroll(mouseX, mouseY, delta)) return true;
        }

        // Scroll this panel if mouse is within bounds
        if (mouseX >= x && mouseX < getEndX() && mouseY >= y && mouseY < getEndY()) {
            if (delta != 0) {
                if (smoothScroll) {
                    // Smooth mode: only update the target; the render loop
                    // interpolates and physically moves the children.
                    // 平滑模式：只更新目标值；渲染循环插值并物理移动子元素。
                    targetScrollY = Math.max(0, Math.min(targetScrollY - Integer.signum(delta) * 15, getMaxScrollY()));
                } else {
                    setScrollY(scrollY - Integer.signum(delta) * 15);
                }
                return true;
            }
        }
        return false;
    }

    // ==================== Rendering ====================

    /**
     * Render this panel and its children. Children are scissor-clipped to the
     * content viewport; their coordinates are absolute, so no mouse conversion
     * is needed.
     * <p>渲染本面板及其子元素。子元素被 scissor 裁剪到内容视口；其坐标为绝对
     * 坐标，因此无需任何鼠标坐标转换。</p>
     */
    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        updateHover(mouseX, mouseY);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        renderSelf(mouseX, mouseY, partialTicks);
        // Scissor-clip children to the content viewport
        // 用 scissor 将子元素裁剪到内容视口
        enableScissor(x + scrollPadding, y + scrollPadding,
                width - scrollPadding * 2, height - scrollPadding * 2);
        for (TElement child : children) {
            if (child.visible) {
                child.render(mouseX, mouseY, partialTicks);
            }
        }
        disableScissor();
    }

    /**
     * Post-render: child overlays are scissor-clipped to the content viewport,
     * then this panel's own overlay (outline) is drawn on top, unclipped.
     * <p>后渲染：子元素叠加层被 scissor 裁剪到内容视口，随后本面板自身的
     * 叠加层（描边）在未裁剪状态下绘制在最上层。</p>
     */
    @Override
    public void postRender(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        enableScissor(x + scrollPadding, y + scrollPadding,
                width - scrollPadding * 2, height - scrollPadding * 2);
        for (TElement child : children) {
            if (child.visible) {
                child.postRender(mouseX, mouseY, partialTicks);
            }
        }
        disableScissor();
        postRenderSelf(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderSelf(int mouseX, int mouseY, float partialTicks) {
        // Update totalContentHeight based on children
        updateContentHeight();

        // Clamp the scroll offset in case the content height changed
        // 内容高度变化后钳制滚动偏移，避免越界导致内容错位
        if (scrollY > getMaxScrollY()) {
            setScrollY(getMaxScrollY());
        }

        // Apply smooth scroll by physically moving the children
        // 平滑滚动：物理移动子元素
        if (smoothScroll && Math.abs(targetScrollY - scrollY) > 0.01) {
            double oldScrollY = scrollY;
            scrollY += (targetScrollY - scrollY) * 0.3;
            if (Math.abs(targetScrollY - scrollY) < 0.1) scrollY = targetScrollY;
            moveChildren(0, (int) -Math.round(scrollY - oldScrollY));
            if (scrollBar != null) scrollBar.refreshKnobSize();
        }
    }

    @Override
    protected void postRenderSelf(int mouseX, int mouseY, float partialTicks) {
        // Render outline
        drawOutline(x, y, getEndX(), getEndY(), 0x80000000);
    }

    // ==================== Child Management ====================

    @Override
    public boolean addChild(TElement child, boolean reposition) {
        if (super.addChild(child, reposition)) {
            recalculateContentHeight();
            return true;
        }
        return false;
    }

    @Override
    public boolean removeChild(TElement child) {
        if (super.removeChild(child)) {
            recalculateContentHeight();
            return true;
        }
        return false;
    }

    @Override
    public void clearChildren() {
        super.clearChildren();
        // Clearing the content resets the scroll state
        // 清空内容时重置滚动状态
        this.scrollY = 0;
        this.targetScrollY = 0;
        recalculateContentHeight();
    }

    // ==================== Mouse ====================

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible) return false;
        // Children are positioned in absolute screen space, so the incoming
        // mouse coordinates are used as-is.
        // 子元素位于屏幕绝对坐标，因此直接使用传入的鼠标坐标。
        boolean inViewport = mouseX >= x && mouseX < getEndX()
                && mouseY >= y && mouseY < getEndY();
        if (!inViewport) return false;

        for (int i = children.size() - 1; i >= 0; i--) {
            TElement child = children.get(i);
            if (child.visible && child.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return onMouseClicked(mouseX, mouseY, button);
    }
}
