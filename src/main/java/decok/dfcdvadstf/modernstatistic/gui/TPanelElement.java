package decok.dfcdvadstf.modernstatistic.gui;

import decok.dfcdvadstf.modernstatistic.gui.widget.TScrollBarWidget;

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
                
                // If child is also a panel with content, use its totalContentHeight
                if (child instanceof TPanelElement) {
                    TPanelElement childPanel = (TPanelElement) child;
                    // childPanel.totalContentHeight is relative to childPanel itself
                    // So we need: (childPanel.y - this.y) + childPanel.totalContentHeight
                    childBottom = (child.y - this.y) + (int)childPanel.totalContentHeight;
                } else {
                    // Regular element: just use its bottom Y
                    childBottom = child.getEndY() - y;
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
        totalContentHeight = maxBottom - getY();
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

        // Translate children
        GL11.glPushMatrix();
        int cy = getContentY();
        GL11.glTranslatef(0, cy, 0);
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
        recalculateContentHeight();
    }

    // ==================== Mouse ====================

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible) return false;
        // Adjust mouseY for scroll offset for children within viewport
        boolean inViewport = mouseX >= x && mouseX < getEndX()
                && mouseY >= y && mouseY < getEndY();
        if (!inViewport) return false;

        int adjustedY = mouseY - getContentY();
        for (int i = children.size() - 1; i >= 0; i--) {
            TElement child = children.get(i);
            if (child.visible && child.mouseClicked(mouseX, adjustedY, button)) return true;
        }
        return onMouseClicked(mouseX, mouseY, button);
    }
}
