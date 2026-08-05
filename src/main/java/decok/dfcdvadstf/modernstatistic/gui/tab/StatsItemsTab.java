package decok.dfcdvadstf.modernstatistic.gui.tab;

import decok.dfcdvadstf.catframe.ui.ContentPanelRenderer;
import decok.dfcdvadstf.catframe.ui.components.Component;
import decok.dfcdvadstf.catframe.ui.components.ContainerObjectSelectionList;
import decok.dfcdvadstf.catframe.ui.components.tab.AbstractScreenTab;
import decok.dfcdvadstf.catframe.ui.navigation.ScreenRectangle;
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

import java.util.Arrays;
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

    public StatsItemsTab() {
        super(106, "stat.itemsButton");
    }

    public void initGui(int width, int height, List<GuiButton> buttonList,
            StatFileWriter writer) {
        this.statFileWriter = writer;
        this.list = new MergedSelectionList(width, height);
        setVisible(false);
    }

    /**
     * The list component, registered into the screen's widget pipeline by
     * {@code GuiStatics.initTabs} so rendering, clicks and the scroll wheel are
     * dispatched automatically.
     * <p>
     * 列表组件，由 {@code GuiStatics.initTabs} 注册进界面的组件管线，
     * 渲染 / 点击 / 滚轮自动分发。
     * </p>
     */
    public Component getList() {
        return list;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // The list is rendered by the screen's component pipeline (addRenderableWidget)
        // 列表由界面的组件管线渲染（addRenderableWidget）
    }

    @Override
    public void actionPerformed(GuiButton button) {
        // Column header clicks are handled inside the list
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // Mouse events are dispatched by the screen's component pipeline
        // 鼠标事件由界面的组件管线分发
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
        // Fill the content zone (between the header/footer separators) with the list
        // 用列表填充内容区（Header/Footer 分隔线之间）
        if (list != null) {
            list.updateSizeAndPosition(rectangle.width, rectangle.height,
                    rectangle.left(), rectangle.top());
        }
    }

    public boolean isEmpty() {
        return list == null || list.size() == 0;
    }

    // ==================== Inner selection list ====================

    private class MergedSelectionList
            extends ContainerObjectSelectionList<MergedSelectionList.MergedEntry> {

        /** Header height in pixels / 列表头高度（像素） */
        private static final int HEADER_HEIGHT = 20;

        /**
         * Screen height at construction time, for the panel background /
         * 构造时的屏幕高度（面板背景用）
         */
        private final int screenHeight;
        // Column order matching high-version StatsScreen:
        // 0=BLOCK_MINED, 1=ITEM_BROKEN, 2=ITEM_CRAFTED, 3=ITEM_USED, 4=ITEM_PICKED_UP,
        // 5=ITEM_DROPPED
        private int sortColumn = -1;
        private int sortDirection = -1; // 1=ascending, -1=descending
        private int hoveredHeader = -1;

        MergedSelectionList(int width, int height) {
            super(width, height, 22, 20);
            this.screenHeight = height;
            buildEntries();
            sortById();
        }

        /** @return the number of entries / 条目数量 */
        public int size() {
            return getItemCount();
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
            sort(new Comparator<MergedEntry>() {
                @Override
                public int compare(MergedEntry a, MergedEntry b) {
                    return Integer.compare(a.itemId, b.itemId);
                }
            });
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

        @Override
        protected void renderBackground(int mouseX, int mouseY, float partialTicks) {
            // Panel background (bottom strip) + tiled dark over the visible list area,
            // mirroring the vanilla slot's drawBackground/drawContainerBackground pair
            // 面板背景（底部条带）+ 可见列表区的平铺深色纹理，
            // 与原版槽的 drawBackground/drawContainerBackground 组合一致
            ContentPanelRenderer.drawPanelBackground(0, getY() + 2, getWidth(),
                    screenHeight - 35);
            mc.getTextureManager().bindTexture(Gui.optionsBackground);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            float f1 = 32.0F;
            int scrolled = (int) scrollAmount();
            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.setColorOpaque_I(4210752);
            tess.addVertexWithUV((double) getX(), (double) getBottom(), 0.0D,
                    (double) ((float) getX() / f1),
                    (double) ((float) (getBottom() + scrolled) / f1));
            tess.addVertexWithUV((double) getRight(), (double) getBottom(), 0.0D,
                    (double) ((float) getRight() / f1),
                    (double) ((float) (getBottom() + scrolled) / f1));
            tess.addVertexWithUV((double) getRight(), (double) getY(), 0.0D,
                    (double) ((float) getRight() / f1),
                    (double) ((float) (getY() + scrolled) / f1));
            tess.addVertexWithUV((double) getX(), (double) getY(), 0.0D,
                    (double) ((float) getX() / f1),
                    (double) ((float) (getY() + scrolled) / f1));
            tess.draw();
        }

        @Override
        protected void renderListItems(int mouseX, int mouseY, float partialTicks) {
            // Keep each entry's visual row index in sync for the zebra striping
            // 同步每个条目的视觉行号，用于斑马纹着色
            int i = 0;
            for (MergedEntry child : children()) {
                if (child.getY() + child.getHeight() >= getY()
                        && child.getY() <= getBottom()) {
                    child.rowIndex = i;
                    renderItem(child, mouseX, mouseY, partialTicks);
                }
                i++;
            }
        }

        // ---- Header ----

        @Override
        protected void renderSeparators() {
            // Column header — drawn outside the scissor so it never scrolls
            // 列表头 —— 在 scissor 之外绘制，固定不滚动
            int rowLeft = getRowLeft();

            for (int col = 0; col < 6; col++) {
                int colX = rowLeft + getColumnX(col);
                boolean isHovered = hoveredHeader == col;
                boolean isModern = COLUMN_USE_MODERN[col];

                // 1. Button background: SLOT texture (u=0,v=0) when hovered, HEADER (u=0,v=18)
                // when not
                int bgV = isHovered ? 0 : 18;
                drawSprite(colX - 18, getY() + 1, 0, bgV);

                // 2. Column icon (shifted +1,+1 when hovered for pressed effect)
                int iconX = colX - 18 + (isHovered ? 1 : 0);
                int iconY = getY() + 1 + (isHovered ? 1 : 0);
                if (isModern) {
                    drawModernSprite(iconX, iconY, COLUMN_ICON_U[col]);
                } else {
                    drawSprite(iconX, iconY, COLUMN_ICON_U[col], COLUMN_ICON_V[col]);
                }
            }

            // 3. Sort direction arrow (stats_icons.png row v=0: ARROW_UP(u=18) |
            // ARROW_DOWN(u=36))
            if (sortColumn >= 0 && sortColumn < 6) {
                int arrowU = (sortDirection == 1) ? 36 : 18;
                drawSprite(rowLeft + getColumnX(sortColumn) - 36, getY() + 1,
                        arrowU, 0);
            }
        }

        /** X center of column col (0..5), matching high-version formula: 75 + 40*col */
        private int getColumnX(int col) {
            return 75 + 40 * col;
        }

        // ---- Header interaction ----

        @Override
        public void render(int mouseX, int mouseY, float partialTicks) {
            // Refresh the header hover before the render pipeline draws it
            // 在渲染管线绘制列表头之前刷新其悬停状态
            updateHeaderHover(mouseX, mouseY);
            super.render(mouseX, mouseY, partialTicks);
            drawTooltip(mouseX, mouseY);
        }

        private void updateHeaderHover(int mouseX, int mouseY) {
            hoveredHeader = -1;
            if (mouseY < getY() || mouseY >= getY() + HEADER_HEIGHT) {
                return;
            }
            int rowLeft = getRowLeft();
            for (int col = 0; col < 6; col++) {
                int colLeft = rowLeft + getColumnX(col) - 18;
                int colRight = rowLeft + getColumnX(col);
                if (mouseX >= colLeft && mouseX < colRight) {
                    hoveredHeader = col;
                    break;
                }
            }
        }

        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            // Header click sorts the column; anything else falls through to the
            // scrollbar/list handling
            // 表头点击排序；其余交给滚动条 / 列表处理
            if (mouseButton == 0 && mouseY >= getY()
                    && mouseY < getY() + HEADER_HEIGHT) {
                int col = getHeaderColumnAt(mouseX);
                if (col >= 0) {
                    sortByColumn(col);
                    mc.getSoundHandler().playSound(
                            PositionedSoundRecord.func_147674_a(
                                    new ResourceLocation("gui.button.press"), 1.0F));
                    return;
                }
            }
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }

        private int getHeaderColumnAt(int mouseX) {
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
            sort(new Comparator<MergedEntry>() {
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

            MergedEntry hoveredEntry = getHovered();

            // Item icon tooltip
            if (hoveredEntry != null) {
                if (mouseX >= rowLeft + 40 && mouseX <= rowLeft + 60) {
                    String name = ("" + I18n.format(
                            hoveredEntry.item.getUnlocalizedName() + ".name")).trim();
                    if (!name.isEmpty()) {
                        drawHoverTooltip(Arrays.asList(name), mouseX, mouseY);
                    }
                    return;
                }
            }

            // Header icon tooltips (6 columns)
            if (hoveredEntry == null && mouseY < getY() + HEADER_HEIGHT) {
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

        // ---- Entry ----

        private class MergedEntry extends ContainerObjectSelectionList.Entry<MergedEntry> {

            final Item item;
            final int itemId;
            final boolean isBlock;
            final int damage;
            /** Visual row index (current order), refreshed each frame / 视觉行号（当前顺序），每帧刷新 */
            int rowIndex;

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
            public List<? extends Component> children() {
                // This entry has no child components
                // 本条目没有子组件
                return java.util.Collections.emptyList();
            }

            @Override
            public void renderContent(int mouseX, int mouseY, boolean hovered,
                    float partialTicks) {
                boolean even = rowIndex % 2 == 0;
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
