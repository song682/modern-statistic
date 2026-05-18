package decok.dfcdvadstf.modernstatistic.gui.widget;

import decok.dfcdvadstf.modernstatistic.gui.TElement;

/**
 * Simple clickable button with callback.
 */
public class TButtonWidget extends TElement {

    public interface PressAction {
        void onPress(TButtonWidget button);
    }

    protected String text;
    protected PressAction onPress;
    protected boolean pressed = false;

    private static final int COLOR_NORMAL_BG = 0x80000000;
    private static final int COLOR_HOVERED_BG = 0x80333333;
    private static final int COLOR_NORMAL_TEXT = 0xE0E0E0;
    private static final int COLOR_DISABLED_TEXT = 0xA0A0A0;
    private static final int COLOR_OUTLINE = 0x80000000;
    private static final int COLOR_OUTLINE_FOCUSED = 0xFFFFFFFF;

    public TButtonWidget(int x, int y, int width, int height, String text, PressAction onPress) {
        super(x, y, width, height);
        this.text = text;
        this.onPress = onPress;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    @Override
    protected void renderSelf(int mouseX, int mouseY, float partialTicks) {
        // Background
        int bgColor = (hovered || focused) ? COLOR_HOVERED_BG : COLOR_NORMAL_BG;
        fill(x, y, getEndX(), getEndY(), bgColor);

        // Text
        if (text != null && !text.isEmpty()) {
            int textColor = COLOR_NORMAL_TEXT;
            drawCenteredString(getFontRenderer(), text,
                    x + width / 2, y + (height - 8) / 2, textColor);
        }
    }

    @Override
    protected void postRenderSelf(int mouseX, int mouseY, float partialTicks) {
        int outlineColor = focused ? COLOR_OUTLINE_FOCUSED : COLOR_OUTLINE;
        drawOutline(x, y, getEndX(), getEndY(), outlineColor);
    }

    @Override
    protected boolean onMouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && onPress != null) {
            onPress.onPress(this);
            return true;
        }
        return false;
    }
}
