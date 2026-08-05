package decok.dfcdvadstf.modernstatistic.gui.overlay;

import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;
import decok.dfcdvadstf.catframe.ui.overlay.Overlay;
import decok.dfcdvadstf.catframe.ui.overlay.OverlayManager;
import decok.dfcdvadstf.catframe.ui.overlay.ScreenAnchor;
import decok.dfcdvadstf.modernstatistic.ModernStatistic;
import decok.dfcdvadstf.modernstatistic.gui.TElement;
import decok.dfcdvadstf.modernstatistic.gui.screen.TBetterStatsScreen;

import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.List;

/**
 * Right-click popup overlay for item stat entries.
 * <p>右键弹出菜单：显示 "View on Wiki" 等操作选项。</p>
 */
public class ItemPopupOverlay extends AbstractComponent implements Overlay {

    private static final int POPUP_WIDTH = 130;
    private static final int LINE_HEIGHT = 14;
    private static final int PADDING = 4;

    private final List<PopupAction> actions;
    private final TBetterStatsScreen screen;

    /** Cached popup origin (screen coordinates). */
    private final int originX;
    private final int originY;

    /**
     * Create a popup overlay at the given screen position.
     *
     * @param entry   the item stat entry this popup relates to
     * @param mouseX  screen X coordinate of the click
     * @param mouseY  screen Y coordinate of the click
     * @param screen  the parent stats screen (for wiki confirmation)
     */
    public ItemPopupOverlay(
            final Object entry,
            final int mouseX,
            final int mouseY,
            final TBetterStatsScreen screen
    ) {
        // Use type-unsafe access to avoid coupling to inner class type at compile time
        final String itemName = resolveItemName(entry);
        this.screen = screen;

        this.actions = new ArrayList<>();
        this.actions.add(new PopupAction(
                I18n.format("betterstats.gui.view_on_wiki"),
                () -> openWiki(itemName),
                true
        ));
        this.actions.add(new PopupAction(
                I18n.format("betterstats.gui.pin_to_hud"),
                null,
                false
        ));

        int popupHeight = PADDING * 2 + actions.size() * LINE_HEIGHT;
        // Clamp position so popup stays on screen
        this.originX = Math.min(mouseX, screen.width - POPUP_WIDTH - 4);
        this.originY = Math.min(mouseY, screen.height - popupHeight - 4);

        setSize(POPUP_WIDTH, popupHeight);
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
    public void render(int mouseX, int mouseY, float partialTicks) {
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

    private void openWiki(String itemName) {
        try {
            String encoded = java.net.URLEncoder.encode(itemName, "UTF-8").replace("+", "_");
            String url = ModernStatistic.config.itemWikiBaseUrl + encoded;
            screen.showWikiConfirm(url);
        } catch (java.io.UnsupportedEncodingException ignored) {
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
