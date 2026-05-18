package decok.dfcdvadstf.modernstatistic.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base GUI element — the building block of the Panel system.
 * <p>Mimics TCDCommons' {@code TElement} for 1.7.10.</p>
 */
public class TElement {

    protected int x, y;
    protected int width, height;
    protected boolean visible = true;
    protected boolean focused;
    protected boolean hovered;

    protected TElement parent;
    protected final List<TElement> children = new ArrayList<>();

    protected static final Minecraft mc = Minecraft.getMinecraft();

    // ==================== Construction ====================

    public TElement(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // ==================== Position & Size ====================

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    /** Right edge (x + width). */
    public int getEndX() { return x + width; }
    /** Bottom edge (y + height). */
    public int getEndY() { return y + height; }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // ==================== Visibility ====================

    public boolean isVisible() { return visible; }

    public void setVisible(boolean visible) { this.visible = visible; }

    /**
     * True if this element AND all its ancestors are visible.
     */
    public boolean isVisibleRecursive() {
        if (!visible) return false;
        return parent == null || parent.isVisibleRecursive();
    }

    // ==================== Focus & Hover ====================

    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) { this.focused = focused; }

    public boolean isHovered() { return hovered; }

    public boolean isFocusedOrHovered() { return focused || hovered; }

    /**
     * Update hover state based on mouse coordinates.
     */
    public void updateHover(int mouseX, int mouseY) {
        this.hovered = visible && mouseX >= x && mouseX < getEndX()
                && mouseY >= y && mouseY < getEndY();
    }

    // ==================== Parent / Children ====================

    public TElement getParent() { return parent; }

    /** Called when this element is added to or removed from a parent. */
    protected void onParentChanged() {}

    public List<TElement> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public int getChildCount() { return children.size(); }

    /**
     * Add a child element.
     * @param child the element to add
     * @param reposition if true, child is positioned sequentially below the last child
     * @return true if added
     */
    public boolean addChild(TElement child, boolean reposition) {
        if (child == null || child == this || children.contains(child)) return false;
        if (child.parent != null) {
            child.parent.removeChild(child);
        }
        child.parent = this;
        if (reposition) {
            TElement last = getLastChild(false);
            if (last != null) {
                child.setPosition(last.getX(), last.getEndY());
            }
        }
        children.add(child);
        child.onParentChanged();
        return true;
    }

    /** Convenience: add child without reposition. */
    public boolean addChild(TElement child) {
        return addChild(child, false);
    }

    public boolean removeChild(TElement child) {
        if (child == null || !children.remove(child)) return false;
        child.parent = null;
        child.onParentChanged();
        return true;
    }

    public void clearChildren() {
        for (TElement child : new ArrayList<>(children)) {
            removeChild(child);
        }
    }

    /**
     * Get the last direct child, optionally skipping invisible ones.
     */
    public TElement getLastChild(boolean visibleOnly) {
        for (int i = children.size() - 1; i >= 0; i--) {
            TElement c = children.get(i);
            if (!visibleOnly || c.visible) return c;
        }
        return null;
    }

    /**
     * Find the first child (recursive) of a given type.
     */
    @SuppressWarnings("unchecked")
    public <T extends TElement> T findChildOfType(Class<T> type, boolean recursive) {
        for (TElement c : children) {
            if (type.isInstance(c)) return (T) c;
            if (recursive) {
                T found = c.findChildOfType(type, true);
                if (found != null) return found;
            }
        }
        return null;
    }

    // ==================== Rendering ====================

    /**
     * Render this element and all visible children.
     */
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        renderSelf(mouseX, mouseY, partialTicks);
        // Render children
        for (TElement child : children) {
            if (child.visible) {
                child.render(mouseX, mouseY, partialTicks);
            }
        }
    }

    /**
     * Called after all children have rendered.
     */
    public void postRender(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        postRenderSelf(mouseX, mouseY, partialTicks);
        for (TElement child : children) {
            if (child.visible) {
                child.postRender(mouseX, mouseY, partialTicks);
            }
        }
    }

    /**
     * Override to render this element's own content (before children).
     */
    protected void renderSelf(int mouseX, int mouseY, float partialTicks) {}

    /**
     * Override to render overlays (after children), e.g. outlines.
     */
    protected void postRenderSelf(int mouseX, int mouseY, float partialTicks) {}

    // ==================== Mouse Events ====================

    /**
     * Dispatch a mouse click to the hit element in the tree.
     * @return true if handled
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible) return false;
        // Check children first (topmost in rendering order = last in list)
        for (int i = children.size() - 1; i >= 0; i--) {
            TElement child = children.get(i);
            if (child.mouseClicked(mouseX, mouseY, button)) return true;
        }
        // Then this element
        if (mouseX >= x && mouseX < getEndX() && mouseY >= y && mouseY < getEndY()) {
            return onMouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    protected boolean onMouseClicked(int mouseX, int mouseY, int button) { return false; }

    /**
     * Handle mouse release.
     */
    public void mouseReleased(int mouseX, int mouseY, int button) {
        if (!visible) return;
        for (TElement child : children) {
            if (child.visible) child.mouseReleased(mouseX, mouseY, button);
        }
    }

    /**
     * Handle scroll wheel.
     */
    public boolean handleMouseScroll(int mouseX, int mouseY, int delta) {
        if (!visible) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).handleMouseScroll(mouseX, mouseY, delta)) return true;
        }
        return false;
    }

    // ==================== Keyboard Events ====================

    public boolean keyTyped(char typedChar, int keyCode) {
        if (!visible) return false;
        for (TElement child : children) {
            if (child.visible && child.keyTyped(typedChar, keyCode)) return true;
        }
        return false;
    }

    // ==================== Utility Drawing Methods ====================

    /** Fill a solid rectangle. */
    public static void fill(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, color);
    }

    /** Draw a 1-pixel outline around a rectangle. */
    public static void drawOutline(int left, int top, int right, int bottom, int color) {
        fill(left, top, right, top + 1, color);
        fill(left, bottom - 1, right, bottom, color);
        fill(left, top, left + 1, bottom, color);
        fill(right - 1, top, right, bottom, color);
    }

    /** Draw text using the Minecraft font renderer. */
    public static void drawString(FontRenderer fr, String text, int x, int y, int color) {
        fr.drawStringWithShadow(text, x, y, color);
    }

    /** Draw a centered string. */
    public static void drawCenteredString(FontRenderer fr, String text, int centerX, int y, int color) {
        fr.drawStringWithShadow(text, centerX - fr.getStringWidth(text) / 2, y, color);
    }

    /** Get the FontRenderer. */
    public static FontRenderer getFontRenderer() { return mc.fontRenderer; }

    /** Get the Tessellator. */
    public static Tessellator getTessellator() { return Tessellator.instance; }

    /** Get the Minecraft instance. */
    public static Minecraft getMC() { return mc; }

    /**
     * Enable GL scissors for clipping to a rectangle.
     */
    public static void enableScissor(int x, int y, int width, int height) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int scale = new net.minecraft.client.gui.ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        GL11.glScissor(x * scale, mc.displayHeight - (y + height) * scale, width * scale, height * scale);
    }

    public static void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
