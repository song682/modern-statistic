package decok.dfcdvadstf.modernstatistic.gui.betterstats;

import decok.dfcdvadstf.modernstatistic.gui.betterstats.widget.TScrollBarWidget;

import org.lwjgl.opengl.GL11;

/**
 * A scrollable panel — mimics TCDCommons' {@code TPanelElement}.
 * <p>Children render within a scrollable viewport. Supports smooth scrolling.</p>
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

    public void setScrollY(double value) {
        this.scrollY = Math.max(0, Math.min(value, getMaxScrollY()));
        this.targetScrollY = this.scrollY;
    }

    /**
     * Maximum scroll value based on total content height minus viewport.
     */
    public double getMaxScrollY() {
        return Math.max(0, totalContentHeight - (height - scrollPadding * 2));
    }

    /**
     * Update totalContentHeight by finding the bottom-most child.
     * <p>Children are stored in coordinates relative to this panel, so
     * {@code child.y} / {@code child.getEndY()} are already relative values.</p>
     * <p>根据最下方的子元素更新 {@code totalContentHeight}。
     * 子元素坐标相对于本面板，{@code child.y} / {@code child.getEndY()} 已是相对值。</p>
     */
    protected void updateContentHeight() {
        if (children.isEmpty()) {
            totalContentHeight = height;
            return;
        }
        
        int maxY = 0;
        for (TElement child : children) {
            if (child.isVisible()) {
                int childBottom;
                
                if (child instanceof TPanelElement) {
                    TPanelElement childPanel = (TPanelElement) child;
                    // child.y is relative to this panel; childPanel.totalContentHeight
                    // is relative to childPanel itself
                    // child.y 相对于本面板；childPanel.totalContentHeight 相对于子面板自身
                    childBottom = child.y + (int)childPanel.totalContentHeight;
                } else {
                    // Regular element: endY is already relative to this panel
                    // 普通元素：endY 已是相对于本面板的值
                    childBottom = child.getEndY();
                }
                
                if (childBottom > maxY) {
                    maxY = childBottom;
                }
            }
        }
        
        totalContentHeight = Math.max(height, maxY + scrollPadding);
    }

    /**
     * The Y offset applied to children when rendering.
     * In Paneled mode, this is the negated scroll amount.
     */
    public int getContentY() {
        return (int) -scrollY;
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
     * <p>Children are in relative coordinates, so {@code child.getEndY()}
     * is already relative to this panel.</p>
     * <p>子元素为相对坐标，{@code child.getEndY()} 已是相对于本面板的值。</p>
     */
    public void recalculateContentHeight() {
        int maxBottom = 0;
        for (TElement child : children) {
            if (child == scrollBar) continue;
            if (child.visible) {
                int bottom = child.getEndY() + scrollPadding;
                if (bottom > maxBottom) maxBottom = bottom;
            }
        }
        totalContentHeight = maxBottom;
        if (scrollBar != null) scrollBar.refreshKnobSize();
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
                scrollY = Math.max(0, Math.min(scrollY - Integer.signum(delta) * 15, getMaxScrollY()));
                targetScrollY = scrollY;
                if (scrollBar != null) scrollBar.setValue(scrollY);
                return true;
            }
        }
        return false;
    }

    // ==================== Rendering ====================

    /**
     * Override to adjust mouse coordinates for children.
     * <p>Children are stored in coordinates relative to this panel. The GL transform
     * is {@code translate(0, y + getContentY())}, so the mouse must be converted to
     * the same relative space: {@code adjustedY = mouseY - y - getContentY()}.</p>
     * <p>子元素坐标相对于本面板。GL 变换为 {@code translate(0, y + getContentY())}，
     * 因此鼠标坐标须转换至同一相对空间：{@code adjustedY = mouseY - y - getContentY()}。</p>
     */
    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        // Update panel's own hover in screen space
        updateHover(mouseX, mouseY);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        renderSelf(mouseX, mouseY, partialTicks);
        // Convert mouse to content (relative) space for children
        // 将鼠标坐标转换为内容（相对）空间
        int adjustedMouseY = mouseY - y - getContentY();
        for (TElement child : children) {
            if (child.visible) {
                child.render(mouseX, adjustedMouseY, partialTicks);
            }
        }
    }

    @Override
    public void postRender(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        postRenderSelf(mouseX, mouseY, partialTicks);
        // Convert mouse to content (relative) space for children
        int adjustedMouseY = mouseY - y - getContentY();
        for (TElement child : children) {
            if (child.visible) {
                child.postRender(mouseX, adjustedMouseY, partialTicks);
            }
        }
    }

    @Override
    protected void renderSelf(int mouseX, int mouseY, float partialTicks) {
        // Update totalContentHeight based on children
        updateContentHeight();
        
        // Apply smooth scroll
        if (smoothScroll && Math.abs(targetScrollY - scrollY) > 0.01) {
            scrollY += (targetScrollY - scrollY) * 0.3;
            if (Math.abs(targetScrollY - scrollY) < 0.1) scrollY = targetScrollY;
        }

        // Scissor to viewport
        int scissorX = x + scrollPadding;
        int scissorY = y + scrollPadding;
        int scissorW = width - scrollPadding * 2;
        int scissorH = height - scrollPadding * 2;
        enableScissor(scissorX, scissorY, scissorW, scissorH);

        // Translate children: panel position + scroll offset
        // Children are in relative coordinates, so the transform must account
        // for the panel's own position as well as the scroll offset.
        // 平移子元素：面板位置 + 滚动偏移。子元素为相对坐标，变换须包含面板自身位置与滚动量。
        GL11.glPushMatrix();
        GL11.glTranslatef(0, y + getContentY(), 0);
    }

    @Override
    protected void postRenderSelf(int mouseX, int mouseY, float partialTicks) {
        // Restore transform
        GL11.glPopMatrix();
        disableScissor();

        // Render outline
        drawOutline(x, y, getEndX(), getEndY(), 0x80000000);
    }

    // ==================== Child Management ====================

    /**
     * Add child and convert its position from absolute to relative.
     * <p>Callers typically position children using absolute (screen-space) coordinates.
     * Since this panel's GL transform already accounts for the panel's own position,
     * children must be stored relative to {@code (this.x, this.y)}.</p>
     * <p>添加子元素并将其位置从绝对坐标转换为相对坐标。
     * 调用方通常使用绝对（屏幕空间）坐标定位子元素。由于本面板的 GL 变换已包含面板自身位置，
     * 子元素须存储为相对于 {@code (this.x, this.y)} 的坐标。</p>
     */
    @Override
    public boolean addChild(TElement child, boolean reposition) {
        if (super.addChild(child, reposition)) {
            // Convert absolute → relative
            child.setPosition(child.getX() - x, child.getY() - y);
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
        recalculateContentHeight();
    }

    // ==================== Mouse ====================

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible) return false;
        // Check viewport bounds (screen space)
        boolean inViewport = mouseX >= x && mouseX < getEndX()
                && mouseY >= y && mouseY < getEndY();
        if (!inViewport) return false;

        // Convert to relative (content) space for children
        // 转换为相对（内容）空间
        int adjustedY = mouseY - y - getContentY();
        for (int i = children.size() - 1; i >= 0; i--) {
            TElement child = children.get(i);
            if (child.visible && child.mouseClicked(mouseX, adjustedY, button)) return true;
        }
        return onMouseClicked(mouseX, mouseY, button);
    }
}
