package decok.dfcdvadstf.modernstatistic.gui.widget;

import decok.dfcdvadstf.modernstatistic.ModernStatistic;
import decok.dfcdvadstf.modernstatistic.gui.TElement;

import org.lwjgl.input.Keyboard;

/**
 * Simple text input field for search/filter.
 */
public class TTextFieldWidget extends TElement {

    public interface TextChangedListener {
        void onTextChanged(String newText);
    }

    protected String text = "";
    protected TextChangedListener listener;
    protected boolean focused = false;
    protected int cursorPos = 0;
    protected int selectionEnd = 0;
    protected int maxLength = 100;
    protected int tickCounter = 0;

    private static final int COLOR_BG = 0xCC000000;
    private static final int COLOR_BG_FOCUSED = 0xDD111111;
    private static final int COLOR_OUTLINE = 0xFF000000;
    private static final int COLOR_OUTLINE_FOCUSED = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xE0E0E0;
    private static final int COLOR_CURSOR = 0xFFFFFF;

    public TTextFieldWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public String getText() { return text; }

    public void setText(String text) {
        this.text = text != null ? text : "";
        this.cursorPos = this.text.length();
        this.selectionEnd = this.cursorPos;
    }

    public void setTextChangedListener(TextChangedListener listener) {
        this.listener = listener;
    }

    public void setMaxLength(int maxLength) { this.maxLength = maxLength; }

    @Override
    protected void renderSelf(int mouseX, int mouseY, float partialTicks) {
        boolean highlight = focused && ModernStatistic.config.inputFocusHighlight;

        // Background — slightly lighter when focused
        fill(x, y, getEndX(), getEndY(), highlight ? COLOR_BG_FOCUSED : COLOR_BG);

        // Text
        String display = text;
        if (display.length() > 20) {
            // Scroll display for long text
            int scroll = Math.max(0, cursorPos - 20);
            display = display.substring(scroll, Math.min(scroll + 22, display.length()));
        }
        int textX = x + 4;
        int textY = y + (height - 8) / 2;
        drawString(getFontRenderer(), display, textX, textY, COLOR_TEXT);

        // Cursor blink
        if (focused && (tickCounter / 6) % 2 == 0) {
            int cursorX = textX + getFontRenderer().getStringWidth(
                    display.substring(0, Math.min(cursorPos, display.length())));
            fill(cursorX, textY - 1, cursorX + 1, textY + 9, COLOR_CURSOR);
        }
    }

    @Override
    protected void postRenderSelf(int mouseX, int mouseY, float partialTicks) {
        boolean highlight = focused && ModernStatistic.config.inputFocusHighlight;
        drawOutline(x, y, getEndX(), getEndY(), highlight ? COLOR_OUTLINE_FOCUSED : COLOR_OUTLINE);
    }

    @Override
    protected boolean onMouseClicked(int mouseX, int mouseY, int button) {
        focused = true;
        return true;
    }

    @Override
    public boolean keyTyped(char typedChar, int keyCode) {
        if (!focused) return false;

        tickCounter++;

        if (keyCode == Keyboard.KEY_ESCAPE) {
            focused = false;
            return true;
        }
        if (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) {
            if (cursorPos > 0) {
                text = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
                cursorPos--;
                notifyChange();
            }
            return true;
        }
        if (keyCode == Keyboard.KEY_LEFT && cursorPos > 0) {
            cursorPos--;
            return true;
        }
        if (keyCode == Keyboard.KEY_RIGHT && cursorPos < text.length()) {
            cursorPos++;
            return true;
        }
        if (typedChar >= 32 && typedChar < 127 && text.length() < maxLength) {
            text = text.substring(0, cursorPos) + typedChar + text.substring(cursorPos);
            cursorPos++;
            notifyChange();
            return true;
        }
        return super.keyTyped(typedChar, keyCode);
    }

    private void notifyChange() {
        if (listener != null) listener.onTextChanged(text);
    }
}
