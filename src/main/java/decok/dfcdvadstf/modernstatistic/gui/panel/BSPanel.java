package decok.dfcdvadstf.modernstatistic.gui.panel;

import decok.dfcdvadstf.modernstatistic.gui.TPanelElement;

/**
 * Base panel with black outline — mimics BetterStats' {@code BSPanel}.
 */
public class BSPanel extends TPanelElement {

    public static final int COLOR_OUTLINE = 0xFF000000;

    public BSPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    protected void postRenderSelf(int mouseX, int mouseY, float partialTicks) {
        super.postRenderSelf(mouseX, mouseY, partialTicks);
        drawOutline(x, y, getEndX(), getEndY(), COLOR_OUTLINE);
    }
}
