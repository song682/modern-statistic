package decok.dfcdvadstf.modernstatistic.gui.tabbed.tab;

import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.components.ContainerObjectSelectionList;
import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;
import decok.dfcdvadstf.catframe.ui.components.tab.AbstractScreenTab;
import decok.dfcdvadstf.catframe.ui.navigation.ScreenRectangle;
import decok.dfcdvadstf.catframe.ui.overlay.OverlayManager;
import decok.dfcdvadstf.modernstatistic.gui.tabbed.list.ModernSelectionList;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.overlay.ItemPopupOverlay;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatCrafting;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.stats.StatList;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Merged tab — combines the vanilla "Blocks" and "Items" stat lists into one
 * view.
 * <p>
 * Columns: Mined | Crafted | Used | Broken | Pickup | Drop.
 * </p>
 * <p>
 * Mined shows block-mining stats (blocks only);
 * Broken shows tool-depletion stats (damageable items only);
 * Crafted / Used apply to both blocks and items.
 * Pickup / Drop show item pickup and drop counts from event tracking.
 * Rows that lack a given stat simply display "-".
 * </p>
 */
public class StatsItemsTab extends AbstractScreenTab {

    private StatFileWriter statFileWriter;
    private static final ResourceLocation STAT_ICONS = new ResourceLocation("textures/gui/container/stats_icons.png");
    private static final ResourceLocation MODERN_STAT_ICONS = new ResourceLocation("modernstatistic",
            "textures/gui/stats_modern_icon.png");
    private static final RenderItem RENDER_ITEM = new RenderItem();

    // Column icon UVs matching vanilla stats_icons.png (128x128, f=0.0078125)
    // Row v=18: CRAFT(u=18) | USED(u=36) | MINED(u=54) | BROKE(u=72)
    // Col 0 (ITEM_MINED): u=54,v=18 — StatsBlock col 0
    // Col 1 (ITEM_BROKEN): u=72,v=18 — StatsItem col 0
    // Col 2 (ITEM_CRAFTED):u=18,v=18 — StatsItem col 1
    // Col 3 (ITEM_USED): u=36,v=18 — StatsItem col 2
    // Col 4-5: stats_modern_icon.png (PICKUP u=0, DROP u=18)
    private static final int[] COLUMN_ICON_U = { 54, 72, 18, 36, 0, 18 };
    private static final int[] COLUMN_ICON_V = { 18, 18, 18, 18, 0, 18 };
    private static final boolean[] COLUMN_USE_MODERN = { false, false, false, false, true, true };

    private MergedSelectionList list;

    /** Host screen dimensions for popup clamping / 宿主界面尺寸（弹窗钳制用） */
    private int screenWidth;
    private int screenHeight;

    public StatsItemsTab() {
        super(106, "stat.itemsButton");
    }

    /**
     * Initialise the tab with the host screen: keeps the screen size for the
     * item right-click popup overlay ({@link ItemPopupOverlay}) edge clamping.
     * The popup only offers pin/unpin in TABBED mode — the "View on Wiki"
     * action is PANELED-mode-only, so no {@code WikiLinkHandler} is needed
     * here.
     * <p>
     * 用宿主界面初始化标签页：保存界面尺寸用于物品右键弹出菜单
     * （{@link ItemPopupOverlay}）的边缘钳制。TABBED 模式的弹窗只提供
     * 固定/取消固定——“查看 Wiki”操作仅属 PANELED 模式，因此这里不需要
     * {@code WikiLinkHandler}。
     * </p>
     */
    public void initGui(int width, int height, List<GuiButton> buttonList,
            StatFileWriter writer) {
        this.statFileWriter = writer;
        this.screenWidth = width;
        this.screenHeight = height;
        this.list = new MergedSelectionList(width, height);
        setVisible(false);
    }

    /**
     * The list component, registered as a render-only component by
     * {@code GuiStatics.initTabs}; input events (clicks, scroll wheel, drags)
     * are forwarded to the visible list explicitly by {@code GuiStatics} —
     * this is what makes the column-header sort buttons clickable.
     * <p>
     * 列表组件，由 {@code GuiStatics.initTabs} 注册为仅渲染组件；
     * 输入事件（点击 / 滚轮 / 拖动）由 {@code GuiStatics} 显式转发给可见列表——
     * 列头排序按钮正是靠这一步才能被点击。
     * </p>
     */
    public GuiEventListener getList() {
        return list;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // The list is rendered by the screen's renderable pipeline (addRenderableOnly)
        // 列表由界面的渲染管线渲染（addRenderableOnly）
    }

