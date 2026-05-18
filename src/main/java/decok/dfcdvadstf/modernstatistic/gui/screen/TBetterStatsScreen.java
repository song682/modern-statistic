package decok.dfcdvadstf.modernstatistic.gui.screen;

import decok.dfcdvadstf.modernstatistic.ModernStatistic;
import decok.dfcdvadstf.modernstatistic.gui.TElement;
import decok.dfcdvadstf.modernstatistic.gui.panel.BSPanel_Downloading;
import decok.dfcdvadstf.modernstatistic.gui.panel.BSPanel_Statistics;
import decok.dfcdvadstf.modernstatistic.gui.panel.stats.*;

import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.resources.I18n;
import net.minecraft.stats.StatFileWriter;

/**
 * The main BetterStats-style statistics screen (Paneled mode).
 * <p>Hosts a root {@link TElement} tree that dispatches rendering and input.</p>
 */
public class TBetterStatsScreen extends GuiScreen implements GuiYesNoCallback {

    // ==================== Tabs ====================

    public enum CurrentTab {
        General, Items, Entities, FoodStuffs, MonstersHunted;

        public String getLocalizedName() {
            switch (this) {
                case General: return I18n.format("stat.generalButton");
                case Items: return I18n.format("stat.itemsButton");
                case Entities: return I18n.format("stat.mobsButton");
                case FoodStuffs: return I18n.format("betterstats.tab.balanced_diet");
                case MonstersHunted: return I18n.format("betterstats.tab.monster_hunter");
                default: return name();
            }
        }
    }

    // ==================== Fields ====================

    protected final GuiScreen parent;
    protected final StatFileWriter statFileWriter;

    protected TElement rootElement;
    protected BSPanel_Downloading panelDownload;
    protected BSPanel_Statistics panelStats;

    protected CurrentTab currentTab = CurrentTab.General;
    protected String searchTerm = "";
    protected double statsScroll = 0;
    protected boolean statsReceived = false;

    /** Pending Wiki URL for GuiConfirmOpenLink confirmation flow. */
    protected String pendingWikiUrl = null;

    // ==================== Constructors ====================

    public TBetterStatsScreen(GuiScreen parent, StatFileWriter statFileWriter) {
        this.parent = parent;
        this.statFileWriter = statFileWriter;
        // Apply default tab from config
        try {
            this.currentTab = CurrentTab.valueOf(ModernStatistic.config.defaultTab);
        } catch (IllegalArgumentException e) {
            this.currentTab = CurrentTab.General;
        }
    }

    // ==================== Getters / Setters ====================

    public GuiScreen getParent() { return parent; }

    public StatFileWriter getStatFileWriter() { return statFileWriter; }

    public CurrentTab getCurrentTab() { return currentTab; }

    public void setCurrentTab(CurrentTab tab) {
        this.currentTab = tab;
        this.statsScroll = 0;
    }

    public String getSearchTerm() { return searchTerm; }

    public void setSearchTerm(String term) { this.searchTerm = term; }

    public boolean isShowEmptyStats() {
        return ModernStatistic.config.showEmptyStats;
    }

    public BSPanel_Statistics getStatPanel() { return panelStats; }

    // ==================== Lifecycle ====================

    @Override
    public void initGui() {
        // Root element fills the screen
        rootElement = new TElement(0, 0, width, height);

        // Create panels
        panelDownload = new BSPanel_Downloading(0, 0, width, height, this);
        panelStats = new BSPanel_Statistics(0, 0, width, height, this);

        rootElement.addChild(panelDownload);
        rootElement.addChild(panelStats);

        // Show downloading first
        panelDownload.setVisible(true);
        panelStats.setVisible(false);

        // Stats are already available from StatFileWriter — skip download phase
        onStatsReady();
    }

    public void onStatsReady() {
        statsReceived = true;
        if (panelDownload != null) panelDownload.setVisible(false);
        if (panelStats != null) {
            panelStats.setVisible(true);
            panelStats.init(this);
        }
    }

    public void refreshStats() {
        if (panelStats != null && statsReceived) {
            panelStats.refreshStatsPanel();
        }
    }

    /** Switch to vanilla GuiStats (the "View vanilla stats" menu action). */
    public void switchToVanillaStats() {
        mc.displayGuiScreen(new GuiStats(parent, statFileWriter));
    }

    /** Open the About screen showing credit and source code links. */
    public void showAboutScreen() {
        mc.displayGuiScreen(new GuiAboutModernStatistic(this));
    }

    // ==================== Rendering ====================

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        if (rootElement != null) {
            rootElement.render(mouseX, mouseY, partialTicks);
            rootElement.postRender(mouseX, mouseY, partialTicks);
        }

        // Draw hover tooltip (simple version)
        drawTooltip(mouseX, mouseY);
    }

    private void drawTooltip(int mouseX, int mouseY) {
        // Simple tooltip: draw item/stat name under cursor
        // (Extended tooltip handled by individual stat widgets)
    }

    // ==================== Input ====================

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (rootElement != null && rootElement.mouseClicked(mouseX, mouseY, button)) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        if (rootElement != null) {
            rootElement.mouseReleased(mouseX, mouseY, button);
        }
        super.mouseMovedOrUp(mouseX, mouseY, button);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        if (rootElement != null) {
            int mouseX = org.lwjgl.input.Mouse.getEventX() * width / mc.displayWidth;
            int mouseY = height - org.lwjgl.input.Mouse.getEventY() * height / mc.displayHeight - 1;
            int wheel = org.lwjgl.input.Mouse.getEventDWheel();
            if (wheel != 0) {
                rootElement.handleMouseScroll(mouseX, mouseY, wheel);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (rootElement != null && rootElement.keyTyped(typedChar, keyCode)) {
            return;
        }
        if (keyCode == 1) { // ESC
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ==================== Wiki link confirmation ====================

    /**
     * Show the "Are you sure you want to open this link?" confirmation dialog
     * before opening a Wiki URL in the browser.
     */
    public void showWikiConfirm(String url) {
        this.pendingWikiUrl = url;
        this.mc.displayGuiScreen(new GuiConfirmOpenLink(this, url, 0, false));
    }

    @Override
    public void confirmClicked(boolean confirmed, int id) {
        if (confirmed && pendingWikiUrl != null) {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(pendingWikiUrl));
            } catch (Exception ignored) {}
        }
        pendingWikiUrl = null;
        this.mc.displayGuiScreen(this);
    }
}
