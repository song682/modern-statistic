package decok.dfcdvadstf.modernstatistic.gui.betterstats.overlay;

import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;
import decok.dfcdvadstf.catframe.ui.overlay.Overlay;
import decok.dfcdvadstf.catframe.ui.overlay.OverlayManager;
import decok.dfcdvadstf.catframe.ui.overlay.ScreenAnchor;
import decok.dfcdvadstf.modernstatistic.ModernStatistic;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.TElement;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.WikiLinkHandler;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;

import java.util.ArrayList;
import java.util.List;

/**
 * Right-click popup overlay for item stat entries.
 * <p>右键弹出菜单：显示 "View on Wiki" 等操作选项。</p>
 */
public class ItemPopupOverlay extends AbstractComponent implements Overlay {

    private static final int LINE_HEIGHT = 14;
    private static final int PADDING = 4;
    /** Upper bound for the auto-sized popup width / 自适应弹窗宽度上限 */
    private static final int MAX_WIDTH = 200;

    private final List<PopupAction> actions;
    private final WikiLinkHandler wikiHandler;

    /** Cached popup origin (screen coordinates). */
    private final int originX;
    private final int originY;

    /**
     * Create a popup overlay at the given screen position.
     *
     * @param entry         the item stat entry this popup relates to
     * @param mouseX        screen X coordinate of the click
     * @param mouseY        screen Y coordinate of the click
     * @param screenWidth   the host screen width / 宿主界面宽度
     * @param screenHeight  the host screen height / 宿主界面高度
     * @param wikiHandler   the host screen's wiki confirmation flow; may be
     *                      {@code null} when {@code showWikiLink} is {@code false} /
     *                      宿主界面的 Wiki 确认流程；{@code showWikiLink} 为 {@code false}
     *                      时可传 {@code null}
     * @param showWikiLink  whether to offer the "View on Wiki" action — the
     *                      PANELED mode only feature; the TABBED mode popup
     *                      hides it / 是否提供“查看 Wiki”操作——该功能仅属于
     *                      PANELED 模式；TABBED 模式的弹窗不显示它
     */
    public ItemPopupOverlay(
            final Object entry,
            final int mouseX,
            final int mouseY,
            final int screenWidth,
            final int screenHeight,
            final WikiLinkHandler wikiHandler,
            final boolean showWikiLink
    ) {
        // Use type-unsafe access to avoid coupling to inner class type at compile time
        final String itemName = resolveItemName(entry);
        final String statId = resolveStatId(entry);
        this.wikiHandler = wikiHandler;

        this.actions = new ArrayList<>();
        // "View on Wiki" is a PANELED-mode-only action (BetterStats heritage);
        // the TABBED mode popup only offers pin/unpin.
        // “查看 Wiki”是 PANELED 模式专属操作（BetterStats 传承）；TABBED 模式的
        // 弹窗只提供固定/取消固定。
        if (showWikiLink) {
            this.actions.add(new PopupAction(
                    I18n.format("betterstats.gui.view_on_wiki"),
                    () -> openWiki(itemName),
                    true
            ));
        }

        // Pin to HUD — enabled only when a valid stat ID could be resolved, and
        // the label flips to "Unpin from HUD" when this stat is already pinned.
        // 固定到 HUD —— 仅在能解析出有效统计 ID 时可用；已固定时标签切换为“取消固定”。
        boolean alreadyPinned = statId != null
                && ModernStatistic.config.getPinnedStatIds().contains(statId);
        this.actions.add(new PopupAction(
                I18n.format(alreadyPinned ? "betterstats.gui.unpin_from_hud" : "betterstats.gui.pin_to_hud"),
                () -> togglePin(statId, alreadyPinned),
                statId != null
        ));

        int popupHeight = PADDING * 2 + actions.size() * LINE_HEIGHT;
        int popupWidth = computePopupWidth();

        // Smart positioning: expand toward the side with more room, then clamp
        // to the screen bounds on all four edges.
        // 智能定位：向空间更大的一侧展开，再向四周边界钳制。
        int popupX = (mouseX + popupWidth > screenWidth) ? mouseX - popupWidth : mouseX;
        popupX = Math.max(4, Math.min(popupX, screenWidth - popupWidth - 4));
        int popupY = (mouseY + popupHeight > screenHeight) ? mouseY - popupHeight : mouseY;
        popupY = Math.max(4, Math.min(popupY, screenHeight - popupHeight - 4));

        this.originX = popupX;
        this.originY = popupY;

        setSize(popupWidth, popupHeight);
        setVisible(true);
    }

    // ──── Overlay ────

    @Override
    public ScreenAnchor getAnchor() {
        return ScreenAnchor.TOP_LEFT;
    }

    @Override
    public int getOffsetX() {
        return originX;
    }

    @Override
    public int getOffsetY() {
        return originY;
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    /**
     * Override isMouseOver to capture ALL clicks while this popup is visible,
     * so clicking outside also closes it.
     * <p>覆盖 isMouseOver 以捕获所有点击，点击弹窗外部也会关闭。</p>
     */
    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return true; // catch all clicks when blocking
    }

    // ──── Render ────

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        // Background
        TElement.fill(originX, originY, originX + width, originY + height, 0xE0000000);
        TElement.drawOutline(originX, originY, originX + width, originY + height, 0xFFFFFFFF);

