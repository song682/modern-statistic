package decok.dfcdvadstf.modernstatistic.gui.betterstats.panel;

import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.modernstatistic.gui.TBetterStatsScreen;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.overlay.DropDownPanel;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.widget.TButtonWidget;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

/**
 * Simplified top menu bar for the BetterStats screen.
 * <p>The About entry opens a drop-down menu (source code / credits /
 * download links), replacing the former {@code GuiAboutModernStatistic} screen.</p>
 * <p>顶部菜单栏：About 入口改为下拉菜单（源代码 / 致谢 / 下载链接），
 * 替代原先的 {@code GuiAboutModernStatistic} 界面。</p>
 */
public class BSPanel_StatisticsMenuBar extends BSPanel {

    /** ModernStatistic source code repository / ModernStatistic 源代码仓库 */
    private static final String SOURCE_URL = "https://github.com/song682/modern-statistic";
    /** Original BetterStats repository (credits) / 原版 BetterStats 仓库（致谢） */
    private static final String CREDIT_URL = "https://github.com/TheCSDev/mc-better-stats";
    /** CurseForge download page / CurseForge 下载页面 */
    private static final String CURSEFORGE_URL = "https://www.curseforge.com/minecraft/mc-mods/modern-statistics";
    /** Modrinth download page / Modrinth 下载页面 */
    private static final String MODRINTH_URL = "https://modrinth.com/mod/modernstatistics";
    /** Icon of the source-code menu item / 源代码菜单项图标 */
    private static final ResourceLocation SOURCE_ICON =
            new ResourceLocation("modernstatistic", "textures/gui/statsicon/code.png");
    /** Icon of the CurseForge menu item / CurseForge 菜单项图标 */
    private static final ResourceLocation CURSEFORGE_ICON =
            new ResourceLocation("modernstatistic", "textures/gui/statsicon/curse.png");
    /** Icon of the Modrinth menu item / Modrinth 菜单项图标 */
    private static final ResourceLocation MODRINTH_ICON =
            new ResourceLocation("modernstatistic", "textures/gui/statsicon/modrinth.png");
    /** Icon of the "view vanilla stats" menu item / “查看原版统计界面”菜单项图标 */
    private static final ResourceLocation VANILLA_STATS_ICON =
            new ResourceLocation("modernstatistic", "textures/gui/statsicon/vanilla_statics.png");

    private static final int BTN_HEIGHT = 14;
    private static final int BTN_SPACING = 4;

    protected final TBetterStatsScreen screen;

    public BSPanel_StatisticsMenuBar(int x, int y, int width, int height, TBetterStatsScreen screen) {
        super(x, y, width, height);
        this.screen = screen;
        setScrollPadding(2);
    }

    public void init() {
        clearChildren();

        int btnX = getX() + getScrollPadding();
        int btnY = getY() + 1;

        // View: drop-down menu — the item jumps to the stats screen picked
        // by the defaultUILayout config (vanilla GuiStats or tabbed GuiStatics).
        // View：下拉菜单——菜单项按下后按 defaultUILayout 配置跳转到
        // 对应模式的统计界面（原版 GuiStats 或标签页式 GuiStatics）。
        TButtonWidget btnView = new TButtonWidget(btnX, btnY, 100, BTN_HEIGHT,
                I18n.format("betterstats.gui.menu_bar.view"),
                btn -> {
                    DropDownPanel menu = new DropDownPanel(btn);
                    // "View vanilla stats" item with its icon / 菜单项带图标
                    DropDownPanel.MenuItem vanillaItem = DropDownPanel.item(
                            Text.translatable("betterstats.gui.menu_bar.view.vanilla_stats"),
                            screen::switchToDefaultStats);
                    vanillaItem.icon(VANILLA_STATS_ICON);
                    menu.addItem(vanillaItem);
                    menu.show();
                });
        addChild(btnView, false);
        btnX += 100 + BTN_SPACING;

        // About: drop-down menu with source code / credits / download links,
        // all going through the wiki-style link confirmation flow.
        // About：下拉菜单提供源代码 / 致谢 / 下载链接，均走 Wiki 式链接确认流程。
        TButtonWidget btnAbout = new TButtonWidget(btnX, btnY, 80, BTN_HEIGHT,
                I18n.format("betterstats.gui.menu_bar.about"),
                btn -> {
                    DropDownPanel menu = new DropDownPanel(btn);
                    // Source Code item with its icon / 源代码项带图标
                    DropDownPanel.MenuItem sourceItem = DropDownPanel.item(
                            Text.translatable("betterstats.gui.menu_bar.about.source"),
                            () -> screen.showWikiConfirm(SOURCE_URL));
                    sourceItem.icon(SOURCE_ICON);
                    menu.addItem(sourceItem);
                    menu.addItem(Text.translatable("betterstats.gui.menu_bar.about.credits"),
                            () -> screen.showWikiConfirm(CREDIT_URL));
                    // Separator between info links and download links / 信息链接与下载链接之间的分隔线
                    menu.addSeparator();
                    // Download items with their icons / 下载链接项带图标
                    DropDownPanel.MenuItem curseItem = DropDownPanel.item(
                            Text.translatable("betterstats.gui.menu_bar.about.curseforge"),
                            () -> screen.showWikiConfirm(CURSEFORGE_URL));
                    curseItem.icon(CURSEFORGE_ICON);
                    menu.addItem(curseItem);
                    DropDownPanel.MenuItem modrinthItem = DropDownPanel.item(
                            Text.translatable("betterstats.gui.menu_bar.about.modrinth"),
                            () -> screen.showWikiConfirm(MODRINTH_URL));
                    modrinthItem.icon(MODRINTH_ICON);
                    menu.addItem(modrinthItem);
                    menu.show();
                });
        addChild(btnAbout, false);
    }
}
