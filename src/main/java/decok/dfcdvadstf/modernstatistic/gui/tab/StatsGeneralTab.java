package decok.dfcdvadstf.modernstatistic.gui.tab;

import decok.dfcdvadstf.catframe.ui.ContentPanelRenderer;
import decok.dfcdvadstf.catframe.ui.components.Component;
import decok.dfcdvadstf.catframe.ui.components.ContainerObjectSelectionList;
import decok.dfcdvadstf.catframe.ui.components.tab.AbstractScreenTab;
import decok.dfcdvadstf.catframe.ui.navigation.ScreenRectangle;
import decok.dfcdvadstf.modernstatistic.gui.list.ModernSelectionList;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.stats.StatList;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

/**
 * Tab for general statistics (the vanilla "General" category).
 * <p>
 * Shows a simple list of misc stats like play time, distance walked, etc.
 * </p>
 */
public class StatsGeneralTab extends AbstractScreenTab {

    private StatFileWriter statFileWriter;
    private GeneralSelectionList list;

    public StatsGeneralTab() {
        super(105, "stat.generalButton");
    }

    public void initGui(int width, int height, List<GuiButton> buttonList,
            StatFileWriter writer) {
        this.statFileWriter = writer;
        this.list = new GeneralSelectionList(width, height);
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
        // General tab has no interactive buttons
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

    // ---- Inner selection list ----

    private class GeneralSelectionList
            extends ModernSelectionList<GeneralSelectionList.GeneralEntry> {

        /**
         * Screen height at construction time, for the panel background /
         * 构造时的屏幕高度（面板背景用）
         */
        private final int screenHeight;

        GeneralSelectionList(int width, int height) {
            // Row height 10 mirrors the vanilla general-stats slot
            // 行高 10 与原版通用统计槽一致
            super(width, height, 22, 10);
            this.screenHeight = height;
            for (Object obj : StatList.generalStats) {
                addEntry(new GeneralEntry((StatBase) obj));
            }
        }

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
            for (GeneralEntry child : children()) {
                if (child.getY() + child.getHeight() >= getY()
                        && child.getY() <= getBottom()) {
                    child.rowIndex = i;
                    renderItem(child, mouseX, mouseY, partialTicks);
                }
                i++;
            }
        }

        private class GeneralEntry extends ContainerObjectSelectionList.Entry<GeneralEntry> {

            private final StatBase stat;
            /** Visual row index (current order), refreshed each frame / 视觉行号（当前顺序），每帧刷新 */
            private int rowIndex;

            GeneralEntry(StatBase stat) {
                this.stat = stat;
            }

            @Override
            public List<? extends Component> children() {
                // This entry has no child components
                // 本条目没有子组件
                return Collections.emptyList();
            }

            @Override
            public void renderContent(int mouseX, int mouseY, boolean hovered,
                    float partialTicks) {
                String name = stat.func_150951_e().getUnformattedText();
                String value = stat.func_75968_a(statFileWriter.writeStat(stat));
                int color = rowIndex % 2 == 0 ? 16777215 : 9474192;

                // GuiScreen.drawString is protected and the host is not necessarily a GuiStats;
                // drawStringWithShadow is its exact equivalent
                // GuiScreen.drawString 是 protected 且宿主不一定是 GuiStats；drawStringWithShadow 与之完全等价
                minecraft.fontRenderer.drawStringWithShadow(name, getX() + 2,
                        getY() + 1, color);
                minecraft.fontRenderer.drawStringWithShadow(value,
                        getX() + 2 + 213 - minecraft.fontRenderer.getStringWidth(value),
                        getY() + 1, color);
            }
        }
    }
}
