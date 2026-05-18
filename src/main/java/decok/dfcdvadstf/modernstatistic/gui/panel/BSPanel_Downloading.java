package decok.dfcdvadstf.modernstatistic.gui.panel;

import decok.dfcdvadstf.modernstatistic.gui.screen.TBetterStatsScreen;
import decok.dfcdvadstf.modernstatistic.gui.screen.TBetterStatsScreen.CurrentTab;
import decok.dfcdvadstf.modernstatistic.gui.widget.TScrollBarWidget;

import net.minecraft.client.resources.I18n;

/**
 * Loading/waiting overlay — mimics BetterStats' {@code BSPanel_Downloading}.
 */
public class BSPanel_Downloading extends BSPanel {

    protected final TBetterStatsScreen screen;

    public BSPanel_Downloading(int x, int y, int width, int height, TBetterStatsScreen screen) {
        super(x, y, width, height);
        this.screen = screen;
        setVisible(false);
    }

    @Override
    protected void renderSelf(int mouseX, int mouseY, float partialTicks) {
        // Semi-transparent background
        fill(x, y, getEndX(), getEndY(), 0x80000000);

        // Centered text
        String text = I18n.format("multiplayer.downloadingStats");
        int cx = x + width / 2;
        int cy = y + height / 2;
        drawCenteredString(getFontRenderer(), text, cx, cy - 4, 0xFFFFAA00);
    }
}
