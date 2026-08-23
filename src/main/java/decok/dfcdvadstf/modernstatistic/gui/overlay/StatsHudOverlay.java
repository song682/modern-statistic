package decok.dfcdvadstf.modernstatistic.gui.overlay;

import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;
import decok.dfcdvadstf.catframe.ui.overlay.Overlay;
import decok.dfcdvadstf.catframe.ui.overlay.OverlayContext;
import decok.dfcdvadstf.catframe.ui.overlay.ScreenAnchor;
import decok.dfcdvadstf.modernstatistic.ModernStatistic;
import decok.dfcdvadstf.modernstatistic.gui.TElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatCrafting;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.stats.StatList;

import java.util.List;

/**
 * HUD statistics overlay — displays pinned stats on the in-game HUD.
 * <p>
 * HUD 统计叠加层——在游戏 HUD 上显示已固定的统计项。
 * </p>
 * <p>
 * Driven by CatFrame's {@code ClientOverlayHandler}: the {@code HUD} context is
 * rendered automatically on {@code RenderGameOverlayEvent.Post} via
 * {@code OverlayManager.renderHud}. This class only manages which stats are
 * shown (from {@code ModernStatisticConfig.pinnedStats}) and how they are drawn.
 * </p>
 * <p>
 * 由 CatFrame 的 {@code ClientOverlayHandler} 驱动：{@code HUD} 上下文会在
 * {@code RenderGameOverlayEvent.Post} 时经 {@code OverlayManager.renderHud}
 * 自动渲染。本类只负责显示哪些统计（来自 {@code ModernStatisticConfig.pinnedStats}）
 * 以及如何绘制。
 * </p>
 * <p>
 * Note: {@link OverlayManager} resolves the anchor position from
 * {@link #getWidth()} / {@link #getHeight()} <em>before</em> calling
 * {@link #extractRenderState}, so the size must be computed dynamically instead
 * of being set inside the render pass.
 * </p>
 * <p>
 * 注意：{@link OverlayManager} 在调用 {@link #extractRenderState} <em>之前</em>就通过
 * {@link #getWidth()} / {@link #getHeight()} 解析锚点位置，因此尺寸必须动态计算，
 * 而不能在渲染阶段内设置。
 * </p>
 */
public class StatsHudOverlay extends AbstractComponent implements Overlay {

    /** Singleton instance / 单例实例 */
    public static final StatsHudOverlay INSTANCE = new StatsHudOverlay();

    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int MAX_WIDTH = 200;

    /** Overlay visibility toggle (managed by /hudstats toggle) / 叠加层可见性开关（由 /hudstats toggle 管理） */
    private boolean overlayEnabled = true;

    private StatsHudOverlay() {
        super(0, 0, 0, 0);
    }

    // ──── Visibility control ────

    /** Toggle HUD overlay visibility / 切换 HUD 叠加层可见性 */
    public void toggle() {
        overlayEnabled = !overlayEnabled;
    }

    /** @return whether the overlay visibility toggle is on / 叠加层可见性开关是否开启 */
    public boolean isOverlayEnabled() {
        return overlayEnabled;
    }

    // ──── Overlay contract ────

    @Override
    public OverlayContext getContext() {
        return OverlayContext.HUD;
    }

    @Override
    public ScreenAnchor getAnchor() {
        return ScreenAnchor.TOP_LEFT;
    }

    @Override
    public int getOffsetX() {
        return 4;
    }

    @Override
    public int getOffsetY() {
        return 4;
    }

    @Override
    public boolean isBlocking() {
        return false;
    }

    /**
     * Visible only when the toggle, the config switch and the game world are all
     * available. Pinning/unpinning is only allowed while the HUD is visible.
     * <p>仅当开关、配置项与游戏世界都可用时可见。只有 HUD 可见时才允许固定/取消固定。</p>
     */
    @Override
    public boolean isVisible() {
        return super.isVisible() && overlayEnabled && ModernStatistic.config.enableHudOverlay;
    }

    // ──── Dynamic size (resolved by OverlayManager before rendering) ────

    @Override
    public int getWidth() {
        return Math.min(computeContentWidth(), MAX_WIDTH);
    }

