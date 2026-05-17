package decok.dfcdvadstf.modernstatistic.tab;

import decok.dfcdvadstf.createworldui.api.ContentPanelRenderer;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.achievement.GuiStats;
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
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Merged tab — combines the vanilla "Blocks" and "Items" stat lists into one view.
 * <p>Columns: Mined | Crafted | Used | Broken | Pickup | Drop.</p>
 * <p>Mined shows block-mining stats (blocks only);
 * Broken shows tool-depletion stats (damageable items only);
 * Crafted / Used apply to both blocks and items.
 * Pickup / Drop show item pickup and drop counts from event tracking.
 * Rows that lack a given stat simply display "-".</p>
 */
public class StatsItemsTab extends StatsTab {

    private static final ResourceLocation STAT_ICONS =
            new ResourceLocation("textures/gui/container/stats_icons.png");
    private static final ResourceLocation MODERN_STAT_ICONS =
            new ResourceLocation("modernstatistic","textures/gui/stats_modern_icon.png");
    private static final RenderItem RENDER_ITEM = new RenderItem();

    // Column icon UVs matching vanilla GuiStats stats_icons.png (128x128, f=0.0078125)
    // Col 0 (BLOCK_MINED):  u=72,v=18  — StatsBlock col 0 (L732-L762)
    // Col 1 (ITEM_BROKEN): u=18,v=18  — StatsItem col 0 (L535-L569)
    // Col 2 (ITEM_CRAFTED):u=36,v=18  — StatsItem col 1
    // Col 3 (ITEM_USED):   u=54,v=18  — StatsItem col 2
    // Col 4-5: stats_modern_icon.png (PICKUP u=0, DROP u=18)
    private static final int[] COLUMN_ICON_U = {72, 18, 36, 54, 0, 18};
    private static final int[] COLUMN_ICON_V = {18, 18, 18, 18, 0, 18};
    private static final boolean[] COLUMN_USE_MODERN = {false, false, false, false, true, true};

    private MergedSlot slot;

    public StatsItemsTab() {
        super(101, "stat.itemsButton");
    }

