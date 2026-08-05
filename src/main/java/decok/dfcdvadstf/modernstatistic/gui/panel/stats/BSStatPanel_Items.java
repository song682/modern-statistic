package decok.dfcdvadstf.modernstatistic.gui.panel.stats;

import java.util.*;

import decok.dfcdvadstf.catframe.ui.layouts.GridLayout;
import decok.dfcdvadstf.catframe.ui.overlay.OverlayManager;
import decok.dfcdvadstf.modernstatistic.ItemStatsTracker;
import decok.dfcdvadstf.modernstatistic.ModernStatistic;
import decok.dfcdvadstf.modernstatistic.gui.TElement;
import decok.dfcdvadstf.modernstatistic.gui.overlay.ItemPopupOverlay;
import decok.dfcdvadstf.modernstatistic.gui.panel.BSPanel;
import decok.dfcdvadstf.modernstatistic.gui.screen.TBetterStatsScreen;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatCrafting;
import net.minecraft.stats.StatList;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Item statistics panel — grid layout grouped by creative tab.
 * <p>21x21 cells with item icon, tooltip on hover, wiki link on middle-click.</p>
 */
public class BSStatPanel_Items extends BSStatPanel {

    protected static final int CELL_SIZE = 21;
    protected static final RenderItem RENDER_ITEM = new RenderItem();
    protected static final Random RANDOM = new Random();

    protected final Map<Integer, ItemStatEntry> entries = new LinkedHashMap<>();

    public BSStatPanel_Items(BSPanel parentToFill, TBetterStatsScreen screen) {
        super(parentToFill, screen);
    }

    // ==================== Data Model ====================

    protected static class ItemStatEntry {
        final Item item;
        final int itemId;
        final boolean isBlock;
        int mined, crafted, used, broken, pickup, drop;

        ItemStatEntry(Item item, int itemId, boolean isBlock) {
            this.item = item;
            this.itemId = itemId;
            this.isBlock = isBlock;
        }

        boolean isEmpty() {
            return mined == 0 && crafted == 0 && used == 0 && broken == 0 && pickup == 0 && drop == 0;
        }
    }

    // ==================== Init ====================

    @Override
    public void init() {
        clearChildren();
        entries.clear();
        buildEntries();

        if (entries.isEmpty()) {
            showNoResults();
            return;
        }

        boolean showEmpty = screen.isShowEmptyStats();
        String search = screen.getSearchTerm().toLowerCase().trim();

        List<ItemStatEntry> filtered = new ArrayList<>();
        for (ItemStatEntry entry : entries.values()) {
            if (!showEmpty && entry.isEmpty()) continue;
            if (!search.isEmpty()) {
                String name = I18n.format(entry.item.getUnlocalizedName() + ".name").toLowerCase();
                if (!name.contains(search)) continue;
            }
            filtered.add(entry);
        }

        if (filtered.isEmpty()) {
            showNoResults();
            return;
        }

        // Use CatFrame GridLayout + RowHelper for auto-positioning instead of manual nextX/nextY
        // 用 CatFrame GridLayout + RowHelper 自动排列，替代手动 nextX/nextY 计算
        GridLayout grid = createContentGrid();
        int columns = Math.max(1, (getWidth() - getScrollPadding() * 2) / CELL_SIZE);
        GridLayout.RowHelper helper = createGridHelper(grid, columns, 1);

        for (ItemStatEntry entry : filtered) {
            ItemStatWidget widget = new ItemStatWidget(entry, 0, 0);
            helper.addChild(widget);  // GridLayout stores position metadata
            addPositionedWidget(widget);  // Add to TElement tree (positions fixed below)
        }

        // Actually calculate widget positions via GridLayout
        grid.recalculate();
        recalculateContentHeight();
    }

    protected void buildEntries() {
        Map<Integer, ItemStatEntry> map = new LinkedHashMap<>();

        // From objectMineStats (blocks)
        for (Object obj : StatList.objectMineStats) {
            StatCrafting sc = (StatCrafting) obj;
            Item item = sc.func_150959_a();
            int id = Item.getIdFromItem(item);
            ItemStatEntry entry = new ItemStatEntry(item, id, true);
            map.put(id, entry);
        }

        // From itemStats (items)
        for (Object obj : StatList.itemStats) {
            StatCrafting sc = (StatCrafting) obj;
            Item item = sc.func_150959_a();
            int id = Item.getIdFromItem(item);
            if (!map.containsKey(id)) {
                map.put(id, new ItemStatEntry(item, id, false));
            }
        }

        // Fill stat values
        for (ItemStatEntry entry : map.values()) {
            int id = entry.itemId;
            entry.mined = getStatValue(StatList.mineBlockStatArray, id);
            entry.crafted = getStatValue(StatList.objectCraftStats, id);
            entry.used = getStatValue(StatList.objectUseStats, id);
            entry.broken = getStatValue(StatList.objectBreakStats, id);
            entry.pickup = ItemStatsTracker.getPickupCount(id);
            entry.drop = ItemStatsTracker.getDropCount(id);
        }

        entries.putAll(map);
    }

    protected int getStatValue(StatBase[] array, int id) {
        if (array != null && id >= 0 && id < array.length && array[id] != null) {
            return screen.getStatFileWriter().writeStat(array[id]);
        }
        return 0;
    }

    // ==================== Widget ====================

    protected class ItemStatWidget extends TElement {

        protected final ItemStatEntry entry;

        public ItemStatWidget(ItemStatEntry entry, int x, int y) {
            super(x, y, CELL_SIZE, CELL_SIZE);
            this.entry = entry;
        }