    @Override
    public void actionPerformed(GuiButton button) {
        // Column header clicks are handled inside the list
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // Mouse events are forwarded to the visible list explicitly by GuiStatics
        // 鼠标事件由 GuiStatics 显式转发给可见列表
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
    }

    @Override
    public void setVisible(boolean visible) {
        // Keep the list visibility in sync (TabManager calls this on every switch)
        // 同步列表可见性（TabManager 每次切换都会调用本方法）
        super.setVisible(visible);
        if (list != null) {
            list.setVisible(visible);
        }
    }

    @Override
    public void doLayout(ScreenRectangle rectangle) {
        // Fill the content zone (between the header/footer separators) with the
        // list; the column header is itself the first scrollable entry
        // (HeaderEntry, vanilla style), so no fixed band is reserved here.
        // 用列表填充内容区（Header/Footer 分隔线之间）；列表头本身就是第一个
        // 可滚动条目（HeaderEntry，原版做法），因此这里无需预留固定表头带。
        if (list != null) {
            list.layoutInContentZone(rectangle.left(), rectangle.top(),
                    rectangle.width, rectangle.bottom(), 0);
        }
    }

    public boolean isEmpty() {
        // Only the header entry present => no data rows
        // 只剩下表头条目 => 没有数据行
        return list == null || !list.hasData();
    }

    // ==================== Inner selection list ====================

