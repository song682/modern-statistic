package decok.dfcdvadstf.modernstatistic.gui.widget;

import decok.dfcdvadstf.modernstatistic.gui.TElement;
import decok.dfcdvadstf.modernstatistic.gui.TPanelElement;

import org.lwjgl.input.Mouse;

/**
 * Vertical scrollbar widget — mimics BetterStats' {@code BSScrollBarWidget}.
 */
public class TScrollBarWidget extends TElement {

    private static final int COLOR_BG = 0x50000000;       // semi-transparent black
    private static final int COLOR_BLACK = 0xFF000000;
    private static final int COLOR_NORMAL = 0x32FFFFFF;   // ~50 alpha white
    private static final int COLOR_HOVERED = 0x6EFFFFFF;  // ~110 alpha white

    protected final TPanelElement target;
    protected boolean dragging = false;
    protected int dragStartY = 0;
    protected double dragStartValue = 0;

    public TScrollBarWidget(int x, int y, int width, int height, TPanelElement target) {
        super(x, y, width, height);
        this.target = target;
    }

    // ==================== Knob ====================

    /**
     * Recalculate knob size based on target content vs viewport.
     */
    public void refreshKnobSize() {
        // No-op; size calculated on-the-fly in render
    }

    protected int getKnobHeight() {
        if (target == null) return height;
        double maxScroll = target.getMaxScrollY();
        double viewH = target.getHeight() - target.getScrollPadding() * 2;
        double totalH = viewH + maxScroll;
        if (totalH <= 0) return height;
        int knobH = (int) (viewH / totalH * height);
        return Math.max(16, Math.min(knobH, height));
    }

    protected int getKnobY() {
        if (target == null) return y;
        double maxScroll = target.getMaxScrollY();
        if (maxScroll <= 0) return y;
        double ratio = target.getScrollY() / maxScroll;
        int travel = height - getKnobHeight();
        return y + (int) (ratio * travel);
    }

    // ==================== Value ====================

    public void setValue(double value) {
        if (target != null) target.setScrollY(value);
    }

    public double getValue() {
        return target != null ? target.getScrollY() : 0;
    }

    // ==================== Rendering ====================

    @Override
    protected void renderSelf(int mouseX, int mouseY, float partialTicks) {
        // Track background
        fill(x, y, getEndX(), getEndY(), COLOR_BG);
        drawOutline(x, y, getEndX(), getEndY(), COLOR_BLACK);
        // Knob
        int knobY = getKnobY();
        int knobH = getKnobHeight();
        int knobColor = (focused || hovered) ? COLOR_HOVERED : COLOR_NORMAL;
        fill(x + 1, knobY + 1, getEndX() - 1, knobY + knobH - 1, knobColor);
    }

    // ==================== Mouse ====================

    @Override
    protected boolean onMouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return false;

        int knobY = getKnobY();
        int knobH = getKnobHeight();

        if (mouseY >= knobY && mouseY < knobY + knobH) {
            // Clicked on knob — start dragging
            dragging = true;
            dragStartY = mouseY;
            dragStartValue = getValue();
            return true;
        } else {
            // Clicked on track — jump scroll
            double maxScroll = target != null ? target.getMaxScrollY() : 0;
            if (maxScroll <= 0) return true;
            double clickRatio = (double) (mouseY - y) / height;
            setValue(clickRatio * maxScroll);
            dragging = true;
            dragStartY = mouseY;
            dragStartValue = getValue();
            return true;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        dragging = false;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        // Handle dragging
        if (dragging && !Mouse.isButtonDown(0)) {
            dragging = false;
        }
        if (dragging) {
            double maxScroll = target != null ? target.getMaxScrollY() : 0;
            if (maxScroll > 0) {
                int deltaY = mouseY - dragStartY;
                int travel = height - getKnobHeight();
                if (travel > 0) {
                    double ratio = (double) deltaY / travel;
                    setValue(dragStartValue + ratio * maxScroll);
                }
            }
        }
        super.render(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean handleMouseScroll(int mouseX, int mouseY, int delta) {
        // Let the target panel handle scrolling via wheel
        return false;
    }
}
