package decok.dfcdvadstf.modernstatistic.gui.list;

import decok.dfcdvadstf.catframe.ui.ContentPanelRenderer;
import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;

/**
 * <p>
 * Shared selection-list base for the statistics tabs, fixing the remaining
 * CatFrame problems that cannot be patched in the framework jar itself (the
 * scrollbar drag and position bugs were fixed upstream in CatFrame 0.7.1.1,
 * so their workarounds were removed here):
 * </p>
 * <ul>
 * <li>{@code AbstractScrollArea.enableScissor} expresses the clip box in window
 * pixels without the GUI scale, cutting off most of the list content on scales
 * &gt; 1; here the scale is taken from the vanilla {@link ScaledResolution} (the
 * naive {@code displayWidth / width} division truncates, e.g. 2560/427 gives 5
 * instead of 6, which clips the bottom rows of the list into a blank band).</li>
 * <li>{@link #layoutInContentZone} anchors the list in the content zone between
 * the header/footer separators and the shared {@link #renderBackground} fills
 * exactly that zone, keeping the middle of the screen fully painted. Tabs that
 * need a column header add it as the first scrollable entry (vanilla
 * HeaderEntry style), which sidesteps the framework pinning the first entry at
 * {@code getY() + 2} underneath a drawn-on header.</li>
 * </ul>
 *
 * <p>
 * 统计标签页共用的选择列表基类，用于修复无法在框架 jar 内修复的剩余 CatFrame
 * 问题（滚动条拖动与位置问题已在 CatFrame 0.7.1.1 上游修复，相应的本地
 * 规避代码已移除）：
 * </p>
 * <ul>
 * <li>{@code AbstractScrollArea.enableScissor} 用窗口像素表达裁剪框却忘了乘以
 * GUI 缩放，缩放 &gt; 1 时列表内容大部分被裁掉；这里改用原版
 * {@link ScaledResolution} 的缩放系数（直接的 {@code displayWidth / width}
 * 整数除法会截断，例如 2560/427 得 5 而非 6，会把列表底部若干行裁成
 * 一条空白带）。</li>
 * <li>{@link #layoutInContentZone} 把列表锚定在 Header/Footer 分隔线之间的内容区，
 * 共用的 {@link #renderBackground} 恰好填满该区域，保证屏幕中间部分被完整填充。
 * 需要列表头的标签页将其作为第一个可滚动条目（原版 HeaderEntry 做法），
 * 避开框架把首个条目固定在 {@code getY() + 2} 而被绘制出来的表头盖住的问题。</li>
 * </ul>
 */
public abstract class ModernSelectionList<E extends ContainerObjectSelectionList.Entry<E>>
        extends ContainerObjectSelectionList<E> {

    /**
     * Top of the content zone (header separator Y). The scrollable component may
     * start below it when a header band is reserved, but the background always
     * fills from here down to the footer separator.
     * 内容区顶部（Header 分隔线 Y）。预留表头带时可滚动组件从其下方开始，
     * 但背景始终从这里填充到 Footer 分隔线。
     */
    private int zoneTop;

    public ModernSelectionList(int width, int height, int y, int itemHeight) {
        super(width, height, y, itemHeight);
        this.zoneTop = y;
    }

    /**
     * Lay the list out inside the content zone between the header/footer
     * separators: the scrollable component (rows, scissor, scrollbar) starts
     * below the reserved header band, while {@link #zoneTop} keeps the shared
     * background filling the zone completely.
     * <p>
     * 在 Header/Footer 分隔线之间的内容区内布局列表：可滚动组件（行、裁剪、
     * 滚动条）从预留表头带之下开始，而 {@link #zoneTop} 保证共用背景完整
     * 填满内容区。
     * </p>
     *
     * @param x            content zone left / 内容区左缘
     * @param zoneTop      content zone top (header separator Y) / 内容区顶部
     * @param width        content zone width / 内容区宽度
     * @param zoneBottom   content zone bottom (footer separator Y) / 内容区底部
     * @param headerHeight fixed header band height to reserve / 预留的固定表头带高度
     */
    public void layoutInContentZone(int x, int zoneTop, int width, int zoneBottom,
            int headerHeight) {
        this.zoneTop = zoneTop;
        updateSizeAndPosition(width,
                Math.max(0, zoneBottom - zoneTop - headerHeight),
                x, zoneTop + headerHeight);
    }

    /** @return top of the content zone the list fills / 列表填充的内容区顶部 */
    public int getZoneTop() {
        return zoneTop;
    }

    @Override
    protected void renderBackground(GuiGraphicsExtractor graphics, int mouseX,
            int mouseY, float partialTicks) {
        // Panel background between the separators + scrolling dark tile over the
        // whole content zone (header band included), mirroring the vanilla slot's
        // drawBackground/drawContainerBackground pair. Filling from zoneTop (not
        // getY()) keeps the middle of the screen painted even when the scrollable
        // component starts below a header band.
        // 分隔线之间的面板背景 + 覆盖整个内容区（含表头带）的滚动深色平铺纹理，
        // 与原版槽的 drawBackground/drawContainerBackground 组合一致。从 zoneTop
        // （而非 getY()）开始填充，保证预留表头带时屏幕中间仍被完整填充。
        ContentPanelRenderer.drawPanelBackground(getX(), zoneTop + 2, getWidth(),
                getBottom() - zoneTop - 2);
        minecraft.getTextureManager().bindTexture(Gui.optionsBackground);
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
        tess.addVertexWithUV((double) getRight(), (double) zoneTop, 0.0D,
                (double) ((float) getRight() / f1),
                (double) ((float) (zoneTop + scrolled) / f1));
        tess.addVertexWithUV((double) getX(), (double) zoneTop, 0.0D,
                (double) ((float) getX() / f1),
                (double) ((float) (zoneTop + scrolled) / f1));
        tess.draw();
    }

    @Override
    protected void enableScissor() {
        // GL scissor is in window pixels but the list geometry is in scaled GUI
        // coordinates — the framework version forgets the GUI scale, so on scales
        // > 1 the clip box lands in the wrong place and the content is cut off.
        // Take the exact vanilla scale factor from ScaledResolution; a plain
        // displayWidth / width division truncates (2560/427 -> 5, not 6) and
        // would clip the bottom rows of the list into a blank band.
        // GL 裁剪以窗口像素表达，而列表几何是缩放后的 GUI 坐标——框架版本忘了乘
        // GUI 缩放，缩放 > 1 时裁剪框错位、内容被裁。这里用 ScaledResolution 的
        // 精确缩放系数；直接 displayWidth / width 会截断（2560/427 得 5 而非 6），
        // 把列表底部若干行裁成空白带。
        int scale = new ScaledResolution(minecraft, minecraft.displayWidth,
                minecraft.displayHeight).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(getX() * scale,
                minecraft.displayHeight - (getY() + getHeight()) * scale,
                getWidth() * scale, getHeight() * scale);
    }

    /**
     * @return the number of entries / 条目数量
     */
    public int size() {
        return getItemCount();
    }
}