        @Override
        protected void renderSelf(int mouseX, int mouseY, float partialTicks) {
            // Slot background
            fill(x, y, getEndX(), getEndY(), 0x80000000);
            drawOutline(x, y, getEndX(), getEndY(), 0xFF373737);

            // Item icon
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.enableGUIStandardItemLighting();
            ItemStack stack = new ItemStack(entry.item, 1, getDisplayDamage());
            RENDER_ITEM.renderItemIntoGUI(getFontRenderer(), getMC().getTextureManager(),
                    stack, x + 3, y + 3);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);

            // Count overlay
            if (entry.pickup > 0 || entry.drop > 0 || entry.mined > 0 || entry.crafted > 0) {
                String count = getStatDisplay();
                int cw = getFontRenderer().getStringWidth(count);
                drawString(getFontRenderer(), count, getEndX() - cw - 1, getEndY() - 10, 0xFFFFFF);
            }
        }

        @Override
        protected void postRenderSelf(int mouseX, int mouseY, float partialTicks) {
            if (hovered) {
                drawOutline(x, y, getEndX(), getEndY(), COLOR_NORMAL_HOVERED);
                drawTooltip(mouseX, mouseY);
            }
        }

        private void drawTooltip(int mouseX, int mouseY) {
            String name = I18n.format(entry.item.getUnlocalizedName() + ".name").trim();
            List<String> lines = new ArrayList<>();
            lines.add(name);
            if (entry.mined > 0) lines.add(I18n.format("stat_type.minecraft.mined") + ": " + entry.mined);
            if (entry.crafted > 0) lines.add(I18n.format("stat_type.minecraft.crafted") + ": " + entry.crafted);
            if (entry.used > 0) lines.add(I18n.format("stat_type.minecraft.used") + ": " + entry.used);
            if (entry.broken > 0) lines.add(I18n.format("stat_type.minecraft.broken") + ": " + entry.broken);
            if (entry.pickup > 0) lines.add(I18n.format("stat.pickup") + ": " + entry.pickup);
            if (entry.drop > 0) lines.add(I18n.format("stat.drop") + ": " + entry.drop);

            // Draw simple tooltip
            drawHoverTooltip(lines, mouseX, mouseY);
        }

        @Override
        protected boolean onMouseClicked(int mouseX, int mouseY, int button) {
            if (button == 2 && ModernStatistic.config.enableWikiLinks) {
                // Middle-click: open wiki
                String itemName = I18n.format(entry.item.getUnlocalizedName() + ".name").trim();
                openWikiUrl(itemName);
                return true;
            }
            if (button == 1 && ModernStatistic.config.enableWikiLinks) {
                // Right-click: show popup overlay
                ItemPopupOverlay popup = new ItemPopupOverlay(entry, mouseX, mouseY, screen);
                OverlayManager.INSTANCE.register(popup);
                return true;
            }
            return false;
        }

        private int getDisplayDamage() {
            if (entry.isBlock && entry.item instanceof ItemBlock) {
                Block block = ((ItemBlock) entry.item).field_150939_a;
                if (block == net.minecraft.init.Blocks.tallgrass) return 1;
            }
            return 0;
        }

        private String getStatDisplay() {
            int total = entry.pickup + entry.mined + entry.crafted + entry.used;
            if (total > 9999) return (total / 1000) + "k";
            return String.valueOf(total);
        }

        private void openWikiUrl(String itemName) {
            try {
                String encoded = java.net.URLEncoder.encode(itemName, "UTF-8").replace("+", "_");
                String url = ModernStatistic.config.itemWikiBaseUrl + encoded;
                screen.showWikiConfirm(url);
            } catch (java.io.UnsupportedEncodingException ignored) {}
        }
    }

    // ==================== Tooltip helper (static, shared) ====================

    protected static void drawHoverTooltip(List<String> lines, int x, int y) {
        if (lines.isEmpty()) return;
        int maxW = 0;
        for (String s : lines) {
            int w = getFontRenderer().getStringWidth(s);
            if (w > maxW) maxW = w;
        }
        int h = lines.size() * 10 - 2;
        int tx = x + 12;
        int ty = y - 12;
        int sw = getMC().currentScreen.width;
        int sh = getMC().currentScreen.height;
        if (tx + maxW > sw) tx -= 28 + maxW;
        if (ty + h + 6 > sh) ty = sh - h - 6;

        // Background
        net.minecraft.client.gui.Gui.drawRect(tx - 3, ty - 4, tx + maxW + 3, ty - 3, 0x505000FF);
        net.minecraft.client.gui.Gui.drawRect(tx - 3, ty + h + 3, tx + maxW + 3, ty + h + 4, 0x5028007F);
        net.minecraft.client.gui.Gui.drawRect(tx - 3, ty - 3, tx + maxW + 3, ty + h + 3, 0xF0100010);
        net.minecraft.client.gui.Gui.drawRect(tx - 4, ty - 3, tx - 3, ty + h + 3, 0x505000FF);
        net.minecraft.client.gui.Gui.drawRect(tx + maxW + 3, ty - 3, tx + maxW + 4, ty + h + 3, 0x5028007F);
        net.minecraft.client.gui.Gui.drawRect(tx - 3, ty - 3 + 1, tx - 3 + 1, ty + h + 3 - 1, 0x5028007F);
        net.minecraft.client.gui.Gui.drawRect(tx + maxW + 2, ty - 3 + 1, tx + maxW + 3, ty + h + 3 - 1, 0x5028007F);

        for (int i = 0; i < lines.size(); i++) {
            getFontRenderer().drawStringWithShadow(lines.get(i), tx, ty + i * 10, -1);
        }
    }
}
