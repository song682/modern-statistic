package decok.dfcdvadstf.modernstatistic.mixin;

import decok.dfcdvadstf.modernstatistic.ModernStatistic;
import decok.dfcdvadstf.modernstatistic.gui.GuiStatics;
import decok.dfcdvadstf.modernstatistic.gui.TBetterStatsScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.stats.StatFileWriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * <p>
 * Routes the vanilla statistics screen to the ModernStatistic UI depending on
 * the
 * configured layout mode (TABBED / PANELED / VANILLA).<br>
 * All tabbed rendering and input logic lives in {@link GuiStatics}; the paneled
 * UI lives in {@link TBetterStatsScreen}. This mixin only switches screens once
 * the stats data has arrived.
 * </p>
 * <p>
 * 根据配置的布局模式（TABBED / PANELED / VANILLA）将原版统计界面路由到
 * ModernStatistic 的界面。<br>
 * 标签页式的渲染与输入逻辑全部在 {@link GuiStatics} 中，面板式界面在
 * {@link TBetterStatsScreen} 中；本 Mixin 仅在统计数据就绪后切换界面。
 * </p>
 */
@Mixin(GuiStats.class)
public abstract class MixinGuiStats extends GuiScreen {

    // === Vanilla shadows ===

    @Shadow
    private GuiScreen field_146549_a;
    @Shadow
    private StatFileWriter field_146546_t;
    @Shadow
    private boolean doesGuiPauseGame;

    // ==================== Injections ====================

    /**
     * Intercept the stats-data-arrived callback and route to the configured layout.
     * <p>
     * VANILLA keeps the original screen; PANELED switches to
     * {@link TBetterStatsScreen}; TABBED switches to {@link GuiStatics}.
     * </p>
     * <p>
     * 拦截统计数据就绪回调并按配置的布局模式路由：
     * VANILLA 保持原版界面；PANELED 切换到 {@link TBetterStatsScreen}；
     * TABBED 切换到 {@link GuiStatics}。
     * </p>
     */
    @Inject(method = "func_146509_g", at = @At("HEAD"), cancellable = true)
    private void modernStatistic$onStatsReady(CallbackInfo ci) {
        // In multiplayer the stats may not be ready yet — let vanilla keep downloading
        // 多人模式下统计数据可能尚未就绪——交给原版继续下载
        if (!this.doesGuiPauseGame)
            return;

        // VANILLA mode: do nothing, let vanilla handle everything
        // VANILLA 模式：不做任何事，全部交给原版
        if (ModernStatistic.config.isVanillaMode())
            return;

        ci.cancel();

        // PANELED mode: switch to the BetterStats-style screen
        // PANELED 模式：切换到 BetterStats 风格界面
        if (ModernStatistic.config.isPaneledMode()) {
            this.mc.displayGuiScreen(
                    new TBetterStatsScreen(this.field_146549_a, this.field_146546_t));
            return;
        }

        // TABBED mode: switch to the tabbed screen (all tab logic lives there)
        // TABBED 模式：切换到标签页式界面（全部标签逻辑都在其中）
        this.mc.displayGuiScreen(
                new GuiStatics(this.field_146549_a, this.field_146546_t));
    }
}