        // Action items
        for (int i = 0; i < actions.size(); i++) {
            PopupAction action = actions.get(i);
            int ay = originY + PADDING + i * LINE_HEIGHT;
            int color = action.enabled ? 0xFFFFFF : 0x808080;

            // Hover highlight
            if (action.enabled && mouseX >= originX && mouseX < originX + width
                    && mouseY >= ay && mouseY < ay + LINE_HEIGHT) {
                TElement.fill(originX, ay, originX + width, ay + LINE_HEIGHT, 0x40FFFFFF);
            }

            TElement.drawString(
                    TElement.getFontRenderer(),
                    action.label,
                    originX + 6,
                    ay + (LINE_HEIGHT - 9) / 2,
                    color
            );
        }
    }

    // ──── Input ────

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!visible) return;

        // Check if click is on an action item
        if (mouseButton == 0 && mouseX >= originX && mouseX < originX + width) {
            int relativeY = mouseY - (originY + PADDING);
            int index = relativeY / LINE_HEIGHT;
            if (index >= 0 && index < actions.size()) {
                PopupAction action = actions.get(index);
                if (action.enabled && action.runnable != null) {
                    action.runnable.run();
                }
            }
        }

        // Close popup on any click
        close();
    }

    // ──── Internal ────

    private void close() {
        setVisible(false);
        OverlayManager.INSTANCE.unregister(this);
    }

    /**
     * Compute the popup width from the widest action label.
     * <p>根据最宽操作标签计算弹窗宽度。</p>
     */
    private int computePopupWidth() {
        FontRenderer fontRenderer = TElement.getFontRenderer();
        int maxWidth = 0;
        for (PopupAction action : actions) {
            maxWidth = Math.max(maxWidth, fontRenderer.getStringWidth(action.label));
        }
        return Math.min(maxWidth + PADDING * 2 + 12, MAX_WIDTH);
    }

    /**
     * Pin or unpin the resolved stat on the HUD overlay.
     * <p>在 HUD 叠加层上固定或取消固定已解析的统计。</p>
     */
    private void togglePin(String statId, boolean currentlyPinned) {
        if (statId == null) {
            return;
        }
        if (currentlyPinned) {
            ModernStatistic.config.removePinnedStat(statId);
        } else {
            ModernStatistic.config.addPinnedStat(statId);
            // Make sure the HUD overlay shows the newly pinned stat
            // 确保 HUD 叠加层显示新固定的统计
            StatsHudOverlay.INSTANCE.setVisible(true);
        }
    }

    private void openWiki(String itemName) {
        if (wikiHandler == null) {
            return;
        }
        try {
            String encoded = java.net.URLEncoder.encode(itemName, "UTF-8").replace("+", "_");
            String url = ModernStatistic.config.itemWikiBaseUrl + encoded;
            wikiHandler.showWikiConfirm(url);
        } catch (java.io.UnsupportedEncodingException ignored) {
        }
    }

    /**
     * Resolve the item's stat ID from an ItemStatEntry via reflection-like access
     * (since ItemStatEntry is a protected inner class of BSStatPanel_Items).
     * Picks the first available stat dimension: mined for blocks, then crafted,
     * then used.
     * <p>从 ItemStatEntry 中解析物品的统计 ID（通过反射方式访问内部类字段）。
     * 依次选取可用的统计维度：方块优先采集量，然后是合成量、使用量。</p>
     */
    private static String resolveStatId(Object entry) {
        try {
            java.lang.reflect.Field itemField = entry.getClass().getDeclaredField("item");
            itemField.setAccessible(true);
            Item item = (Item) itemField.get(entry);
            int id = Item.getIdFromItem(item);
            if (id >= 0 && id < StatList.mineBlockStatArray.length && StatList.mineBlockStatArray[id] != null) {
                return ((StatBase) StatList.mineBlockStatArray[id]).statId;
            }
            if (id >= 0 && id < StatList.objectCraftStats.length && StatList.objectCraftStats[id] != null) {
                return StatList.objectCraftStats[id].statId;
            }
            if (id >= 0 && id < StatList.objectUseStats.length && StatList.objectUseStats[id] != null) {
                return StatList.objectUseStats[id].statId;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Resolve the item's display name from an ItemStatEntry via reflection-like access
     * (since ItemStatEntry is a protected inner class of BSStatPanel_Items).
     * <p>从 ItemStatEntry 中解析物品显示名称（通过反射方式访问内部类字段）。</p>
     */
    private static String resolveItemName(Object entry) {
        try {
            // ItemStatEntry has: Item item; field
            java.lang.reflect.Field itemField = entry.getClass().getDeclaredField("item");
            itemField.setAccessible(true);
            net.minecraft.item.Item item = (net.minecraft.item.Item) itemField.get(entry);
            return I18n.format(item.getUnlocalizedName() + ".name").trim();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // ──── Action model ────

    private static class PopupAction {
        final String label;
        final Runnable runnable;
        final boolean enabled;

        PopupAction(String label, Runnable runnable, boolean enabled) {
            this.label = label;
            this.runnable = runnable;
            this.enabled = enabled;
        }
    }
}