    private class MergedSelectionList
            extends ModernSelectionList<MergedSelectionList.BaseEntry> {

        /** Header height in pixels / 列表头高度（像素） */
        static final int HEADER_HEIGHT = 20;

        // Column order matching high-version StatsScreen:
        // 0=BLOCK_MINED, 1=ITEM_BROKEN, 2=ITEM_CRAFTED, 3=ITEM_USED, 4=ITEM_PICKED_UP,
        // 5=ITEM_DROPPED
        private int sortColumn = -1;
        private int sortDirection = -1; // 1=ascending, -1=descending
        /** Column-header entry, always the first entry, like vanilla / 列表头条目，与原版一样恒为首个条目 */
        private final HeaderEntry headerEntry;

        MergedSelectionList(int width, int height) {
            super(width, height, 22, 20);
            // The header is the first scrollable entry exactly like the
            // high-version StatsScreen; HEADER_HEIGHT equals the default entry
            // height, so replaceEntries re-adds it without special handling.
            // 表头是高版本 StatsScreen 那样的第一个可滚动条目；HEADER_HEIGHT
            // 与默认条目高度一致，replaceEntries 重建时无需特殊处理。
            headerEntry = new HeaderEntry();
            addEntry(headerEntry, HEADER_HEIGHT);
            buildEntries();
            sortById();
        }
        // ---- Entry building ----

        private void buildEntries() {
            Map<Integer, MergedEntry> byId = new HashMap<>();

            // Blocks: from objectMineStats
            for (Object obj : StatList.objectMineStats) {
                StatCrafting sc = (StatCrafting) obj;
                int id = Item.getIdFromItem(sc.func_150959_a());
                boolean hasStats = statFileWriter.writeStat(sc) > 0;
                if (!hasStats && StatList.objectUseStats[id] != null) {
                    hasStats = statFileWriter.writeStat(StatList.objectUseStats[id]) > 0;
                }
                if (!hasStats && StatList.objectCraftStats[id] != null) {
                    hasStats = statFileWriter.writeStat(StatList.objectCraftStats[id]) > 0;
                }
                if (hasStats) {
                    byId.put(id, new MergedEntry(sc.func_150959_a(), id, true,
                            getDisplayDamage(sc.func_150959_a(), true)));
                }
            }

            // Items: from itemStats (add only if not already present as block)
            for (Object obj : StatList.itemStats) {
                StatCrafting sc = (StatCrafting) obj;
                int id = Item.getIdFromItem(sc.func_150959_a());
                if (byId.containsKey(id))
                    continue;

                boolean hasStats = statFileWriter.writeStat(sc) > 0;
                if (!hasStats && StatList.objectUseStats[id] != null) {
                    hasStats = statFileWriter.writeStat(StatList.objectUseStats[id]) > 0;
                }
                if (!hasStats && StatList.objectBreakStats[id] != null) {
                    hasStats = statFileWriter.writeStat(StatList.objectBreakStats[id]) > 0;
                }
                if (!hasStats && StatList.objectCraftStats[id] != null) {
                    hasStats = statFileWriter.writeStat(StatList.objectCraftStats[id]) > 0;
                }
                if (hasStats) {
                    byId.put(id, new MergedEntry(sc.func_150959_a(), id, false));
                }
            }

            // Fallback: catch items not in objectMineStats or itemStats
            // (e.g. items with subtypes like spawn eggs, or items where only
            // objectUseStats is recorded like throwable/placeable items)
            for (Object obj : Item.itemRegistry) {
                Item item = (Item) obj;
                if (item == null)
                    continue;
                int id = Item.getIdFromItem(item);
                if (byId.containsKey(id))
                    continue;

                boolean hasStats = false;
                if (StatList.objectCraftStats[id] != null
                        && statFileWriter.writeStat(StatList.objectCraftStats[id]) > 0) {
                    hasStats = true;
                }
                if (!hasStats && StatList.objectUseStats[id] != null
                        && statFileWriter.writeStat(StatList.objectUseStats[id]) > 0) {
                    hasStats = true;
                }
                if (!hasStats && StatList.objectBreakStats[id] != null
                        && statFileWriter.writeStat(StatList.objectBreakStats[id]) > 0) {
                    hasStats = true;
                }
                if (hasStats) {
                    byId.put(id, new MergedEntry(item, id, false));
                }
            }

            for (MergedEntry entry : byId.values()) {
                addEntry(entry);
            }
        }

        private void sortById() {
            sortRows(new Comparator<MergedEntry>() {
                @Override
                public int compare(MergedEntry a, MergedEntry b) {
                    return Integer.compare(a.itemId, b.itemId);
                }
            });
        }

        /**
         * Sort the data rows while the header entry stays pinned on top — the
         * counterpart of vanilla's {@code clearEntriesExcept(getFirst())} + re-add.
         * <p>
         * 只排序数据行，表头条目恒定在最上方——对应原版的
         * {@code clearEntriesExcept(getFirst())} + 重新添加。
         * </p>
         */
        private void sortRows(Comparator<MergedEntry> dataSorter) {
            List<BaseEntry> rows = new ArrayList<BaseEntry>();
            for (BaseEntry entry : children()) {
                if (entry != headerEntry) {
                    rows.add(entry);
                }
            }
            Collections.sort(rows, new Comparator<BaseEntry>() {
                @Override
                public int compare(BaseEntry a, BaseEntry b) {
                    // Only data rows are collected above / 上面只会收集数据行
                    return dataSorter.compare((MergedEntry) a, (MergedEntry) b);
                }
            });
            rows.add(0, headerEntry);
            replaceEntries(rows);
        }

        /** @return true if at least one data row exists / 是否存在至少一个数据条目 */
        boolean hasData() {
            return getItemCount() > 1;
        }

        // ---- Geometry ----

        @Override
        public int getRowWidth() {
            // Clickable area: from contentX (0 relative) to rightmost column center + 18
            // (icon half). Must be >= getColumnX(5)=275 for column 5 clicks to register
            return getColumnX(5) + 18 + 4;
        }

        @Override
        public int getRowLeft() {
            // Preserve the vanilla formula (width - listWidth) / 2 exactly
            // 保持原版公式 (width - listWidth) / 2 精确一致
            return getX() + (width - getRowWidth()) / 2;
        }

        // ---- Background ----

        // ---- Header ----

        /** X center of column col (0..5), matching high-version formula: 75 + 40*col */
        private int getColumnX(int col) {
            return 75 + 40 * col;
        }

        // ---- Header interaction ----

        @Override
        protected void renderWidget(GuiGraphicsExtractor graphics, int mouseX,
                int mouseY, float partialTicks) {
            super.renderWidget(graphics, mouseX, mouseY, partialTicks);
            drawTooltip(mouseX, mouseY);
        }

        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            // A click on the scrolling header entry sorts the column; anything
            // else falls through to the scrollbar/list handling
            // 点击随列表滚动的表头条目则按列排序；其余交给滚动条 / 列表处理
            if (mouseButton == 0 && headerEntry.isMouseOver(mouseX, mouseY)) {
                int col = getHeaderColumnAt(mouseX, mouseY);
                if (col >= 0) {
                    sortByColumn(col);
                    mc.getSoundHandler().playSound(
                            PositionedSoundRecord.func_147674_a(
                                    new ResourceLocation("gui.button.press"), 1.0F));
                    return;
                }
            }

            // Right-click on a data row → item popup overlay, mirroring the
            // PANELED mode's BSStatPanel_Items.ItemStatWidget.onMouseClicked.
            // TABBED mode only offers pin/unpin (no wiki handler, no wiki
            // action) — that feature belongs to the PANELED screen.
            // 右键点击数据行 → 物品右键弹出菜单，与 PANELED 模式的
            // BSStatPanel_Items.ItemStatWidget.onMouseClicked 对应。
            // TABBED 模式只提供固定/取消固定（无 wiki handler、无 wiki 操作）——
            // 该功能属于 PANELED 界面。
            if (mouseButton == 1 && isMouseOver(mouseX, mouseY)) {
                BaseEntry hovered = getEntryAtPosition(mouseX, mouseY);
                if (hovered instanceof MergedEntry) {
                    ItemPopupOverlay popup = new ItemPopupOverlay(hovered, mouseX,
                            mouseY, screenWidth, screenHeight, null, false);
                    OverlayManager.INSTANCE.register(popup);
                    return;
                }
            }
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }

