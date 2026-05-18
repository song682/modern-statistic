package decok.dfcdvadstf.modernstatistic.mixin;

import decok.dfcdvadstf.createworldui.api.ContentPanelRenderer;
import decok.dfcdvadstf.createworldui.api.tab.TabState;
import decok.dfcdvadstf.modernstatistic.ModernStatistic;
import decok.dfcdvadstf.modernstatistic.gui.screen.TBetterStatsScreen;
import decok.dfcdvadstf.modernstatistic.tab.StatsGeneralTab;
import decok.dfcdvadstf.modernstatistic.tab.StatsItemsTab;
import decok.dfcdvadstf.modernstatistic.tab.StatsMobsTab;
import decok.dfcdvadstf.modernstatistic.tab.StatsTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.IProgressMeter;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.resources.I18n;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * <p>Transforms the vanilla statistics screen via Mixin to implement a tabbed layout
 * similar to the ModernCreateWorldUI style.</p>
 *
 * <p>Replaces the four bottom category buttons with three top-positioned tab buttons
 * (General, Items, Mobs) and merges Blocks + Items into a single "Items" tab.</p>
 *
 * <p>通过 Mixin 将原版统计界面改造为标签页式布局（类似 ModernCreateWorldUI）。</p>
 * <p>用三个顶部标签页按钮（通用、物品、生物）替代原来的四个底部按钮，
 * 并将方块统计和物品统计合并到一个"物品"标签页中。</p>
 */
@Mixin(GuiStats.class)
public abstract class MixinGuiStats extends GuiScreen {

    // === Vanilla shadows ===

    @Shadow private GuiScreen field_146549_a;
    @Shadow private StatFileWriter field_146546_t;
    @Shadow private boolean doesGuiPauseGame;

    // === New fields ===

    @Unique private StatsTab modernStatistic$tabGeneral;
    @Unique private StatsTab modernStatistic$tabItems;
    @Unique private StatsTab modernStatistic$tabMobs;
    @Unique private StatsTab modernStatistic$currentTab;

    @Unique private static final ResourceLocation MODERN_STATISTIC$TABS_TEXTURE =
            new ResourceLocation("createworldui", "textures/gui/tabs.png");
    @Unique private static final int MODERN_STATISTIC$TAB_WIDTH = 130;
    @Unique private static final int MODERN_STATISTIC$TAB_HEIGHT = 24;


    // ==================== Injections ====================

    /**
     * Intercept the stats-data-arrived callback.
     * Replace vanilla's four inner slot lists with our three tabs.
     */
    @Inject(method = "func_146509_g", at = @At("HEAD"), cancellable = true)
    private void modernStatistic$onStatsReady(CallbackInfo ci) {
        if (!this.doesGuiPauseGame) return;

        // VANILLA mode: do nothing, let vanilla handle everything
        if (ModernStatistic.config.isVanillaMode()) return;

        ci.cancel();

        // PANELED mode: switch to BetterStats-style screen
        if (ModernStatistic.config.isPaneledMode()) {
            this.mc.displayGuiScreen(
                new TBetterStatsScreen(this.field_146549_a, this.field_146546_t));
            return;
        }

        // TABBED mode: existing logic follows
        // Create tabs
        modernStatistic$tabGeneral = new StatsGeneralTab();
        modernStatistic$tabItems = new StatsItemsTab();
        modernStatistic$tabMobs = new StatsMobsTab();

        @SuppressWarnings("unchecked")
        List<GuiButton> btns = this.buttonList;

        modernStatistic$tabGeneral.initGui(
                (GuiStats) (Object) this, this.width, this.height, btns, this.field_146546_t);
        modernStatistic$tabItems.initGui(
                (GuiStats) (Object) this, this.width, this.height, btns, this.field_146546_t);
        modernStatistic$tabMobs.initGui(
                (GuiStats) (Object) this, this.width, this.height, btns, this.field_146546_t);

        // Default to General tab
        modernStatistic$currentTab = modernStatistic$tabGeneral;
        modernStatistic$currentTab.setVisible(true);

        modernStatistic$setupButtons();
        this.doesGuiPauseGame = false;
    }

    // ==================== Button setup ====================

