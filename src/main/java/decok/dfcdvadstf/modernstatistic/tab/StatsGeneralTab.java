package decok.dfcdvadstf.modernstatistic.tab;

import decok.dfcdvadstf.createworldui.api.ContentPanelRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.stats.StatList;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Tab for general statistics (the vanilla "General" category).
 * <p>Shows a simple list of misc stats like play time, distance walked, etc.</p>
 */
public class StatsGeneralTab extends StatsTab {

    private GeneralSlot slot;

    public StatsGeneralTab() {
        super(100, "stat.generalButton");
    }

    @Override
    public void initGui(GuiStats parent,
                        int width, int height, List<GuiButton> buttonList,
                         StatFileWriter writer) {
        super.initGui(parent, width, height, buttonList, writer);
        this.slot = new GeneralSlot();
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
        // General tab has no interactive buttons
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // GuiSlot handles scrolling via registered scroll buttons
    }

    // ---- Inner GuiSlot ----

    private class GeneralSlot extends GuiSlot {

        GeneralSlot() {
            super(mc, parentScreen.width, parentScreen.height, 22,
                  parentScreen.height - 35, 10);
            setShowSelectionBox(false);
        }

        @Override
        protected int getSize() {
            return StatList.generalStats.size();
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
            return getSize() * 10;
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
            StatBase stat = (StatBase) StatList.generalStats.get(index);
            String name = stat.func_150951_e().getUnformattedText();
            String value = stat.func_75968_a(statFileWriter.writeStat(stat));

            parentScreen.drawString(mc.fontRenderer, name, x + 2, y + 1,
                    index % 2 == 0 ? 16777215 : 9474192);
            parentScreen.drawString(mc.fontRenderer, value,
                    x + 2 + 213 - mc.fontRenderer.getStringWidth(value), y + 1,
                    index % 2 == 0 ? 16777215 : 9474192);
        }
    }
}