        /**
         * @return column (0..5) under the mouse while it is on the header entry,
         * otherwise -1 / 鼠标位于表头条目上时返回所在列号（0..5），否则 -1
         */
        private int getHeaderColumnAt(int mouseX, int mouseY) {
            if (mouseY < headerEntry.getY()
                    || mouseY >= headerEntry.getY() + headerEntry.getHeight()) {
                return -1;
            }
            int rowLeft = getRowLeft();
            for (int col = 0; col < 6; col++) {
                int colLeft = rowLeft + getColumnX(col) - 18;
                int colRight = rowLeft + getColumnX(col);
                if (mouseX >= colLeft && mouseX < colRight) {
                    return col;
                }
            }
            return -1;
        }

        private void sortByColumn(int column) {
            if (sortColumn == -1) {
                sortColumn = column;
                sortDirection = -1; // default descending
            } else if (sortColumn == column && sortDirection == -1) {
                sortDirection = 1; // flip to ascending
            } else if (sortColumn == column && sortDirection == 1) {
                sortColumn = -1; // reset to default sort
                sortDirection = 0;
            } else {
                sortColumn = column;
                sortDirection = -1; // new column, default descending
            }

            if (sortColumn == -1) {
                sortById();
                return;
            }

            final int col = sortColumn;
            final int dir = sortDirection;
            sortRows(new Comparator<MergedEntry>() {
                @Override
                public int compare(MergedEntry a, MergedEntry b) {
                    int va = a.getStatValue(col, statFileWriter);
                    int vb = b.getStatValue(col, statFileWriter);

                    if (va != vb) {
                        return (va - vb) * dir;
                    }
                    return Integer.compare(a.itemId, b.itemId);
                }
            });
        }

        // ---- Tooltip ----

        private void drawTooltip(int mouseX, int mouseY) {
            if (mouseY < getY() || mouseY > getBottom())
                return;
            int rowLeft = getRowLeft();

            BaseEntry hoveredEntry = getHovered();

            // Item icon tooltip (data rows only — never the header entry)
            // 物品图标 tooltip（仅数据行 —— 表头条目除外）
            if (hoveredEntry instanceof MergedEntry) {
                MergedEntry hoveredRow = (MergedEntry) hoveredEntry;
                if (mouseX >= rowLeft + 40 && mouseX <= rowLeft + 60) {
                    String name = ("" + I18n.format(
                            hoveredRow.item.getUnlocalizedName() + ".name")).trim();
                    if (!name.isEmpty()) {
                        drawHoverTooltip(Arrays.asList(name), mouseX, mouseY);
                    }
                    return;
                }
            }

            // Header icon tooltips (6 columns) — the header is the first list entry
            // 列表头图标 tooltip（6 列）—— 表头即列表第一个条目
            if (hoveredEntry == headerEntry) {
                String[] tips = { "stat.mined", "stat.depleted", "stat.crafted",
                        "stat.used", "stat.pickup", "stat.drop" };
                for (int col = 0; col < 6; col++) {
                    int colLeft = rowLeft + getColumnX(col) - 18;
                    int colRight = rowLeft + getColumnX(col);
                    if (mouseX >= colLeft && mouseX <= colRight) {
                        String tip = ("" + I18n.format(tips[col])).trim();
                        if (!tip.isEmpty()) {
                            drawHoverTooltip(Arrays.asList(tip), mouseX, mouseY);
                        }
                        return;
                    }
                }
            }
        }