    @Unique
    private void modernStatistic$setupButtons() {
        // Done button — centered, 280px wide
        this.buttonList.add(new GuiButton(0,
                this.width / 2 - 140, this.height - 28,
                280, 20, I18n.format("gui.done")));

        // Create tab buttons at the top
        modernStatistic$createTabButtons();
    }

    @Unique
    private void modernStatistic$createTabButtons() {
        int tabCount = 3;
        int btnW = Math.min(MODERN_STATISTIC$TAB_WIDTH, this.width / tabCount);
        int totalW = btnW * tabCount;
        int startX = this.width / 2 - totalW / 2;

        StatsTab[] tabs = { modernStatistic$tabGeneral, modernStatistic$tabItems, modernStatistic$tabMobs };

        for (int i = 0; i < tabs.length; i++) {
            final StatsTab tab = tabs[i];
            final int xPos = startX + i * btnW;

            if (tab == null) continue;

            boolean enabled = true;
            if (tab instanceof StatsItemsTab && ((StatsItemsTab) tab).isEmpty()) {
                enabled = false;
            }
            if (tab instanceof StatsMobsTab && ((StatsMobsTab) tab).isEmpty()) {
                enabled = false;
            }

            GuiButton btn = new GuiButton(tab.getTabId(), xPos, 0, btnW,
                    MODERN_STATISTIC$TAB_HEIGHT, tab.getTabName()) {
                @Override
                public void drawButton(Minecraft mc, int mouseX, int mouseY) {
                    if (!this.visible) return;

                    mc.getTextureManager().bindTexture(MODERN_STATISTIC$TABS_TEXTURE);
                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

                    boolean hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
                            && mouseX < this.xPosition + this.width
                            && mouseY < this.yPosition + this.height;
                    boolean selected = modernStatistic$currentTab != null
                            && modernStatistic$currentTab.getTabId() == this.id;

                    TabState state;
                    if (selected) {
                        state = hovered ? TabState.SELECTED_HOVER : TabState.SELECTED;
                    } else {
                        state = hovered ? TabState.HOVER : TabState.NORMAL;
                    }

                    drawTexturedModalRect(this.xPosition, this.yPosition,
                            state.u, state.v, this.width, MODERN_STATISTIC$TAB_HEIGHT);
                    int color = this.enabled ? state.getTextColor() : 0x888888;
                    drawCenteredString(mc.fontRenderer, this.displayString,
                            this.xPosition + this.width / 2,
                            this.yPosition + (this.height - 8) / 2, color);
                }
            };
            btn.enabled = enabled;
            btn.visible = true;
            this.buttonList.add(btn);
        }
    }

    // ==================== drawScreen: bottom layer ====================

    /** Draw the vanilla dark border/gradient as the bottommost layer. */
    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void modernStatistic$drawDarkBorder(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        // PANELED mode: TBetterStatsScreen handles its own rendering
        // VANILLA mode: vanilla handles its own rendering
        if (ModernStatistic.config.isPaneledMode() || ModernStatistic.config.isVanillaMode()) return;
        this.drawDefaultBackground();
    }

    // ==================== drawScreen redirects ====================

    @Redirect(method = "drawScreen",
              at = @At(value = "FIELD",
                       target = "Lnet/minecraft/client/gui/IProgressMeter;field_146510_b_:[Ljava/lang/String;"),
              slice = @Slice(
                  from = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/achievement/GuiStats;doesGuiPauseGame:Z"),
                  to = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiSlot;drawScreen(IIF)V")
              ))
    private String[] modernStatistic$redirectField146510_b() {
        // VANILLA mode: return original field value for vanilla path
        return IProgressMeter.field_146510_b_;
    }

