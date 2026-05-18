package decok.dfcdvadstf.modernstatistic.gui.screen;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;

/**
 * About screen for ModernStatistic — shows two link buttons:
 * <ul>
 *   <li><b>Credit Link</b> — original BetterStats GitHub repo</li>
 *   <li><b>Source Code</b> — ModernStatistic GitHub repo</li>
 * </ul>
 * Both links go through {@link GuiConfirmOpenLink} for safety confirmation.
 */
public class GuiAboutModernStatistic extends GuiScreen implements GuiYesNoCallback {

    private static final String CREDIT_URL = "https://github.com/TheCSDev/mc-better-stats";
    private static final String SOURCE_URL = "https://github.com/song682/modern-statistic";

    private static final int BTN_CREDIT = 0;
    private static final int BTN_SOURCE = 1;
    private static final int BTN_DONE = 2;

    private final GuiScreen parent;
    private String pendingUrl;

    public GuiAboutModernStatistic(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        int centerX = this.width / 2;
        int btnW = 200;
        int btnH = 20;
        int btnX = centerX - btnW / 2;
        int y = this.height / 2 - 40;

        // Credit Link button
        this.buttonList.add(new GuiButton(BTN_CREDIT, btnX, y, btnW, btnH,
                I18n.format("modernstatistic.about.credit_link")));

        y += btnH + 6;

        // Source Code button
        this.buttonList.add(new GuiButton(BTN_SOURCE, btnX, y, btnW, btnH,
                I18n.format("modernstatistic.about.source_code")));

        y += btnH + 16;

        // Done button
        this.buttonList.add(new GuiButton(BTN_DONE, btnX, y, btnW, btnH,
                I18n.format("gui.done")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        // Title
        this.drawCenteredString(this.fontRendererObj,
                I18n.format("modernstatistic.about.title"),
                this.width / 2, this.height / 2 - 60, 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) return;

        switch (button.id) {
            case BTN_CREDIT:
                showLinkConfirm(CREDIT_URL);
                break;
            case BTN_SOURCE:
                showLinkConfirm(SOURCE_URL);
                break;
            case BTN_DONE:
                this.mc.displayGuiScreen(parent);
                break;
        }
    }

    private void showLinkConfirm(String url) {
        this.pendingUrl = url;
        this.mc.displayGuiScreen(new GuiConfirmOpenLink(this, url, 0, false));
    }

    @Override
    public void confirmClicked(boolean confirmed, int id) {
        if (confirmed && pendingUrl != null) {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(pendingUrl));
            } catch (Exception ignored) {}
        }
        pendingUrl = null;
        this.mc.displayGuiScreen(this);
    }
}
