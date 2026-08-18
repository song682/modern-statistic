package decok.dfcdvadstf.modernstatistic.gui.screen;

import decok.dfcdvadstf.catframe.ui.ContentPanelRenderer;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.catframe.ui.components.TabButton;
import decok.dfcdvadstf.catframe.ui.components.events.KeyTypedEvent;
import decok.dfcdvadstf.catframe.ui.components.tab.AbstractScreenTab;
import decok.dfcdvadstf.catframe.ui.components.tab.Tab;
import decok.dfcdvadstf.catframe.ui.components.tab.TabBar;
import decok.dfcdvadstf.catframe.ui.components.tab.TabManager;
import decok.dfcdvadstf.catframe.ui.layouts.HeaderFooterLayout;
import decok.dfcdvadstf.catframe.ui.navigation.ScreenRectangle;
import decok.dfcdvadstf.catframe.ui.overlay.OverlayManager;
import decok.dfcdvadstf.catframe.ui.screens.Screen;
import decok.dfcdvadstf.modernstatistic.gui.tab.ModernStatisticTabBar;
import decok.dfcdvadstf.modernstatistic.gui.tab.StatsGeneralTab;
import decok.dfcdvadstf.modernstatistic.gui.tab.StatsItemsTab;
import decok.dfcdvadstf.modernstatistic.gui.tab.StatsMobsTab;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.stats.StatFileWriter;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Tabbed statistics screen (TABBED layout mode) — built on the CatFrame
 * {@link Screen} base
 * while following the BetterStats Screen (BSS) design pattern.<br>
 * Hosts the CatFrame {@link TabBar}/{@link TabManager} navigation together with
 * the
 * General / Items / Mobs tabs registered in the {@code TabRegistry}, replacing
 * the vanilla
 * four-category-button layout with a top-positioned tab bar.
 * </p>
 * <p>
 * This screen owns all the tabbed rendering and input logic that used to live
 * inside
 * {@code MixinGuiStats}; the mixin now only routes TABBED mode to this screen.
 * </p>
 *
 * <p>
 * 标签页式统计界面（TABBED 布局模式）——基于 CatFrame {@link Screen} 基类，同时遵循
 * BetterStats Screen（BSS）设计模式。<br>
 * 由 {@link TabBar}/{@link TabManager} 承载导航，配合注册在 {@code TabRegistry} 中的
 * 通用 / 物品 / 生物三个标签页，以顶部标签栏替代原版四个分类按钮。
 * </p>
 * <p>
 * 本界面接管了原先写在 {@code MixinGuiStats} 里的全部标签页渲染与输入逻辑；
 * Mixin 现在只负责把 TABBED 模式路由到本界面。
 * </p>
 * <p>
 * The screen is built on a {@link HeaderFooterLayout}: the {@link TabBar} is
 * the
 * header zone, the Done button is the footer zone, and the tab content fills
 * the
 * content zone (set via {@link TabManager#setTabArea}).
 * 界面基于 {@link HeaderFooterLayout} 构建：{@link TabBar} 为 header 区域，
 * Done 按钮为 footer 区域，标签页内容填充内容区（经 {@link TabManager#setTabArea} 设置）。
 * </p>
 */
public class GuiStatics extends Screen implements GuiYesNoCallback {

    // ==================== Constants ====================

    /** TabRegistry ids of our tabs / 我们标签页的注册 ID */
    private static final int TAB_GENERAL = 105;
    private static final int TAB_ITEMS = 106;
    private static final int TAB_MOBS = 107;

    /**
     * Footer separator offset from the bottom (mirrors vanilla GuiStats) / 底部
     * Footer 分隔线距底部偏移（与原版 GuiStats 一致）
     */
    private static final int FOOTER_OFFSET = 35;

    /**
     * Content zone top — the header separator Y that the TabBar draws at
     * {@code NAV_HEIGHT - 2}; the content panel fills the area between this
     * line and the footer separator.
     * 内容区顶部 —— TabBar 在 {@code NAV_HEIGHT - 2} 处绘制的 Header 分隔线 Y；
     * 内容面板填充该线与 Footer 分隔线之间的区域。
     */
    private static final int CONTENT_TOP = TabBar.NAV_HEIGHT - 2;

    // ==================== Fields ====================

    /** The screen to return to when this screen closes / 关闭本界面时返回的屏幕 */
    protected final GuiScreen parent;
    /**
     * The stat file writer providing the actual stat values / 提供实际统计值的
     * StatFileWriter
     */
    protected final StatFileWriter statFileWriter;

    /** Navigation bar (background + tab buttons) / 导航栏（背景 + 标签按钮） */
    protected TabBar tabBar;
    /**
     * Tab manager — loads tabs from the registry and handles switching /
     * 标签页管理器——从注册表加载标签页并处理切换
     */
    protected TabManager tabManager;

    /**
     * Three-zone layout: header = TabBar, footer = action buttons, content = tabs.
     * The layout owns the header/footer zone positions; the content zone rectangle
     * is handed to the {@link TabManager} via {@link TabManager#setTabArea}.
     * 三区域布局：header = TabBar，footer = 操作按钮，content = 标签页。
     * 布局负责 header/footer 区域的定位；内容区矩形经 {@link TabManager#setTabArea}
     * 交给标签页管理器。
     */
    protected HeaderFooterLayout layout;

    /** Our three tab instances / 我们的三个标签页实例 */
    protected AbstractScreenTab tabGeneral;
    protected AbstractScreenTab tabItems;
    protected AbstractScreenTab tabMobs;
    /** The currently visible tab / 当前可见的标签页 */
    protected AbstractScreenTab currentTab;

    /** Done button — closes the screen back to the parent / 完成按钮——关闭界面返回父界面 */
    private Button doneButton;

    /**
     * Pending Wiki URL for the GuiConfirmOpenLink confirmation flow (BSS pattern) /
     * 待确认的 Wiki 链接（BSS 模式）
     */
    protected String pendingWikiUrl = null;

    // ==================== Constructors ====================

    /**
     * Create the tabbed statistics screen.
     * <p>
     * 创建标签页式统计界面。
     * </p>
     *
     * @param parent         the screen to return to on close / 关闭时返回的屏幕
     * @param statFileWriter the stat file writer / 统计文件写入器
     */
    public GuiStatics(GuiScreen parent, StatFileWriter statFileWriter) {
        super(Text.translatable("stats.title"));
        this.parent = parent;
        this.statFileWriter = statFileWriter;
    }

    // ==================== Getters ====================

    /** @return the screen to return to on close / 关闭时返回的屏幕 */
    public GuiScreen getParent() {
        return parent;
    }

    /** @return the stat file writer / 统计文件写入器 */
    public StatFileWriter getStatFileWriter() {
        return statFileWriter;
    }

    /** @return the currently visible tab / 当前可见的标签页 */
    public AbstractScreenTab getCurrentTab() {
        return currentTab;
    }

    // ==================== Lifecycle ====================

    /**
     * Build the tabbed UI: create the TabBar + TabManager, custom-initialise the
     * three tabs,
     * register them into the nav bar, disable empty tabs, show the default tab
     * (General)
     * and add the Done button.
     * <p>
     * 构建标签页界面：创建 TabBar + TabManager、自定义初始化三个标签页、
     * 注册进导航栏、禁用空标签页、显示默认标签页（General）并添加 Done 按钮。
     * </p>
     */
    @Override
    protected void init() {
        // Rebuild from scratch on every init (including window resize)
        // 每次初始化（含窗口缩放）都从零重建
        buttonList.clear();
        @SuppressWarnings("unchecked")
        List<GuiButton> btns = buttonList;

        // Create TabBar + TabManager — TabManager loads and instantiates our registered
        // tabs
        // 创建 TabBar + TabManager —— TabManager 会加载并实例化已注册的标签页
        tabBar = new ModernStatisticTabBar();
        tabManager = new TabManager(this, btns, width, height, tabBar);

        // Grab the tab instances by their registered ids (105/106/107)
        // 按注册 ID（105/106/107）获取标签页实例
        collectTabs();
        // Custom init: build the selection lists inside each tab (stats are already
        // ready here) and register them into the widget pipeline
        // 自定义初始化：在标签页内部构建选择列表（此时统计数据已就绪）并注册进组件管线
        initTabs(btns);
        // Register the actual instances into the TabBar for TabButton creation
        // （TabManager 构造时只注册了 entries，需要注册实际实例才能创建 TabButton）
        registerTabsToBar();
        // Disable tab buttons whose content is empty (Items/Mobs)
        // 禁用内容为空的标签页按钮（物品 / 生物）
        disableEmptyTabs();
        // Show the default tab (General), hide the others
        // 显示默认标签页（General），隐藏其余
        applyDefaultTab();

        // Three-zone layout: header = TabBar, footer = action buttons, content = tabs.
        // The layout owns the zone positions; the content zone rectangle is derived
        // from the header/footer separators (see the setTabArea call below).
        // 三区域布局：header = TabBar，footer = 操作按钮，content = 标签页。
        // 布局负责区域定位；内容区矩形由 Header/Footer 分隔线推导（见下方 setTabArea）。
        layout = new HeaderFooterLayout(TabBar.NAV_HEIGHT, FOOTER_OFFSET);
        layout.setHeader(tabBar);

        // Done button — CatFrame Button with vanilla texture (identical look to the
        // vanilla GuiButton it replaces); press callback returns to the parent screen.
        // Its position is managed by the layout's footer zone (centered).
        // 完成按钮——CatFrame Button，使用原版纹理（与被替换的原版 GuiButton 外观一致）；
        // 按下回调返回父界面。位置由布局的 footer 区域管理（居中）。
        doneButton = Button.builder(Text.translatable("gui.done"),
                b -> this.mc.displayGuiScreen(parent))
                .width(280)
                .height(20)
                .useVanillaTexture(true)
                .build();
        layout.addToFooter(doneButton);
        addRenderableWidget(doneButton);

        // Apply the layout: header pinned to the top, footer pinned to the bottom
        // 应用布局：header 固定顶部，footer 固定底部
        layout.recalculate(width, height);

        // Content zone — the rectangle between the header separator (y=22) and the
        // footer separator (y=height-35). Tabs fill it via doLayout on every switch.
        // 内容区 —— Header 分隔线（y=22）与 Footer 分隔线（y=height-35）之间的矩形；
        // 标签页在每次切换时通过 doLayout 填充该区域。
        tabManager.setTabArea(new ScreenRectangle(0, CONTENT_TOP, width,
                height - CONTENT_TOP - FOOTER_OFFSET));
    }

    /**
     * Assign our three tab instances from the TabManager. / 从 TabManager
     * 获取我们的三个标签页实例。
     */
    private void collectTabs() {
        for (Tab tab : tabManager.getAllTabs().values()) {
            if (tab instanceof AbstractScreenTab) {
                AbstractScreenTab st = (AbstractScreenTab) tab;
                switch (st.getTabId()) {
                    case TAB_GENERAL:
                        tabGeneral = st;
                        break;
                    case TAB_ITEMS:
                        tabItems = st;
                        break;
                    case TAB_MOBS:
                        tabMobs = st;
                        break;
                }
            }
        }
    }

    /**
     * Custom-init each tab: create its selection list (hidden until selected) and
     * register it into the screen's widget pipeline so rendering, clicks and the
     * scroll wheel are dispatched automatically.
     * <p>
     * 自定义初始化每个标签页：创建其选择列表（选中前隐藏），并注册进界面的
     * 组件管线，渲染 / 点击 / 滚轮自动分发。
     * </p>
     */
    private void initTabs(List<GuiButton> btns) {
        if (tabGeneral instanceof StatsGeneralTab) {
            StatsGeneralTab t = (StatsGeneralTab) tabGeneral;
            t.initGui(width, height, btns, statFileWriter);
            addRenderableWidget(t.getList());
        }
        if (tabItems instanceof StatsItemsTab) {
            StatsItemsTab t = (StatsItemsTab) tabItems;
            t.initGui(width, height, btns, statFileWriter);
            addRenderableWidget(t.getList());
        }
        if (tabMobs instanceof StatsMobsTab) {
            StatsMobsTab t = (StatsMobsTab) tabMobs;
            t.initGui(width, height, btns, statFileWriter);
            addRenderableWidget(t.getList());
        }
    }

    /**
     * Register the actual tab instances into the TabBar for navigation rendering. /
     * 将实际标签页实例注册进 TabBar 供导航渲染。
     */
    private void registerTabsToBar() {
        for (int id : tabManager.getSortedTabIds()) {
            Tab tab = tabManager.getAllTabs().get(id);
            if (tab != null) {
                tabBar.registerTab(tab);
            }
        }
        tabBar.setNavWidth(width);
    }

    /** Disable tab buttons whose content is empty. / 禁用内容为空的标签页按钮。 */
    private void disableEmptyTabs() {
        for (int id : tabManager.getSortedTabIds()) {
            TabButton btn = tabBar.getTabButton(id);
            if (btn != null) {
                Tab t = btn.getTab();
                if ((t instanceof StatsItemsTab && ((StatsItemsTab) t).isEmpty())
                        || (t instanceof StatsMobsTab && ((StatsMobsTab) t).isEmpty())) {
                    btn.setActive(false);
                }
            }
        }
    }

    /**
     * Show the default tab — fixed to General in TABBED mode. The
     * {@code defaultTab}
     * config option belongs to the PANELED screen ({@link TBetterStatsScreen}),
     * whose
     * panel set (incl. BalancedDiet/MonsterHunter) does not map onto our three
     * tabs.
     * <p>
     * TABBED 模式的默认标签页固定为 General——{@code defaultTab} 配置属于 PANELED 界面
     * （{@link TBetterStatsScreen}），其面板集合（含 BalancedDiet/MonsterHunter）
     * 与本界面的三个标签页不对应。
     * </p>
     */
    private void applyDefaultTab() {
        AbstractScreenTab def = tabGeneral;
        // setCurrentTab is a no-op when the tab is already current, so force visibility
        // setCurrentTab 在目标已是当前标签页时不会执行任何操作，因此需要强制设置可见性
        tabManager.setCurrentTab(def, false);
        def.setVisible(true);
        currentTab = def;
    }

    // ==================== Rendering ====================

    /**
     * Draw the vanilla dirt background over the whole screen via
     * {@link GuiScreen#drawBackground(int)} — the same way the vanilla stats screen
     * (GuiStats extends GuiScreen) draws it. The nav bar / content black fills are
     * drawn on top later, leaving the footer zone on the dirt texture.
     * <p>
     * 整个界面用 {@link GuiScreen#drawBackground(int)} 绘制原版泥土背景——与原版统计界面
     * （GuiStats extends GuiScreen）一致。导航栏 / 内容区的黑色背景随后绘制在其上，
     * 因此 Footer 区域最终显示泥土纹理。
     * </p>
     */
    @Override
    protected void renderBackground(int mouseX, int mouseY, float partialTicks) {
        this.drawBackground(0);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Background + CatFrame renderables (incl. the tab selection lists) + vanilla
        // buttonList
        // 背景 + CatFrame 可渲染组件（含标签页选择列表） + 原版按钮列表
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Draw the current tab's content (rendered by the component pipeline)
        // 绘制当前标签页内容（由组件管线渲染）
        if (tabManager != null) {
            tabManager.drawScreen(mouseX, mouseY, partialTicks);
        }

        // Draw the nav bar (background + TabButton instances + header separator)
        // 绘制导航栏（背景 + TabButton 实例 + 顶部标题分隔线）
        if (tabBar != null && tabManager != null) {
            tabBar.drawNavButtons(mouseX, mouseY, partialTicks, tabManager);
        }

        // Footer separator — above the bottom bar
        // 底部 Footer 分隔线——在底栏上方
        ContentPanelRenderer.drawFooterSeparator(0, height - FOOTER_OFFSET, width);

        // Hover tooltips for tab buttons (drawNavButtons already updated hover state)
        // 标签页按钮悬停提示（drawNavButtons 已更新悬停状态）
        drawTabTooltips(mouseX, mouseY);

        // Render all registered overlays (e.g. right-click popup menu)
        // 渲染所有已注册的 Overlay（如右键弹出菜单）
        OverlayManager.INSTANCE.renderAll(mouseX, mouseY, partialTicks);
    }

    /**
     * Draw hover tooltips for the tab buttons in the nav bar. / 绘制导航栏标签页按钮的悬停提示。
     */
    private void drawTabTooltips(int mouseX, int mouseY) {
        if (mouseY < 0 || mouseY >= TabBar.NAV_HEIGHT)
            return;
        if (tabManager == null || tabBar == null)
            return;

        List<Integer> sortedIds = tabManager.getSortedTabIds();
        for (int id : sortedIds) {
            TabButton btn = tabBar.getTabButton(id);
            if (btn != null && btn.isHovered()) {
                Tab tab = btn.getTab();
                if (tab != null) {
                    String tip = tab.getTabName();
                    if (tip != null && !tip.isEmpty()) {
                        drawHoveringText(Arrays.asList(tip), mouseX, mouseY, fontRendererObj);
                    }
                }
                return;
            }
        }
    }

    // ==================== Input ====================

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // Vanilla buttons + CatFrame component dispatch (incl. the selection lists)
        // 先处理原版按钮 + CatFrame 组件分发（含选择列表）
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // Overlay clicks (right-click popup menus) — mirror TBetterStatsScreen
        // Overlay 点击（右键弹出菜单）——与 TBetterStatsScreen 一致
        if (OverlayManager.INSTANCE.handleMouseClick(mouseX, mouseY, mouseButton)) {
            return;
        }

        // Tab switching via TabBar navigation (TabButton clicks, active tabs only)
        // 通过 TabBar 导航处理标签切换（TabButton 点击，仅可用标签）
        if (handleTabClick(mouseX, mouseY, mouseButton)) {
            return;
        }

        // Footer action buttons (Done). The super call above only reaches the
        // vanilla buttonList — the CatFrame Screen dispatch is invisible to javac
        // (the released jar reobfs it to func_73864_a), so the CatFrame Button needs
        // an explicit forward.
        // 底部操作按钮（Done）。上面的 super 调用只处理原版 buttonList——CatFrame 的
        // Screen 分发对 javac 不可见（发布的 jar 将其 reobf 为 func_73864_a），
        // 故 CatFrame Button 需要显式转发。
        if (doneButton != null && doneButton.isMouseOver(mouseX, mouseY)) {
            doneButton.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        // Forward to the current tab (header clicks etc. are handled by the list)
        // 转发给当前标签页（表头点击等由列表自身处理）
        if (tabManager != null) {
            tabManager.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    /**
     * Handle TabButton clicks; returns true if a tab switch happened. / 处理
     * TabButton 点击；发生切换时返回 true。
     */
    private boolean handleTabClick(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0)
            return false;
        if (mouseY < 0 || mouseY >= TabBar.NAV_HEIGHT)
            return false;
        if (tabManager == null || tabBar == null)
            return false;

        List<Integer> sortedIds = tabManager.getSortedTabIds();
        for (int id : sortedIds) {
            TabButton btn = tabBar.getTabButton(id);
            if (btn != null && btn.isMouseOver(mouseX, mouseY) && btn.isActive()) {
                // Let TabManager handle the switch (widget lifecycle + sound + callbacks)
                // 让 TabManager 处理切换（控件生命周期 + 音效 + 回调）
                tabManager.setCurrentTab(btn.getTab(), true);
                // Sync currentTab reference for drawScreen
                // 同步 currentTab 引用供 drawScreen 使用
                Tab current = tabManager.getCurrentTab();
                if (current instanceof AbstractScreenTab) {
                    currentTab = (AbstractScreenTab) current;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Vanilla {@code keyTyped} — re-implemented here as {@code public} to satisfy
     * the CatFrame
     * {@code Component} contract at compile time: the released
     * {@code CatFrame-v0.6.2.jar}
     * reobfs this method to {@code func_73869_a}, so javac (MCP names) cannot see
     * the
     * base-class override. Handles only Esc-to-close, mirroring the base
     * implementation;
     * split key events are dispatched via {@code ScreenKeyboardInput} instead.
     * <p>
     * 原版 {@code keyTyped}——在此以 public 重新实现，以满足 CatFrame {@code Component}
     * 接口的编译期契约：发布的 {@code CatFrame-v0.6.2.jar} 将该方法 reobf 为
     * {@code func_73869_a}，javac（MCP 名）无法看到基类覆盖。只处理 Esc 关闭，与基类
     * 实现一致；拆分键盘事件由 {@code ScreenKeyboardInput} 派发。
     * </p>
     */
    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE && shouldCloseOnEsc() && this.mc.currentScreen == this) {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode) {
        // Ctrl+Tab / Ctrl+Shift+Tab cycling, Ctrl+digit direct jump via the TabBar
        // 通过 TabBar 处理 Ctrl+Tab / Ctrl+Shift+Tab 循环切换、Ctrl+数字直接跳转
        if (tabBar != null && tabManager != null) {
            boolean ctrl = KeyTypedEvent.isControlKeyPressed();
            boolean shift = KeyTypedEvent.isShiftKeyPressed();
            if (tabBar.keyPressedNav(keyCode, ctrl, shift, tabManager)) {
                syncCurrentTab();
                return true;
            }
        }
        return super.keyPressed(keyCode);
    }

    /**
     * Sync the {@link #currentTab} reference after a TabManager-side switch. /
     * TabManager 侧切换后同步 {@link #currentTab} 引用。
     */
    private void syncCurrentTab() {
        Tab current = tabManager.getCurrentTab();
        if (current instanceof AbstractScreenTab) {
            currentTab = (AbstractScreenTab) current;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled)
            return;

        // Delegate to the tab manager (tab switching)
        // 委托给标签页管理器（标签切换）
        if (tabManager != null) {
            tabManager.actionPerformed(button);
        }
    }

    // ==================== Close / Pause ====================

    /**
     * Esc/Done close this screen — return to the parent screen. / Esc / Done
     * 关闭本界面——返回父界面。
     */
    @Override
    public void onClose() {
        this.mc.displayGuiScreen(parent);
    }

    /**
     * The statistics screen must not pause the world (matches the Mixin behaviour).
     * / 统计界面不暂停世界（与 Mixin 行为一致）。
     */
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ==================== Wiki link confirmation ====================

    /**
     * Show the "Are you sure you want to open this link?" confirmation dialog
     * before opening a Wiki URL in the browser (BSS pattern).
     * <p>
     * 在浏览器中打开 Wiki 链接前显示"确定要打开此链接吗？"确认对话框（BSS 模式）。
     * </p>
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
            } catch (Exception ignored) {
            }
        }
        pendingWikiUrl = null;
        this.mc.displayGuiScreen(this);
    }
}