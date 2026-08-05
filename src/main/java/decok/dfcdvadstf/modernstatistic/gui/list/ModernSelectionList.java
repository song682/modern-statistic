package decok.dfcdvadstf.modernstatistic.gui.list;

import decok.dfcdvadstf.catframe.ui.components.ContainerObjectSelectionList;
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

    public ModernSelectionList(int width, int height, int y, int itemHeight) {
        super(width, height, y, itemHeight);
    }

    @Override
    protected int scrollBarX() {
        // Pin the scrollbar to the container's right edge (vanilla 1.7.10 look)
        // 滚动条固定在容器右缘（原版 1.7.10 外观）
        return getX() + width - scrollbarWidth();
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