        /**
         * Draws a vanilla-style blue-bordered tooltip.
         * Replicates {@code GuiScreen.drawHoveringText} which is protected
         * and therefore inaccessible from our package.
         */
        private void drawHoverTooltip(List<String> lines, int x, int y) {
            if (lines.isEmpty())
                return;

            int maxWidth = 0;
            for (String s : lines) {
                int w = mc.fontRenderer.getStringWidth(s);
                if (w > maxWidth)
                    maxWidth = w;
            }
            int height = lines.size() * 10 - 2;

            int tx = x + 12;
            int ty = y - 12;
            if (tx + maxWidth > this.width) {
                tx -= 28 + maxWidth;
            }
            if (ty + height + 6 > this.height) {
                ty = this.height - height - 6;
            }

            // Background
            Gui.drawRect(tx - 3, ty - 4, tx + maxWidth + 3, ty - 3, 0x505000FF); // top border
            Gui.drawRect(tx - 3, ty + height + 3, tx + maxWidth + 3, ty + height + 4, 0x5028007F); // bottom border
            Gui.drawRect(tx - 3, ty - 3, tx + maxWidth + 3, ty + height + 3, 0xF0100010); // fill
            Gui.drawRect(tx - 4, ty - 3, tx - 3, ty + height + 3, 0x505000FF); // left border
            Gui.drawRect(tx + maxWidth + 3, ty - 3, tx + maxWidth + 4, ty + height + 3, 0x5028007F); // right border
            // Gradient top edge
            Gui.drawRect(tx - 3, ty - 3 + 1, tx - 3 + 1, ty + height + 3 - 1, 0x5028007F);
            Gui.drawRect(tx + maxWidth + 2, ty - 3 + 1, tx + maxWidth + 3, ty + height + 3 - 1, 0x5028007F);

            // Text
            for (int i = 0; i < lines.size(); i++) {
                mc.fontRenderer.drawStringWithShadow(lines.get(i), tx, ty + i * 10, -1);
            }
        }

        // ---- Helpers ----

        private void drawStat(StatBase stat, int columnX, int y, boolean even) {
            String s;
            if (stat != null) {
                s = stat.func_75968_a(statFileWriter.writeStat(stat));
            } else {
                s = "-";
            }
            // GuiScreen.drawString is protected and the host is not necessarily a GuiStats;
            // drawStringWithShadow is its exact equivalent
            // GuiScreen.drawString 是 protected 且宿主不一定是 GuiStats；drawStringWithShadow 与之完全等价
            mc.fontRenderer.drawStringWithShadow(s,
                    columnX - mc.fontRenderer.getStringWidth(s), y + 5,
                    even ? 16777215 : 9474192);
        }

        private void drawItemIcon(int x, int y, Item item, int damage) {
            drawSprite(x + 1, y + 1, 0, 0);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.enableGUIStandardItemLighting();
            RENDER_ITEM.renderItemIntoGUI(mc.fontRenderer, mc.getTextureManager(),
                    new ItemStack(item, 1, damage), x + 2, y + 2);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        }

        /**
         * Vanilla creates stats with {@code new ItemStack(block)} which
         * always uses meta=0. For most blocks that is fine, but a few
         * (notably {@code Blocks.tallgrass}) have a visually misleading
         * meta-0 icon — tallgrass meta-0 is a dead bush, while the
         * "real" tall-grass variant is meta 1.
         */
        private int getDisplayDamage(Item item, boolean isBlock) {
            if (!isBlock)
                return 0;
            if (item instanceof ItemBlock) {
                Block block = ((ItemBlock) item).field_150939_a;
                // Tall grass: meta 0 = dead bush, use meta 1 = grass
                if (block == Blocks.tallgrass)
                    return 1;
            }
            return 0;
        }