    @Override
    public int getHeight() {
        int lines = countResolvableStats();
        return lines == 0 ? 0 : PADDING * 2 + lines * LINE_HEIGHT;
    }

    // ──── Render ────

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.getStatFileWriter() == null) {
            return;
        }
        if (!isVisible()) {
            return;
        }

        List<String> ids = ModernStatistic.config.getPinnedStatIds();
        int lines = countResolvableStats();
        if (lines == 0) {
            return;
        }

        StatFileWriter statFileWriter = mc.thePlayer.getStatFileWriter();
        int w = getWidth();
        int h = getHeight();

        // Background panel / 背景面板
        TElement.fill(x, y, x + w, y + h, 0x90000000);
        TElement.drawOutline(x, y, x + w, y + h, 0xFF808080);

        // Stat lines / 统计行
        int lineY = y + PADDING;
        for (String id : ids) {
            StatBase stat = StatList.func_151177_a(id);
            if (stat == null) {
                continue;
            }
            String label = getLabel(stat, statFileWriter);
            FontRenderer fr = TElement.getFontRenderer();
            TElement.drawString(fr, label, x + PADDING, lineY + (LINE_HEIGHT - 9) / 2, 0xFFFFFF);
            lineY += LINE_HEIGHT;
        }
    }

    // ──── Internal ────

    /**
     * Count pinned stats that resolve to a real {@link StatBase}.
     * <p>统计可解析为真实 {@link StatBase} 的固定统计数量。</p>
     */
    private int countResolvableStats() {
        int count = 0;
        for (String id : ModernStatistic.config.getPinnedStatIds()) {
            if (StatList.func_151177_a(id) != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Compute the widest label width among resolvable stats.
     * <p>计算可解析统计中最宽标签的宽度。</p>
     */
    private int computeContentWidth() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.getStatFileWriter() == null) {
            return 0;
        }
        StatFileWriter statFileWriter = mc.thePlayer.getStatFileWriter();
        FontRenderer fr = mc.fontRenderer;
        int maxWidth = 0;
        for (String id : ModernStatistic.config.getPinnedStatIds()) {
            StatBase stat = StatList.func_151177_a(id);
            if (stat == null) {
                continue;
            }
            maxWidth = Math.max(maxWidth, fr.getStringWidth(getLabel(stat, statFileWriter)));
        }
        return maxWidth + PADDING * 2 + 6;
    }

    /**
     * Build the display label: for item/block stats the value is embedded into
     * the action template ("Mined 12 Stone" / "挖掘了 12 个石头") so the name
     * does not read like a duplicated action sentence followed by a bare
     * number; all other stats keep the plain "name: value" form.
     * <p>
     * 构建显示标签：物品/方块统计把数值嵌入动作模板（"挖掘了 12 个石头"），
     * 避免“名称本身已是动作句 + 冒号 + 裸数字”的语义重复；其余统计保留
     * “名称: 数值”形式。
     * </p>
     */
    private String getLabel(StatBase stat, StatFileWriter statFileWriter) {
        int value = statFileWriter.writeStat(stat);
        if (stat instanceof StatCrafting) {
            String actionKey = getCraftingActionKey(stat.statId);
            if (actionKey != null) {
                String itemName = I18n.format(
                        ((StatCrafting) stat).func_150959_a().getUnlocalizedName() + ".name").trim();
                return I18n.format(actionKey, value, itemName);
            }
        }
        return stat.func_150951_e().getUnformattedText() + ": " + value;
    }

    /**
     * Map an item/block stat ID prefix to its HUD label template key. The
     * template receives (value, itemName) as format args.
     * <p>
     * 将物品/方块统计 ID 前缀映射为 HUD 标签模板键，模板以（数值, 物品名）
     * 作为格式化参数。
     * </p>
     */
    private static String getCraftingActionKey(String statId) {
        if (statId.startsWith("stat.mineBlock.")) return "hudstats.stat.mineBlock";
        if (statId.startsWith("stat.craftItem.")) return "hudstats.stat.craftItem";
        if (statId.startsWith("stat.useItem.")) return "hudstats.stat.useItem";
        if (statId.startsWith("stat.breakItem.")) return "hudstats.stat.breakItem";
        return null;
    }
}
