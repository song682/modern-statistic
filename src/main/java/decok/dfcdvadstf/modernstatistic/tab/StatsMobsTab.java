package decok.dfcdvadstf.modernstatistic.tab;

import decok.dfcdvadstf.createworldui.api.ContentPanelRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityList;
import net.minecraft.stats.StatFileWriter;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tab for mob/entity statistics — kills and deaths per entity type.
 */
public class StatsMobsTab extends StatsTab {

    private MobsSlot slot;

    public StatsMobsTab() {
        super(102, "stat.mobsButton");
    }

    @Override
    public void initGui(GuiStats parent,
                        int width, int height, List<GuiButton> buttonList,
                        StatFileWriter writer) {
        super.initGui(parent, width, height, buttonList, writer);
        this.slot = new MobsSlot();
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
        // No interactive buttons in this tab
    }

    /** @return true if the mob list is empty (used to disable the tab button) */
    public boolean isEmpty() {
        return slot == null || slot.getSize() == 0;
    }

    // ---- Inner GuiSlot ----

    private class MobsSlot extends GuiSlot {

        private final List<EntityList.EntityEggInfo> mobEntries = new ArrayList<>();

        MobsSlot() {
            super(mc, parentScreen.width, parentScreen.height, 22,
                  parentScreen.height - 35, mc.fontRenderer.FONT_HEIGHT * 4);
            setShowSelectionBox(false);

            for (Object obj : EntityList.entityEggs.values()) {
                EntityList.EntityEggInfo info = (EntityList.EntityEggInfo) obj;
                if (statFileWriter.writeStat(info.field_151512_d) > 0
                        || statFileWriter.writeStat(info.field_151513_e) > 0) {
                    mobEntries.add(info);
                }
            }
        }

        @Override
        protected int getSize() {
            return mobEntries.size();
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
            return getSize() * mc.fontRenderer.FONT_HEIGHT * 4;
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

        @Override
        protected void drawSlot(int index, int x, int y, int slotHeight,
                                 Tessellator tess, int mouseX, int mouseY) {
            EntityList.EntityEggInfo info = mobEntries.get(index);
            String name = I18n.format(
                    "entity." + EntityList.getStringFromID(info.spawnedID) + ".name");
            int kills = statFileWriter.writeStat(info.field_151512_d);
            int killedBy = statFileWriter.writeStat(info.field_151513_e);
            String killsText = I18n.format(
                    "stat.entityKills", Integer.valueOf(kills), name);
            String killedByText = I18n.format(
                    "stat.entityKilledBy", name, Integer.valueOf(killedBy));

            if (kills == 0) {
                killsText = I18n.format("stat.entityKills.none", name);
            }
            if (killedBy == 0) {
                killedByText = I18n.format("stat.entityKilledBy.none", name);
            }

            int fh = mc.fontRenderer.FONT_HEIGHT;
            parentScreen.drawString(mc.fontRenderer, name, x + 2 - 10, y + 1, 16777215);
            parentScreen.drawString(mc.fontRenderer, killsText, x + 2,
                    y + 1 + fh, kills == 0 ? 6316128 : 9474192);
            parentScreen.drawString(mc.fontRenderer, killedByText, x + 2,
                    y + 1 + fh * 2, killedBy == 0 ? 6316128 : 9474192);
        }
    }
}
