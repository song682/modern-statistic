package decok.dfcdvadstf.modernstatistic.gui.tab;

import decok.dfcdvadstf.catframe.ui.ContentPanelRenderer;
import decok.dfcdvadstf.catframe.ui.components.tab.AbstractScreenTab;
import decok.dfcdvadstf.catframe.ui.navigation.ScreenRectangle;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.stats.StatList;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Tab for general statistics (the vanilla "General" category).
 * <p>
 * Shows a simple list of misc stats like play time, distance walked, etc.
 * </p>
 */
public class StatsGeneralTab extends AbstractScreenTab {

    private StatFileWriter statFileWriter;
    private GeneralSlot slot;

    public StatsGeneralTab() {
        super(105, "stat.generalButton");
    }

    public void initGui(int width, int height, List<GuiButton> buttonList,
            StatFileWriter writer) {
        this.statFileWriter = writer;
        this.slot = new GeneralSlot(width, height);
        this.slot.registerScrollButtons(1, 1);
        setVisible(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!visible || slot == null)
            return;
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

    @Override
    public void keyTyped(char typedChar, int keyCode) {
    }

    @Override
    public void doLayout(ScreenRectangle rectangle) {
        // Fill the content zone (between the header/footer separators) with the slot
        // 用 GuiSlot 填充内容区（Header/Footer 分隔线之间）
        if (slot != null) {
            slot.setBounds(rectangle);
        }
    }

    // ---- Inner GuiSlot ----

    private class GeneralSlot extends GuiSlot {

        GeneralSlot(int width, int height) {
            // An inner-class constructor may not reference enclosing fields implicitly
            // in its super() call, hence the qualified mc reference
            // 内部类构造器的 super() 调用中不能隐式引用外部类字段，故 mc 使用限定引用
            super(StatsGeneralTab.this.mc, width, height, 22, height - 35, 10);
            setShowSelectionBox(false);
        }

        void setBounds(ScreenRectangle rectangle) {
            // GuiSlot keeps width/height fixed at construction time (the screen size),
            // only the visible scroll area bounds are updated by the layout
            // GuiSlot 的 width/height 在构造时固定（屏幕尺寸），布局只更新可见滚动区边界
            this.left = rectangle.left();
            this.top = rectangle.top();
            this.right = rectangle.right();
            this.bottom = rectangle.bottom();
        }

        @Override
        protected int getSize() {
            return StatList.generalStats.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick,
                int mouseX, int mouseY) {
        }

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
            tessellator.addVertexWithUV((double) this.left, (double) this.bottom, 0.0D,
                    (double) ((float) this.left / f1), (double) ((float) (this.bottom + scrolled) / f1));
            tessellator.addVertexWithUV((double) this.right, (double) this.bottom, 0.0D,
                    (double) ((float) this.right / f1), (double) ((float) (this.bottom + scrolled) / f1));
            tessellator.addVertexWithUV((double) this.right, (double) this.top, 0.0D,
                    (double) ((float) this.right / f1), (double) ((float) (this.top + scrolled) / f1));
            tessellator.addVertexWithUV((double) this.left, (double) this.top, 0.0D, (double) ((float) this.left / f1),
                    (double) ((float) (this.top + scrolled) / f1));
            tessellator.draw();
        }

        @Override
        protected void drawBackground() {
            ContentPanelRenderer.drawPanelBackground(0, 24, width, height - 35);
        }

        @Override
        protected void drawSlot(int index, int x, int y, int slotHeight,
                Tessellator tess, int mouseX, int mouseY) {
            StatBase stat = (StatBase) StatList.generalStats.get(index);
            String name = stat.func_150951_e().getUnformattedText();
            String value = stat.func_75968_a(statFileWriter.writeStat(stat));

            // GuiScreen.drawString is protected and the host is not necessarily a GuiStats;
            // drawStringWithShadow is its exact equivalent
            // GuiScreen.drawString 是 protected 且宿主不一定是 GuiStats；drawStringWithShadow 与之完全等价
            mc.fontRenderer.drawStringWithShadow(name, x + 2, y + 1,
                    index % 2 == 0 ? 16777215 : 9474192);
            mc.fontRenderer.drawStringWithShadow(value,
                    x + 2 + 213 - mc.fontRenderer.getStringWidth(value), y + 1,
                    index % 2 == 0 ? 16777215 : 9474192);
        }
    }
}