    @Override
    public void initGui(GuiStats parent,
                        int width, int height, List<GuiButton> buttonList,
                        StatFileWriter writer) {
        super.initGui(parent, width, height, buttonList, writer);
        this.slot = new MergedSlot();
        this.slot.registerScrollButtons(1, 1);
        setVisible(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!visible || slot == null) return;
        slot.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void actionPerformed(GuiButton button) {
        // Column header clicks are handled inside the slot
    }

    /** @return true if the merged list is empty (used to disable the tab button) */
    public boolean isEmpty() {
        return slot == null || slot.getSize() == 0;
    }

    // ==================== Inner GuiSlot ====================

    private class MergedSlot extends GuiSlot {

        private final List<MergedEntry> entries = new ArrayList<>();
        // Column order matching high-version StatsScreen:
        // 0=BLOCK_MINED, 1=ITEM_BROKEN, 2=ITEM_CRAFTED, 3=ITEM_USED, 4=ITEM_PICKED_UP, 5=ITEM_DROPPED
        private int sortColumn = -1;
        private int sortDirection = -1; // 1=ascending, -1=descending
        private int hoveredHeader = -1;



        MergedSlot() {
            super(mc, parentScreen.width, parentScreen.height, 22,
                    parentScreen.height - 35, 20);
            setShowSelectionBox(false);
            setHasListHeader(true, 20);
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
                if (byId.containsKey(id)) continue;

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
            //  objectUseStats is recorded like throwable/placeable items)
            for (Object obj : Item.itemRegistry) {
                Item item = (Item) obj;
                if (item == null) continue;
                int id = Item.getIdFromItem(item);
                if (byId.containsKey(id)) continue;

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

            entries.addAll(byId.values());
        }

        private void sortById() {
            Collections.sort(entries, new Comparator<MergedEntry>() {
                @Override
                public int compare(MergedEntry a, MergedEntry b) {
                    return Integer.compare(a.itemId, b.itemId);
                }
            });
        }

        // ---- GuiSlot overrides ----

        @Override
        protected int getSize() {
            return entries.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick,
                                       int mouseX, int mouseY) {}

        @Override
        protected boolean isSelected(int index) {
            return false;
        }

        @Override
        protected int getContentHeight() {
            return getSize() * slotHeight;
        }

        @Override
        protected void drawContainerBackground(Tessellator tessellator) {
            mc.getTextureManager().bindTexture(Gui.optionsBackground);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            float f1 = 32.0F;
            int scrolled = getAmountScrolled();
            tessellator.startDrawingQuads();
            tessellator.setColorOpaque_I(4210752);
            tessellator.addVertexWithUV((double)this.left, (double)this.bottom, 0.0D, (double)((float)this.left / f1), (double)((float)(this.bottom + scrolled) / f1));
            tessellator.addVertexWithUV((double)this.right, (double)this.bottom, 0.0D, (double)((float)this.right / f1), (double)((float)(this.bottom + scrolled) / f1));
            tessellator.addVertexWithUV((double)this.right, (double)this.top, 0.0D, (double)((float)this.right / f1), (double)((float)(this.top + scrolled) / f1));
            tessellator.addVertexWithUV((double)this.left, (double)this.top, 0.0D, (double)((float)this.left / f1), (double)((float)(this.top + scrolled) / f1));
            tessellator.draw();
        }

        @Override
        protected void drawBackground() {
            ContentPanelRenderer.drawPanelBackground(0, 24,
                    parentScreen.width, parentScreen.height - 35);
        }

        // ---- Header ----

        @Override
        protected void drawListHeader(int x, int y, Tessellator tess) {
            if (!Mouse.isButtonDown(0)) {
                hoveredHeader = -1;
            }

            for (int col = 0; col < 6; col++) {
                int colX = x + getColumnX(col);
                boolean isHovered = hoveredHeader == col;
                boolean isModern = COLUMN_USE_MODERN[col];

                // 1. Button background: SLOT texture (u=0,v=0) when hovered, HEADER (u=0,v=18) when not
                int bgV = isHovered ? 0 : 18;
                drawSprite(colX - 18, y + 1, 0, bgV);

                // 2. Column icon (shifted +1,+1 when hovered for pressed effect)
                int iconX = colX - 18 + (isHovered ? 1 : 0);
                int iconY = y + 1 + (isHovered ? 1 : 0);
                if (isModern) {
                    drawModernSprite(iconX, iconY, COLUMN_ICON_U[col]);
                } else {
                    drawSprite(iconX, iconY, COLUMN_ICON_U[col], COLUMN_ICON_V[col]);
                }
            }

            // 3. Sort direction arrow (stats_icons.png: u=0,v=36=descending, u=18,v=36=ascending)
            if (sortColumn >= 0 && sortColumn < 6) {
                int arrowU = (sortDirection == 1) ? 18 : 0;
                drawSprite(x + getColumnX(sortColumn) - 36, y + 1, arrowU, 36);
            }
        }

        @Override
        protected void func_148132_a(int mouseX, int mouseY) {
            hoveredHeader = -1;

            if (mouseY >= top && mouseY <= top + 20) {
                // mouseX is relative to this.left (GuiSlot subtracts left before calling)
                for (int col = 0; col < 6; col++) {
                    int colLeft = getColumnX(col) - 18;
                    int colRight = getColumnX(col);
                    if (mouseX >= colLeft && mouseX < colRight) {
                        hoveredHeader = col;
                        break;
                    }
                }
            }

            if (hoveredHeader >= 0) {
                sortByColumn(hoveredHeader);
                mc.getSoundHandler().playSound(
                        PositionedSoundRecord.func_147674_a(
                                new ResourceLocation("gui.button.press"), 1.0F));
            }
        }

        /** X center of column col (0..5), matching high-version formula: 75 + 40*col */
        private int getColumnX(int col) {
            return 75 + 40 * col;
        }

        @Override
        protected int getScrollBarX() {
            return this.width / 2 + getListWidth() / 2 + 4;
        }

        @Override
        public int getListWidth() {
            // Content width: from icon (x+40) to rightmost column center + 18
            return getColumnX(5) + 18 - 40 + 4;
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
            Collections.sort(entries, new Comparator<MergedEntry>() {
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

        // ---- Slot drawing ----

        @Override
        protected void drawSlot(int index, int x, int y, int slotHeight,
                                 Tessellator tess, int mouseX, int mouseY) {
            MergedEntry entry = entries.get(index);
            drawItemIcon(x + 40, y, entry.item, entry.damage);
            int id = entry.itemId;
            boolean even = index % 2 == 0;

            // Col 0: BLOCK_MINED — blocks only
            if (entry.isBlock && StatList.mineBlockStatArray[id] != null) {
                drawStat(StatList.mineBlockStatArray[id], x + getColumnX(0), y, even);
            } else {
                drawStat(null, x + getColumnX(0), y, even);
            }
            // Col 1: ITEM_BROKEN — damageable items only
            if (!entry.isBlock && StatList.objectBreakStats[id] != null) {
                drawStat(StatList.objectBreakStats[id], x + getColumnX(1), y, even);
            } else {
                drawStat(null, x + getColumnX(1), y, even);
            }
            // Col 2: ITEM_CRAFTED
            drawStat(StatList.objectCraftStats[id], x + getColumnX(2), y, even);
            // Col 3: ITEM_USED
            drawStat(StatList.objectUseStats[id], x + getColumnX(3), y, even);
            // Col 4: ITEM_PICKED_UP
            drawPickupStat(id, x + getColumnX(4), y, even);
            // Col 5: ITEM_DROPPED
            drawDropStat(id, x + getColumnX(5), y, even);
        }

        // ---- Tooltip ----

        @Override
        protected void func_148142_b(int mouseX, int mouseY) {
            if (mouseY < top || mouseY > bottom) return;

            int index = func_148124_c(mouseX, mouseY);
            int slotLeft = this.left;

            if (index >= 0) {
                if (mouseX >= slotLeft + 40 && mouseX <= slotLeft + 40 + 20) {
                    MergedEntry entry = entries.get(index);
                    String name = ("" + I18n.format(
                            entry.item.getUnlocalizedName() + ".name")).trim();
                    if (!name.isEmpty()) {
                        int tx = mouseX + 12;
                        int ty = mouseY - 12;
                        int tw = mc.fontRenderer.getStringWidth(name);
                        Gui.drawRect(tx - 3, ty - 3,
                                tx + tw + 3, ty + 8 + 3, 0xC0000000);
                        mc.fontRenderer.drawStringWithShadow(name, tx, ty, -1);
                    }
                    return;
                }
            }

            // Header tooltips
            if (index < 0) {
                String[] tips = {"stat.mined", "stat.broken", "stat.crafted",
                        "stat.used", "stat.pickup", "stat.drop"};
                for (int col = 0; col < 6; col++) {
                    int colLeft = slotLeft + getColumnX(col) - 18;
                    int colRight = slotLeft + getColumnX(col);
                    if (mouseX >= colLeft && mouseX <= colRight) {
                        String tip = ("" + I18n.format(tips[col])).trim();
                        if (!tip.isEmpty()) {
                            int tx = mouseX + 12;
                            int ty = mouseY - 12;
                            int tw = mc.fontRenderer.getStringWidth(tip);
                            Gui.drawRect(tx - 3, ty - 3,
                                    tx + tw + 3, ty + 8 + 3, 0xC0000000);
                            mc.fontRenderer.drawStringWithShadow(tip, tx, ty, -1);
                        }
                        return;
                    }
                }
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
            // Right-align: value ends at column center
            parentScreen.drawString(mc.fontRenderer, s,
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
         * always uses meta=0.  For most blocks that is fine, but a few
         * (notably {@code Blocks.tallgrass}) have a visually misleading
         * meta-0 icon — tallgrass meta-0 is a dead bush, while the
         * "real" tall-grass variant is meta 1.
         */
        private int getDisplayDamage(Item item, boolean isBlock) {
            if (!isBlock) return 0;
            if (item instanceof ItemBlock) {
                Block block = ((ItemBlock) item).field_150939_a;
                // Tall grass: meta 0 = dead bush, use meta 1 = grass
                if (block == Blocks.tallgrass) return 1;
            }
            return 0;
        }

        private void drawSprite(int x, int y, int u, int v) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(STAT_ICONS);
            float f = 0.0078125F;
            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.addVertexWithUV(x + 0,  y + 18, 0, (u + 0)  * f, (v + 18) * f);
            tess.addVertexWithUV(x + 18, y + 18, 0, (u + 18) * f, (v + 18) * f);
            tess.addVertexWithUV(x + 18, y + 0,  0, (u + 18) * f, (v + 0)  * f);
            tess.addVertexWithUV(x + 0,  y + 0,  0, (u + 0)  * f, (v + 0)  * f);
            tess.draw();
        }
        
        private void drawModernSprite(int x, int y, int u) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(MODERN_STAT_ICONS);
            float f = 0.0078125F;
            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.addVertexWithUV(x + 0,  y + 18, 0, (u + 0)  * f, 18 * f);
            tess.addVertexWithUV(x + 18, y + 18, 0, (u + 18) * f, 18 * f);
            tess.addVertexWithUV(x + 18, y + 0,  0, (u + 18) * f, 0);
            tess.addVertexWithUV(x + 0,  y + 0,  0, (u + 0)  * f, 0);
            tess.draw();
        }
        
        private void drawPickupStat(int itemId, int columnX, int y, boolean even) {
            int count = decok.dfcdvadstf.modernstatistic.ItemStatsTracker.getPickupCount(itemId);
            String s = count > 0 ? String.valueOf(count) : "-";
            // Right-align
            parentScreen.drawString(mc.fontRenderer, s,
                    columnX - mc.fontRenderer.getStringWidth(s), y + 5,
                    even ? 16777215 : 9474192);
        }
        
        private void drawDropStat(int itemId, int columnX, int y, boolean even) {
            int count = decok.dfcdvadstf.modernstatistic.ItemStatsTracker.getDropCount(itemId);
            String s = count > 0 ? String.valueOf(count) : "-";
            // Right-align
            parentScreen.drawString(mc.fontRenderer, s,
                    columnX - mc.fontRenderer.getStringWidth(s), y + 5,
                    even ? 16777215 : 9474192);
        }
    }

    // ==================== Data class ====================

    private static class MergedEntry {
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

        /** Get stat value for the given column (0=BLOCK_MINED, 1=ITEM_BROKEN, 2=ITEM_CRAFTED, 3=ITEM_USED, 4=PICKED_UP, 5=DROPPED). */
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