        private void drawSprite(int x, int y, int u, int v) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(STAT_ICONS);
            float f = 0.0078125F;
            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.addVertexWithUV(x + 0, y + 18, 0, (u + 0) * f, (v + 18) * f);
            tess.addVertexWithUV(x + 18, y + 18, 0, (u + 18) * f, (v + 18) * f);
            tess.addVertexWithUV(x + 18, y + 0, 0, (u + 18) * f, (v + 0) * f);
            tess.addVertexWithUV(x + 0, y + 0, 0, (u + 0) * f, (v + 0) * f);
            tess.draw();
        }

        private void drawModernSprite(int x, int y, int u) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(MODERN_STAT_ICONS);
            float f = 0.0078125F;
            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.addVertexWithUV(x + 0, y + 18, 0, (u + 0) * f, 18 * f);
            tess.addVertexWithUV(x + 18, y + 18, 0, (u + 18) * f, 18 * f);
            tess.addVertexWithUV(x + 18, y + 0, 0, (u + 18) * f, 0);
            tess.addVertexWithUV(x + 0, y + 0, 0, (u + 0) * f, 0);
            tess.draw();
        }

        private void drawPickupStat(int itemId, int columnX, int y, boolean even) {
            int count = decok.dfcdvadstf.modernstatistic.ItemStatsTracker.getPickupCount(itemId);
            String s = count > 0 ? String.valueOf(count) : "-";
            // Right-align: value ends at column center
            // 右对齐：数值在列中心结束
            mc.fontRenderer.drawStringWithShadow(s,
                    columnX - mc.fontRenderer.getStringWidth(s), y + 5,
                    even ? 16777215 : 9474192);
        }

        private void drawDropStat(int itemId, int columnX, int y, boolean even) {
            int count = decok.dfcdvadstf.modernstatistic.ItemStatsTracker.getDropCount(itemId);
            String s = count > 0 ? String.valueOf(count) : "-";
            // Right-align: value ends at column center
            // 右对齐：数值在列中心结束
            mc.fontRenderer.drawStringWithShadow(s,
                    columnX - mc.fontRenderer.getStringWidth(s), y + 5,
                    even ? 16777215 : 9474192);
        }

        // ---- Entries ----

        /**
         * Common entry base — the counterpart of vanilla's abstract
         * {@code ItemStatisticsList.Entry}; the list holds the header and the
         * data rows through this type.
         * <p>
         * 条目公共基类——对应原版抽象的 {@code ItemStatisticsList.Entry}；
         * 列表通过该类型同时容纳表头与数据行。
         * </p>
         */
        private abstract class BaseEntry
                extends ContainerObjectSelectionList.Entry<BaseEntry> {
        }

        /**
         * Column header rendered as the first scrollable entry, exactly like the
         * high-version vanilla StatsScreen's HeaderEntry: it scrolls with the
         * rows, so it can never cover the first data row.
         * <p>
         * 以第一个可滚动条目的形式渲染列表头，与高版本原版 StatsScreen 的
         * HeaderEntry 完全一致：随行滚动，因此永远不会遮住第一行数据。
         * </p>
         */
        private class HeaderEntry extends BaseEntry {

            @Override
            public List<? extends GuiEventListener> children() {
                // The header has no child widgets / 表头没有子组件
                return Collections.emptyList();
            }

            @Override
            public void renderContent(int mouseX, int mouseY, boolean hovered,
                    float partialTicks) {
                int headerY = getY();
                int hoveredCol = getHeaderColumnAt(mouseX, mouseY);

                for (int col = 0; col < 6; col++) {
                    int colX = getX() + getColumnX(col);
                    boolean isHovered = hoveredCol == col;
                    boolean isModern = COLUMN_USE_MODERN[col];

                    // 1. Button background: SLOT texture (u=0,v=0) when hovered,
                    // HEADER (u=0,v=18) when not
                    // 按钮背景：悬停时用 SLOT 纹理 (u=0,v=0)，否则 HEADER (u=0,v=18)
                    int bgV = isHovered ? 0 : 18;
                    drawSprite(colX - 18, headerY + 1, 0, bgV);

                    // 2. Column icon (shifted +1,+1 when hovered for pressed effect)
                    // 列图标（悬停时偏移 +1,+1 制造按压效果）
                    int iconX = colX - 18 + (isHovered ? 1 : 0);
                    int iconY = headerY + 1 + (isHovered ? 1 : 0);
                    if (isModern) {
                        drawModernSprite(iconX, iconY, COLUMN_ICON_U[col]);
                    } else {
                        drawSprite(iconX, iconY, COLUMN_ICON_U[col], COLUMN_ICON_V[col]);
                    }
                }

                // 3. Sort direction arrow (stats_icons.png row v=0: ARROW_UP(u=18)
                // | ARROW_DOWN(u=36)); ascending points up and descending points
                // down, matching the high-version SORT_UP/SORT_DOWN sprite choice.
                // 排序方向箭头（stats_icons.png 第 v=0 行）；升序朝上、降序朝下，
                // 与高版本 SORT_UP/SORT_DOWN 的取法一致。
                if (sortColumn >= 0 && sortColumn < 6) {
                    int arrowU = (sortDirection == 1) ? 18 : 36;
                    drawSprite(getX() + getColumnX(sortColumn) - 36, headerY + 1,
                            arrowU, 0);
                }
            }
        }

        // ---- Entry ----

        private class MergedEntry extends BaseEntry {

            final Item item;
            final int itemId;
            final boolean isBlock;
            final int damage;

            MergedEntry(Item item, int itemId, boolean isBlock) {
                this(item, itemId, isBlock, 0);
            }

            MergedEntry(Item item, int itemId, boolean isBlock, int damage) {
                this.item = item;
                this.itemId = itemId;
                this.isBlock = isBlock;
                this.damage = damage;
            }

            @Override
            public List<? extends GuiEventListener> children() {
                // This entry has no child components
                // 本条目没有子组件
                return java.util.Collections.emptyList();
            }

            @Override
            public void renderContent(int mouseX, int mouseY, boolean hovered,
                    float partialTicks) {
                // Zebra striping by the index among all entries, header included —
                // exactly the vanilla children().indexOf(this) formula
                // 斑马纹按全部条目（含表头）中的下标取色——与原版
                // children().indexOf(this) 公式完全一致
                boolean even = MergedSelectionList.this.children().indexOf(this) % 2 == 0;
                drawItemIcon(getX() + 40, getY(), item, damage);
                int id = itemId;

                // Col 0: BLOCK_MINED — blocks only
                if (isBlock && StatList.mineBlockStatArray[id] != null) {
                    drawStat(StatList.mineBlockStatArray[id],
                            getX() + getColumnX(0), getY(), even);
                } else {
                    drawStat(null, getX() + getColumnX(0), getY(), even);
                }
                // Col 1: ITEM_BROKEN — damageable items only
                if (!isBlock && StatList.objectBreakStats[id] != null) {
                    drawStat(StatList.objectBreakStats[id],
                            getX() + getColumnX(1), getY(), even);
                } else {
                    drawStat(null, getX() + getColumnX(1), getY(), even);
                }
                // Col 2: ITEM_CRAFTED
                drawStat(StatList.objectCraftStats[id],
                        getX() + getColumnX(2), getY(), even);
                // Col 3: ITEM_USED
                drawStat(StatList.objectUseStats[id],
                        getX() + getColumnX(3), getY(), even);
                // Col 4: ITEM_PICKED_UP
                drawPickupStat(id, getX() + getColumnX(4), getY(), even);
                // Col 5: ITEM_DROPPED
                drawDropStat(id, getX() + getColumnX(5), getY(), even);
            }

            /**
             * Get stat value for the given column (0=BLOCK_MINED, 1=ITEM_BROKEN,
             * 2=ITEM_CRAFTED, 3=ITEM_USED, 4=PICKED_UP, 5=DROPPED).
             */
            int getStatValue(int column, net.minecraft.stats.StatFileWriter writer) {
                StatBase stat = null;
                switch (column) {
                    case 0: // BLOCK_MINED
                        if (isBlock) {
                            stat = StatList.mineBlockStatArray[itemId];
                        }
                        break;
                    case 1: // ITEM_BROKEN
                        if (!isBlock) {
                            stat = StatList.objectBreakStats[itemId];
                        }
                        break;
                    case 2: // ITEM_CRAFTED
                        stat = StatList.objectCraftStats[itemId];
                        break;
                    case 3: // ITEM_USED
                        stat = StatList.objectUseStats[itemId];
                        break;
                    case 4: // ITEM_PICKED_UP
                        return decok.dfcdvadstf.modernstatistic.ItemStatsTracker.getPickupCount(itemId);
                    case 5: // ITEM_DROPPED
                        return decok.dfcdvadstf.modernstatistic.ItemStatsTracker.getDropCount(itemId);
                }
                return stat != null ? writer.writeStat(stat) : 0;
            }
        }
    }
}
