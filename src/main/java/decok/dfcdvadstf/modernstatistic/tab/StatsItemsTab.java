package decok.dfcdvadstf.modernstatistic.tab;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatCrafting;
import net.minecraft.stats.StatList;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Merged tab — combines the vanilla "Blocks" and "Items" stat lists into one view.
 * <p>Columns: Crafted | Used | Mined/Broken.</p>
 * <p>The third column adapts: for blocks it shows mined count; for items it shows broken/depleted count.</p>
 */
public class StatsItemsTab extends StatsTab {

    private static final ResourceLocation STAT_ICONS =
            new ResourceLocation("textures/gui/container/stats_icons.png");
    private static final RenderItem RENDER_ITEM = new RenderItem();

    private MergedSlot slot;

    public StatsItemsTab() {
        super(101, "stat.itemsButton");
    }

    @Override
    public void initGui(net.minecraft.client.gui.achievement.GuiStats parent,
                        int width, int height, List<GuiButton> buttonList,
                        net.minecraft.stats.StatFileWriter writer) {
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
        private int sortColumn = -1;  // -1 = unsorted, 0=crafted, 1=used, 2=mined/broken
        private int sortDirection = -1; // 1=ascending, -1=descending
        private int hoveredHeader = -1;

        MergedSlot() {
            super(mc, parentScreen.width, parentScreen.height, 32,
                  parentScreen.height - 64, 20);
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
                if (!hasStats) {
                    hasStats = StatList.objectUseStats[id] != null
                            && statFileWriter.writeStat(StatList.objectUseStats[id]) > 0;
                }
                if (!hasStats) {
                    hasStats = StatList.objectCraftStats[id] != null
                            && statFileWriter.writeStat(StatList.objectCraftStats[id]) > 0;
                }
                if (hasStats) {
                    byId.put(id, new MergedEntry(sc.func_150959_a(), id, sc, true));
                }
            }

            // Items: from itemStats (add only if not already present as block)
            for (Object obj : StatList.itemStats) {
                StatCrafting sc = (StatCrafting) obj;
                int id = Item.getIdFromItem(sc.func_150959_a());
                if (byId.containsKey(id)) continue;

                boolean hasStats = statFileWriter.writeStat(sc) > 0;
                if (!hasStats) {
                    hasStats = StatList.objectBreakStats[id] != null
                            && statFileWriter.writeStat(StatList.objectBreakStats[id]) > 0;
                }
                if (!hasStats) {
                    hasStats = StatList.objectCraftStats[id] != null
                            && statFileWriter.writeStat(StatList.objectCraftStats[id]) > 0;
                }
                if (hasStats) {
                    byId.put(id, new MergedEntry(sc.func_150959_a(), id, sc, false));
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
        protected void drawBackground() {
            parentScreen.drawDefaultBackground();
        }

        // ---- Header ----

        @Override
        protected void drawListHeader(int x, int y, Tessellator tess) {
            if (!Mouse.isButtonDown(0)) {
                hoveredHeader = -1;
            }

            // Header click detection — moved to func_148132_a below
            // Draw the three column sort-arrow buttons (from the base Stats pattern)
            drawHeaderButton(x + 115 - 18, y + 1, hoveredHeader == 0);
            drawHeaderButton(x + 165 - 18, y + 1, hoveredHeader == 1);
            drawHeaderButton(x + 215 - 18, y + 1, hoveredHeader == 2);

            // Draw column icons
            // Column 0: Crafted (u=18)
            drawSprite(x + 115 - 18 + (hoveredHeader == 0 ? 1 : 0),
                       y + 1 + (hoveredHeader == 0 ? 1 : 0), 18, 18);
            // Column 1: Used (u=36)
            drawSprite(x + 165 - 18 + (hoveredHeader == 1 ? 1 : 0),
                       y + 1 + (hoveredHeader == 1 ? 1 : 0), 36, 18);
            // Column 2: Mined/Broken (u=54 for mined as default)
            drawSprite(x + 215 - 18 + (hoveredHeader == 2 ? 1 : 0),
                       y + 1 + (hoveredHeader == 2 ? 1 : 0), 54, 18);

            // Draw sort direction indicator
            if (sortColumn != -1) {
                int sx = 79;
                if (sortColumn == 1) sx = 129;
                else if (sortColumn == 2) sx = 179;
                int sy = (sortDirection == 1) ? 36 : 0;
                drawSprite(x + sx, y + 1, sy, 0);
            }
        }

        @Override
        protected void func_148132_a(int mouseX, int mouseY) {
            hoveredHeader = -1;

            if (mouseY >= top && mouseY <= top + 20) {
                if (mouseX >= 79 && mouseX < 115) {
                    hoveredHeader = 0;
                } else if (mouseX >= 129 && mouseX < 165) {
                    hoveredHeader = 1;
                } else if (mouseX >= 179 && mouseX < 215) {
                    hoveredHeader = 2;
                }
            }

            if (hoveredHeader >= 0) {
                sortByColumn(hoveredHeader);
                mc.getSoundHandler().playSound(
                        PositionedSoundRecord.func_147674_a(
                                new ResourceLocation("gui.button.press"), 1.0F));
            }
        }

        private void sortByColumn(int column) {
            if (column != sortColumn) {
                sortColumn = column;
                sortDirection = -1; // default descending
            } else if (sortDirection == -1) {
                sortDirection = 1; // flip to ascending
            } else {
                sortColumn = -1; // reset to default sort
                sortDirection = 0;
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
            drawItemIcon(x + 40, y, entry.item);
            int id = entry.itemId;
            boolean even = index % 2 == 0;

            // Column 0: Crafted
            drawStat(StatList.objectCraftStats[id], x + 115, y, even);
            // Column 1: Used
            drawStat(StatList.objectUseStats[id], x + 165, y, even);
            // Column 2: Mined (for blocks) or Broken (for items)
            if (entry.isBlock && StatList.mineBlockStatArray[id] != null) {
                drawStat(StatList.mineBlockStatArray[id], x + 215, y, even);
            } else if (!entry.isBlock && StatList.objectBreakStats[id] != null) {
                drawStat(StatList.objectBreakStats[id], x + 215, y, even);
            } else {
                drawStat(entry.statCrafting, x + 215, y, even);
            }
        }

        // ---- Tooltip ----

        @Override
        protected void func_148142_b(int mouseX, int mouseY) {
            if (mouseY < top || mouseY > bottom) return;

            int index = func_148124_c(mouseX, mouseY);
            int left = width / 2 - 92 - 16;

            if (index >= 0) {
                if (mouseX >= left + 40 && mouseX <= left + 40 + 20) {
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
                String tip = "";
                if (mouseX >= left + 115 - 18 && mouseX <= left + 115) {
                    tip = "stat.crafted";
                } else if (mouseX >= left + 165 - 18 && mouseX <= left + 165) {
                    tip = "stat.used";
                } else if (mouseX >= left + 215 - 18 && mouseX <= left + 215) {
                    tip = "stat.mined";
                }
                tip = ("" + I18n.format(tip)).trim();
                if (!tip.isEmpty()) {
                    int tx = mouseX + 12;
                    int ty = mouseY - 12;
                    int tw = mc.fontRenderer.getStringWidth(tip);
                    Gui.drawRect(tx - 3, ty - 3,
                            tx + tw + 3, ty + 8 + 3, 0xC0000000);
                    mc.fontRenderer.drawStringWithShadow(tip, tx, ty, -1);
                }
            }
        }

        // ---- Helpers ----

        private void drawHeaderButton(int x, int y, boolean pressed) {
            drawSprite(x, y, 0, pressed ? 0 : 18);
        }

        private void drawStat(StatBase stat, int rightX, int y, boolean even) {
            String s;
            if (stat != null) {
                s = stat.func_75968_a(statFileWriter.writeStat(stat));
            } else {
                s = "-";
            }
            parentScreen.drawString(mc.fontRenderer, s,
                    rightX - mc.fontRenderer.getStringWidth(s), y + 5,
                    even ? 16777215 : 9474192);
        }

        private void drawItemIcon(int x, int y, Item item) {
            drawButtonBackground(x + 1, y + 1);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.enableGUIStandardItemLighting();
            RENDER_ITEM.renderItemIntoGUI(mc.fontRenderer, mc.getTextureManager(),
                    new ItemStack(item, 1, 0), x + 2, y + 2);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        }

        private void drawButtonBackground(int x, int y) {
            drawSprite(x, y, 0, 0);
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
    }

    // ==================== Data class ====================

    private static class MergedEntry {
        final Item item;
        final int itemId;
        final StatCrafting statCrafting; // original stat from mineStats or itemStats
        final boolean isBlock;

        MergedEntry(Item item, int itemId, StatCrafting statCrafting, boolean isBlock) {
            this.item = item;
            this.itemId = itemId;
            this.statCrafting = statCrafting;
            this.isBlock = isBlock;
        }

        /** Get stat value for the given column (0=crafted, 1=used, 2=mined/broken). */
        int getStatValue(int column, net.minecraft.stats.StatFileWriter writer) {
            StatBase stat = null;
            switch (column) {
                case 0:
                    stat = StatList.objectCraftStats[itemId];
                    break;
                case 1:
                    stat = StatList.objectUseStats[itemId];
                    break;
                case 2:
                    if (isBlock) {
                        stat = StatList.mineBlockStatArray[itemId];
                    } else {
                        stat = StatList.objectBreakStats[itemId];
                    }
                    if (stat == null) stat = statCrafting;
                    break;
            }
            return stat != null ? writer.writeStat(stat) : 0;
        }
    }
}
