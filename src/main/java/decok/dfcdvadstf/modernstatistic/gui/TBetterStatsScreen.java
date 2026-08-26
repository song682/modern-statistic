package decok.dfcdvadstf.modernstatistic.gui;

import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.screens.Screen;
import decok.dfcdvadstf.modernstatistic.ModernStatistic;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.TElement;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.WikiLinkHandler;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.panel.BSPanel_Downloading;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.panel.BSPanel_Statistics;
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
public class TBetterStatsScreen extends Screen implements GuiYesNoCallback, WikiLinkHandler {

    // ==================== Tabs ====================

    public enum CurrentTab {
        General, Blocks, Items, Entities, FoodStuffs, MonstersHunted;

        public String getLocalizedName() {
            switch (this) {
                case General: return I18n.format("stat.generalButton");
                case Blocks: return I18n.format("stat.blocksButton");
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
        super(Text.translatable("stats.title"));
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

    /**
     * Build the UI: the root {@link TElement} fills the screen and hosts the
     * downloading/stats panels. CatFrame's {@code Screen} calls this from
     * {@code initGui()} on first show and every resize.
     * <p>
     * 构建界面：根 {@link TElement} 填满屏幕并承载下载/统计面板。CatFrame 的
     * {@code Screen} 在首次显示与每次缩放时经 {@code initGui()} 调用本方法。
     * </p>
     */
    @Override
    protected void init() {
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

    // ==================== Rendering ====================

    /**
     * Render pipeline: CatFrame base draws background + renderables + vanilla
     * buttons, then the root {@link TElement} tree draws the panels (item
     * icons included). Overlays (right-click popup) render last — both here
     * and via {@code ClientOverlayHandler} on {@code DrawScreenEvent.Post} —
     * so the popup always lands on top of the item icon and the display
     * elements underneath it.
     * <p>
     * 渲染管线：CatFrame 基类绘制背景 + 可渲染组件 + 原版按钮，随后根
     * {@link TElement} 树绘制面板（含物品图标）。Overlay（右键弹出菜单）
     * 最后渲染——此处与 {@code ClientOverlayHandler} 的
     * {@code DrawScreenEvent.Post} 都会渲染——保证弹窗始终位于物品图标
     * 及其下所有 display element 之上。
     * </p>
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Reset CatFrame deferred rendering pipeline for this frame
        // 重置 CatFrame 延迟渲染管线
        decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor.getInstance().resetForNewFrame();
        
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (rootElement != null) {
            rootElement.render(mouseX, mouseY, partialTicks);
            rootElement.postRender(mouseX, mouseY, partialTicks);
        }

        // Render all registered overlays (e.g. right-click popup menu)
        // 渲染所有已注册的 Overlay（如右键弹出菜单）——保持在最上层
        decok.dfcdvadstf.catframe.ui.overlay.OverlayManager.INSTANCE.renderAll(mouseX, mouseY, partialTicks);
        
        // Flush deferred elements (items + tooltips) — must be called after all rendering
        // 刷新延迟元素（物品 + tooltip）——必须在所有渲染完成后调用
        decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor.getInstance().extractDeferredElements();
    }

    // ==================== Input ====================

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        // Blocking overlays get first crack at mouse input
        if (decok.dfcdvadstf.catframe.ui.overlay.OverlayManager.INSTANCE.handleMouseClick(mouseX, mouseY, button)) {
            return;
        }
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

    /**
     * Vanilla {@code keyTyped} — re-implemented here as {@code public} to satisfy
     * the CatFrame {@code Component} contract at compile time: the released
     * CatFrame jar reobfs this method to {@code func_73869_a}, so javac (MCP
     * names) cannot see the base-class override. The root {@link TElement} tree
     * is not part of CatFrame's component tree, so its keyboard dispatch is
     * forwarded explicitly; Esc returns to the parent screen.
     * <p>
     * 原版 {@code keyTyped}——在此以 public 重新实现，以满足 CatFrame
     * {@code Component} 接口的编译期契约：发布的 CatFrame jar 将该方法 reobf 为
     * {@code func_73869_a}，javac（MCP 名）无法看到基类覆盖。根 {@link TElement}
     * 树不属于 CatFrame 组件树，因此其键盘派发在此显式转发；Esc 返回父界面。
     * </p>
     */
    @Override
    public void keyTyped(char typedChar, int keyCode) {
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