    @Redirect(method = "drawScreen",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/gui/GuiSlot;drawScreen(IIF)V"))
    private void modernStatistic$drawTabContent(GuiSlot slot, int mouseX, int mouseY, float partialTicks) {
        // PANELED mode: no-op, TBetterStatsScreen handles rendering
        if (ModernStatistic.config.isPaneledMode()) return;
        // VANILLA mode: call original vanilla drawScreen
        if (ModernStatistic.config.isVanillaMode()) {
            slot.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }
        if (modernStatistic$currentTab != null) {
            modernStatistic$currentTab.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    @Redirect(method = "drawScreen",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/gui/achievement/GuiStats;drawCenteredString(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)V"),
              slice = @Slice(
                  from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiSlot;drawScreen(IIF)V"),
                  to = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;drawScreen(IIF)V")
              ))
    private void modernStatistic$suppressTitle(GuiStats self, FontRenderer fontRenderer, String text, int x, int y, int color) {
        // PANELED mode: no-op
        if (ModernStatistic.config.isPaneledMode()) return;
        // VANILLA mode: draw original vanilla title
        if (ModernStatistic.config.isVanillaMode()) {
            self.drawCenteredString(fontRenderer, text, x, y, color);
            return;
        }
        // title removed in tabbed layout
    }

    @Redirect(method = "drawScreen",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/gui/GuiScreen;drawScreen(IIF)V"),
              slice = @Slice(
                  from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiSlot;drawScreen(IIF)V"),
                  to = @At(value = "RETURN")
              ))
    private void modernStatistic$drawTopBarAndButtons(GuiScreen self, int mouseX, int mouseY, float partialTicks) {
        // PANELED mode: no-op, TBetterStatsScreen handles rendering
        if (ModernStatistic.config.isPaneledMode()) return;
        // VANILLA mode: manually draw buttons (calling self.drawScreen would recurse via polymorphic dispatch)
        if (ModernStatistic.config.isVanillaMode()) {
            for (Object obj : this.buttonList) {
                if (obj instanceof GuiButton) {
                    GuiButton b = (GuiButton) obj;
                    if (b.visible) b.drawButton(this.mc, mouseX, mouseY);
                }
            }
            return;
        }
        // Top bar background behind tabs
        this.mc.getTextureManager().bindTexture(
                new ResourceLocation("createworldui", "textures/gui/options_background_dark.png"));
        modernStatistic$drawTiledTexture(0, 0, this.width,
                MODERN_STATISTIC$TAB_HEIGHT - 2, 16, 16);

        // Header separator — right below the tab row, gapped under selected tab
        // (mirrors ModernCreateWorld: hidden under selected tab so it doesn't overlap the tab visuals)
        int lineY = MODERN_STATISTIC$TAB_HEIGHT - 2;
        int currentTabId = modernStatistic$currentTab != null ? modernStatistic$currentTab.getTabId() : -1;
        int[] tabOrder = {100, 101, 102};
        int tabIndex = -1;
        for (int ti = 0; ti < tabOrder.length; ti++) {
            if (tabOrder[ti] == currentTabId) { tabIndex = ti; break; }
        }

        int btnW = Math.min(MODERN_STATISTIC$TAB_WIDTH, this.width / 3);
        int totalW = btnW * 3;
        int startX = this.width / 2 - totalW / 2;

        if (tabIndex >= 0 && tabIndex < 3) {
            int selectedTabX = startX + tabIndex * btnW;
            int selectedTabEnd = selectedTabX + btnW;
            if (selectedTabX > 0) {
                ContentPanelRenderer.drawHeaderSeparator(0, lineY, selectedTabX);
            }
            if (selectedTabEnd < this.width) {
                ContentPanelRenderer.drawHeaderSeparator(selectedTabEnd, lineY, this.width - selectedTabEnd);
            }
        } else {
            ContentPanelRenderer.drawHeaderSeparator(0, lineY, this.width);
        }

        // Footer separator — above the bottom bar
        ContentPanelRenderer.drawFooterSeparator(0, this.height - 35, this.width);

        // Draw all buttons (including tab buttons and Done)
        for (Object obj : this.buttonList) {
            if (obj instanceof GuiButton) {
                GuiButton b = (GuiButton) obj;
                if (b.visible) {
                    b.drawButton(this.mc, mouseX, mouseY);
                }
            }
        }

        // Hover tooltips for tab buttons
        modernStatistic$drawTabTooltips(mouseX, mouseY, btnW, startX);
    }

    @Unique
    private void modernStatistic$drawTabTooltips(int mouseX, int mouseY,
                                                  int btnW, int startX) {
        if (mouseY < 0 || mouseY > MODERN_STATISTIC$TAB_HEIGHT) return;

        int[] tabIds = {100, 101, 102};
        for (int i = 0; i < tabIds.length; i++) {
            int x = startX + i * btnW;
            if (mouseX >= x && mouseX < x + btnW) {
                String key = "stat.generalButton";
                if (tabIds[i] == 101) key = "stat.itemsButton";
                else if (tabIds[i] == 102) key = "stat.mobsButton";
                String tip = I18n.format(key);
                if (!tip.isEmpty()) {
                    this.drawHoveringText(
                            java.util.Arrays.asList(tip), mouseX, mouseY,
                            this.fontRendererObj);
                }
                return;
            }
        }
    }

    // ==================== actionPerformed ====================

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void modernStatistic$onActionPerformed(GuiButton button, CallbackInfo ci) {
        // PANELED mode: no-op, TBetterStatsScreen handles input
        // VANILLA mode: no-op, vanilla handles button actions
        if (ModernStatistic.config.isPaneledMode() || ModernStatistic.config.isVanillaMode()) return;
        if (!button.enabled) return;

        if (button.id == 0) {
            // Done
            this.mc.displayGuiScreen(this.field_146549_a);
            ci.cancel();
            return;
        }

        // Tab switching
        if (button.id == 100 || button.id == 101 || button.id == 102) {
            modernStatistic$switchToTab(button.id);
            ci.cancel();
            return;
        }

        // Delegate to current tab
        if (modernStatistic$currentTab != null) {
            modernStatistic$currentTab.actionPerformed(button);
        }
        ci.cancel();
    }

    @Unique
    private void modernStatistic$switchToTab(int tabId) {
        if (modernStatistic$currentTab != null) {
            modernStatistic$currentTab.setVisible(false);
        }

        switch (tabId) {
            case 100: modernStatistic$currentTab = modernStatistic$tabGeneral; break;
            case 101: modernStatistic$currentTab = modernStatistic$tabItems;   break;
            case 102: modernStatistic$currentTab = modernStatistic$tabMobs;    break;
        }

        if (modernStatistic$currentTab != null) {
            modernStatistic$currentTab.setVisible(true);
        }
    }

    // ==================== mouse / key ====================

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void modernStatistic$onMouseClicked(int mouseX, int mouseY, int mouseButton,
                                                 CallbackInfo ci) {
        // PANELED mode: no-op, TBetterStatsScreen handles input
        // VANILLA mode: no-op, vanilla handles mouse input
        if (ModernStatistic.config.isPaneledMode() || ModernStatistic.config.isVanillaMode()) return;
        if (modernStatistic$currentTab != null) {
            modernStatistic$currentTab.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void modernStatistic$onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        // PANELED mode: no-op, TBetterStatsScreen handles input
        // VANILLA mode: no-op, vanilla handles key input
        if (ModernStatistic.config.isPaneledMode() || ModernStatistic.config.isVanillaMode()) return;
        if (modernStatistic$currentTab != null) {
            modernStatistic$currentTab.keyTyped(typedChar, keyCode);
        }

        // ESC
        if (keyCode == 1) {
            this.mc.displayGuiScreen(this.field_146549_a);
            ci.cancel();
            return;
        }
        ci.cancel();
    }

    // ==================== Utility ====================

    @Unique
    @SuppressWarnings("unused")
    private void modernStatistic$drawTiledTexture(int x, int y, int width, int height,
                                                   int tileW, int tileH) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        net.minecraft.client.renderer.Tessellator tess =
                net.minecraft.client.renderer.Tessellator.instance;
        tess.startDrawingQuads();

        for (int tx = 0; tx < width; tx += tileW) {
            for (int ty = 0; ty < height; ty += tileH) {
                int w = Math.min(tileW, width - tx);
                int h = Math.min(tileH, height - ty);
                double u2 = (double) w / (double) tileW;
                double v2 = (double) h / (double) tileH;
                tess.addVertexWithUV(x + tx,      y + ty + h,  0.0, 0.0, v2);
                tess.addVertexWithUV(x + tx + w,  y + ty + h,  0.0, u2,  v2);
                tess.addVertexWithUV(x + tx + w,  y + ty,      0.0, u2,  0.0);
                tess.addVertexWithUV(x + tx,      y + ty,      0.0, 0.0, 0.0);
            }
        }

        tess.draw();
    }
}
