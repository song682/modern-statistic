package decok.dfcdvadstf.modernstatistic.gui.list;

import decok.dfcdvadstf.catframe.ui.ContentPanelRenderer;
import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;

/**
 * <p>
 * Shared selection-list base for the statistics tabs, fixing two CatFrame
 * scrollbar problems that cannot be patched in the framework jar itself:
 * </p>
 * <ul>
 * <li>{@code AbstractScrollArea.mouseDrag} adds the <em>absolute</em> mouse Y to
 * the scroll amount on every drag frame, so a single press slams the scroller to
 * the bottom and it can no longer be controlled; here the drag is recomputed from
 * the click-start state (proper delta tracking) and clicking anywhere on the track
 * jumps the thumb there first (modern track-click behaviour).</li>
 * <li>{@code AbstractSelectionList.scrollBarX} places the scrollbar right after the
 * (centered) row content, which lands in the middle of the screen for narrow lists;
 * here it is pinned to the container's right edge, matching the vanilla 1.7.10
 * slots.</li>
 * <li>{@code AbstractScrollArea.enableScissor} expresses the clip box in window
 * pixels without the GUI scale, cutting off most of the list content on scales
 * &gt; 1; here the scale is derived from {@code displayWidth / width}, matching the
 * high-version vanilla ScissorTest.</li>
 * <li>{@link #layoutInContentZone} anchors the list in the content zone between
 * the header/footer separators and the shared {@link #renderBackground} fills
 * exactly that zone, keeping the middle of the screen fully painted. Tabs that
 * need a column header add it as the first scrollable entry (vanilla
 * HeaderEntry style), which sidesteps the framework pinning the first entry at
 * {@code getY() + 2} underneath a drawn-on header.</li>
 * </ul>
 *
 * <p>
 * 统计标签页共用的选择列表基类，用于修复无法在框架 jar 内修复的两个 CatFrame
 * 滚动条问题：
 * </p>
 * <ul>
 * <li>{@code AbstractScrollArea.mouseDrag} 每帧把<em>绝对</em>鼠标 Y 累加进滚动量，
 * 按下一次就立刻滚到底、之后无法控制；这里改为基于点击起始状态重算（真正的增量拖动），
 * 且点击轨道任意位置会先把滑块定位过去（现代轨道点击行为）。</li>
 * <li>{@code AbstractSelectionList.scrollBarX} 把滚动条放在行内容右缘之后，
 * 对居中列表来说会落在屏幕中间；这里固定到容器右缘，与原版 1.7.10 槽一致。</li>
 * <li>{@code AbstractScrollArea.enableScissor} 用窗口像素表达裁剪框却忘了乘以
 * GUI 缩放，缩放 &gt; 1 时列表内容大部分被裁掉；这里按
 * {@code displayWidth / width} 换算缩放，与高版本原版 ScissorTest 一致。</li>
 * <li>{@link #layoutInContentZone} 把列表锚定在 Header/Footer 分隔线之间的内容区，
 * 共用的 {@link #renderBackground} 恰好填满该区域，保证屏幕中间部分被完整填充。
 * 需要列表头的标签页将其作为第一个可滚动条目（原版 HeaderEntry 做法），
 * 避开框架把首个条目固定在 {@code getY() + 2} 而被绘制出来的表头盖住的问题。</li>
 * </ul>
 */
public abstract class ModernSelectionList<E extends ContainerObjectSelectionList.Entry<E>>
        extends ContainerObjectSelectionList<E> {

    /** true while the user is dragging the scrollbar thumb / 用户正在拖动滚动条滑块 */
    private boolean dragScrolling;
    /** scroll amount captured at drag start / 拖动开始时的滚动量 */
    private double dragStartScroll;
    /** mouse Y captured at drag start / 拖动开始时的鼠标 Y */
    private int dragStartMouseY;
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
    protected int scrollBarX() {
        // Pin the scrollbar to the container's right edge (vanilla 1.7.10 look)
        // 滚动条固定在容器右缘（原版 1.7.10 外观）
        return getX() + width - scrollbarWidth();
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
        // Scale the box here, like the high-version vanilla ScissorTest.
        // GL 裁剪以窗口像素表达，而列表几何是缩放后的 GUI 坐标——框架版本忘了乘
        // GUI 缩放，缩放 > 1 时裁剪框错位、内容被裁。这里按缩放修正。
        int guiWidth = minecraft.currentScreen != null ? minecraft.currentScreen.width : width;
        // displayWidth / guiWidth == the GUI scale (guiWidth = displayWidth / scale)
        // GUI 缩放 = displayWidth / guiWidth（guiWidth 即缩放后的 GUI 宽）
        int scale = guiWidth > 0 ? Math.max(1, minecraft.displayWidth / guiWidth) : 1;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(getX() * scale,
                minecraft.displayHeight - (getY() + getHeight()) * scale,
                getWidth() * scale, getHeight() * scale);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        // Start thumb dragging on any track click: jump the thumb to the clicked
        // position first, then track deltas from there
        // 点击轨道任意位置开始拖动：先把滑块定位到点击处，再从该处跟踪增量
        if (mouseButton == 0 && isOverScrollbar(mouseX, mouseY) && scrollable()) {
            double max = Math.max(1, maxScrollAmount());
            double barH = scrollerHeight();
            double scale = max / (this.height - barH);
            setScrollAmount((mouseY - this.y - barH / 2.0) * scale);
            dragStartScroll = scrollAmount();
            dragStartMouseY = mouseY;
            dragScrolling = true;
        }
    }

    @Override
    public void mouseDrag(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
        // Do NOT call super: the framework formula adds the absolute mouse Y to the
        // scroll amount every frame, making the scroller uncontrollable
        // 不调用 super：框架公式每帧把绝对鼠标 Y 累加进滚动量，滑块会失控
        if (dragScrolling && mouseButton == 0) {
            double max = Math.max(1, maxScrollAmount());
            double barH = scrollerHeight();
            double scale = max / (this.height - barH);
            setScrollAmount(dragStartScroll + (mouseY - dragStartMouseY) * scale);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        dragScrolling = false;
    }

    /**
     * @return the number of entries / 条目数量
     */
    public int size() {
        return getItemCount();
    }
}
