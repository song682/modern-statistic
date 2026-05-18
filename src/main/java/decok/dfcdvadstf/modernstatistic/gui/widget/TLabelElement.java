package decok.dfcdvadstf.modernstatistic.gui.widget;

import decok.dfcdvadstf.modernstatistic.gui.TElement;

/**
 * Text label with alignment support.
 */
public class TLabelElement extends TElement {

    /** Left alignment. */
    public static final int ALIGN_LEFT = 0;
    /** Center alignment. */
    public static final int ALIGN_CENTER = 1;
    /** Right alignment. */
    public static final int ALIGN_RIGHT = 2;

    protected String text = "";
    protected int alignment = ALIGN_LEFT;
    protected int textColor = 0xFFFFFF;
    protected int hoverTextColor = -1;

    public TLabelElement(int x, int y, int width, int height, String text) {
        super(x, y, width, height);
        this.text = text;
    }

    public TLabelElement(int x, int y, int width, int height) {
        this(x, y, width, height, "");
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public int getAlignment() { return alignment; }
    public void setAlignment(int alignment) { this.alignment = alignment; }

    public int getTextColor() { return textColor; }
    public void setTextColor(int color) { this.textColor = color; }

    public int getHoverTextColor() { return hoverTextColor; }
    public void setHoverTextColor(int color) { this.hoverTextColor = color; }

    @Override
    protected void renderSelf(int mouseX, int mouseY, float partialTicks) {
        if (text == null || text.isEmpty()) return;
        int color = (hoverTextColor >= 0 && hovered) ? hoverTextColor : textColor;
        int textY = y + (height - 8) / 2;
        switch (alignment) {
            case ALIGN_CENTER:
                drawCenteredString(getFontRenderer(), text, x + width / 2, textY, color);
                break;
            case ALIGN_RIGHT:
                drawString(getFontRenderer(), text,
                        x + width - getFontRenderer().getStringWidth(text), textY, color);
                break;
            default:
                drawString(getFontRenderer(), text, x, textY, color);
                break;
        }
    }
}
