package decok.dfcdvadstf.modernstatistic.gui.betterstats.overlay;

import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.Style;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;
import decok.dfcdvadstf.catframe.ui.overlay.Overlay;
import decok.dfcdvadstf.catframe.ui.overlay.OverlayManager;
import decok.dfcdvadstf.catframe.ui.overlay.ScreenAnchor;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.TElement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Drop-down menu overlay — a full popup menu that opens below a trigger button
 * (a {@link decok.dfcdvadstf.modernstatistic.gui.betterstats.widget.TButtonWidget}
 * or {@link decok.dfcdvadstf.modernstatistic.gui.betterstats.widget.TSelectEnumWidget}).
 * <p>
 * 下拉菜单悬浮层 —— 在触发按钮（{@link decok.dfcdvadstf.modernstatistic.gui.betterstats.widget.TButtonWidget}
 * 或 {@link decok.dfcdvadstf.modernstatistic.gui.betterstats.widget.TSelectEnumWidget}）
 * 下方弹出的完整弹出菜单。
 * <p>
 * Design / 设计要点:
 * <ul>
 *   <li>Items draw <b>no background</b> — hover feedback comes from the
 *       {@link Text} style system (colour change via {@link Style}), so the menu
 *       blends with the screen behind it. / 菜单项<b>无背景</b> —— 悬停反馈由
 *       {@link Text} 样式系统（经 {@link Style} 颜色变化）提供，菜单与背后的界面融为一体。</li>
 *   <li>Each item may carry a left-side icon — an {@link ItemStack} item texture
 *       or a {@link ResourceLocation} texture icon. / 每项可选左侧图标 ——
 *       {@link ItemStack} 物品纹理或 {@link ResourceLocation} 纹理图标。</li>
 *   <li>Horizontal separators group related items. / 水平分隔线用于分组相关菜单项。</li>
 *   <li>Click callbacks follow {@link Style.ClickEvent.Action}: a fixed
 *       {@link Action} enum (OPEN_URL, COPY_TO_CLIPBOARD, ...) plus a value
 *       string, or a custom {@link Runnable} for bespoke behaviour. /
 *       点击回调参照 {@link Style.ClickEvent.Action}：固定的 {@link Action} 枚举
 *       （OPEN_URL、COPY_TO_CLIPBOARD 等）+ 值字符串，或自定义 {@link Runnable}。</li>
 * </ul>
 * <p>
 * Usage / 用法:
 * <pre>{@code
 *   TButtonWidget btn = new TButtonWidget(x, y, w, h, "Menu", button -> {
 *       new DropDownPanel(button)
 *           .addItem(Text.translatable("betterstats.gui.open_wiki"),
 *                    Action.OPEN_URL, "https://example.com")
 *           .addItem(Text.literal("Copy"), Action.COPY_TO_CLIPBOARD, "text")
 *           .addItem(Text.literal("Pin"), () -> togglePin())
 *           .addSeparator()
 *           .show();
 *   });
 *   // Item with an icon:
 *   // 带图标的菜单项：
 *   DropDownPanel.Item it = DropDownPanel.item(Text.literal("Diamond"), () -> pick());
 *   it.icon(new ItemStack(Items.diamond));
 *   panel.addItem(it);
 * }</pre>
 */
public class DropDownPanel extends AbstractComponent implements Overlay {

    // ──── Layout / 布局常量 ────
    private static final int ITEM_HEIGHT = 16;
    private static final int SEPARATOR_HEIGHT = 5;
    private static final int PADDING = 4;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;
    private static final int MIN_WIDTH = 80;
    private static final int MAX_WIDTH = 240;
    private static final int SCREEN_MARGIN = 4;
    private static final int TRIGGER_GAP = 2;

    // ──── Colors / 颜色 ────
    private static final int COLOR_BG = 0xE0000000;
    private static final int COLOR_OUTLINE = 0xFFFFFFFF;
    private static final int COLOR_SEPARATOR = 0xFF666666;
    private static final int COLOR_TEXT = 0xE0E0E0;
    private static final int COLOR_TEXT_DISABLED = 0xA0A0A0;

    /** Default hover style — white text, the same feedback the button widgets use. */
    private static final Style DEFAULT_HOVER_STYLE = Style.EMPTY.withColor(0xFFFFFFFF);

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final RenderItem RENDER_ITEM = new RenderItem();

    // ──── State / 状态 ────
    private final List<MenuItem> items = new ArrayList<>();
    private final int triggerX;
    private final int triggerY;
    private final int triggerHeight;
    private int originX;
    private int originY;

    // ──── Action enum (mirrors Style.ClickEvent.Action) / 动作枚举 ────

    /**
     * Built-in action types for menu items — the same model as
     * {@link Style.ClickEvent.Action}: an action kind plus a string value.
     * {@link #CUSTOM} (or a {@link Runnable} handler) covers bespoke behaviour.
     * <p>
     * 菜单项内置动作类型 —— 与 {@link Style.ClickEvent.Action} 同构：
     * 动作类型 + 字符串值。{@link #CUSTOM}（或 {@link Runnable} 回调）用于自定义行为。
     */
    public enum Action {
        /** Open a URL in the system browser. / 在系统浏览器中打开 URL。 */
        OPEN_URL,
        /** Open a file with the system default application. / 用系统默认程序打开文件。 */
        OPEN_FILE,
        /** Send a chat message (a leading "/" runs it as a command). / 发送聊天消息（"/" 开头视为命令）。 */
        RUN_COMMAND,
        /** Open the chat screen with the value pre-filled. / 打开聊天界面并预填内容。 */
        SUGGEST_COMMAND,
        /** Page switch — no built-in dispatch; use a custom handler. / 页面切换 —— 无内置分发，请使用自定义回调。 */
        CHANGE_PAGE,
        /** Copy the value to the system clipboard. / 将值复制到系统剪贴板。 */
        COPY_TO_CLIPBOARD,
        /** Fully custom behaviour via the item's handler. / 完全自定义行为（走菜单项的回调）。 */
        CUSTOM
    }

    // ──── Constructors / 构造器 ────

    /**
     * Create a drop-down panel anchored below the given trigger rectangle
     * (the button's bounds). The popup is clamped to the screen edges.
     * <p>
     * 创建锚定在给定触发矩形（按钮边界）下方的下拉面板。弹窗会向屏幕边缘钳制。
     *
     * @param triggerX      trigger button X / 触发按钮 X
     * @param triggerY      trigger button Y / 触发按钮 Y
     * @param triggerWidth  trigger button width / 触发按钮宽度
     * @param triggerHeight trigger button height / 触发按钮高度
     */
    public DropDownPanel(int triggerX, int triggerY, int triggerWidth, int triggerHeight) {
        this.triggerX = triggerX;
        this.triggerY = triggerY;
        // triggerWidth keeps the button-rectangle semantics of the signature;
        // the panel's own width is derived from its items and clamped instead.
        // triggerWidth 保留签名的按钮矩形语义；面板宽度由菜单项推导并钳制。
        this.triggerHeight = triggerHeight;
        setVisible(false);
        layout();
    }

    /**
     * Convenience constructor for any {@link TElement} trigger (covers both
     * {@link decok.dfcdvadstf.modernstatistic.gui.betterstats.widget.TButtonWidget}
     * and {@link decok.dfcdvadstf.modernstatistic.gui.betterstats.widget.TSelectEnumWidget}).
     * <p>
     * 任意 {@link TElement} 触发器的便捷构造器（同时覆盖
     * {@link decok.dfcdvadstf.modernstatistic.gui.betterstats.widget.TButtonWidget}
     * 与 {@link decok.dfcdvadstf.modernstatistic.gui.betterstats.widget.TSelectEnumWidget}）。
     */
    public DropDownPanel(TElement trigger) {
        this(trigger.getX(), trigger.getY(), trigger.getWidth(), trigger.getHeight());
    }

    // ──── Item factories / 菜单项工厂 ────

    /** Create an item bound to a built-in {@link Action} plus its value. */
    public static MenuItem item(Text label, Action action, String value) {
        return new MenuItem(label, action, value, null, true);
    }

    /** Create an item bound to a built-in {@link Action} plus its value. */
    public static MenuItem item(Text label, Action action, String value, boolean enabled) {
        return new MenuItem(label, action, value, null, enabled);
    }

    /** Create an item with a fully custom callback. / 创建带完全自定义回调的菜单项。 */
    public static MenuItem item(Text label, Runnable handler) {
        return new MenuItem(label, Action.CUSTOM, null, handler, true);
    }

    /** Create an item with a fully custom callback. / 创建带完全自定义回调的菜单项。 */
    public static MenuItem item(Text label, Runnable handler, boolean enabled) {
        return new MenuItem(label, Action.CUSTOM, null, handler, enabled);
    }

    // ──── Building / 构建 ────

    /** Append a menu item and re-layout. / 追加菜单项并重新布局。 */
    public DropDownPanel addItem(MenuItem item) {
        items.add(item);
        layout();
        return this;
    }

    /** Append a menu item bound to a built-in {@link Action}. */
    public DropDownPanel addItem(Text label, Action action, String value) {
        return addItem(item(label, action, value));
    }

    /** Append a menu item bound to a built-in {@link Action}. */
    public DropDownPanel addItem(Text label, Action action, String value, boolean enabled) {
        return addItem(item(label, action, value, enabled));
    }

    /** Append a menu item with a fully custom callback. */
    public DropDownPanel addItem(Text label, Runnable handler) {
        return addItem(item(label, handler));
    }

    /** Append a menu item with a fully custom callback. */
    public DropDownPanel addItem(Text label, Runnable handler, boolean enabled) {
        return addItem(item(label, handler, enabled));
    }

    /** Append a horizontal separator line. / 追加水平分隔线。 */
    public DropDownPanel addSeparator() {
        items.add(MenuItem.separator());
        layout();
        return this;
    }

    // ──── Show / Close ────

    /**
     * Register with the {@link OverlayManager} and show the menu.
     * <p>注册到 {@link OverlayManager} 并显示菜单。</p>
     */
    public DropDownPanel show() {
        layout();
        setVisible(true);
        OverlayManager.INSTANCE.register(this);
        return this;
    }

    /**
     * Hide and unregister the menu. Also triggered by any click outside an
     * item and by Esc. / 隐藏并注销菜单。点击非菜单项区域或按 Esc 也会触发。
     */
    public void close() {
        setVisible(false);
        OverlayManager.INSTANCE.unregister(this);
    }

    // ──── Overlay ────

    @Override
    public ScreenAnchor getAnchor() {
        return ScreenAnchor.TOP_LEFT;
    }

    @Override
    public int getOffsetX() {
        return originX;
    }

    @Override
    public int getOffsetY() {
        return originY;
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    /**
     * Capture ALL clicks while visible, so clicking outside the panel also
     * closes it. / 捕获所有点击，点击面板外部同样关闭菜单。
     */
    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return true;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        // Esc closes the menu (host screens that forward keys to overlays).
        // Esc 关闭菜单（宿主界面将按键转发给 Overlay 时生效）。
        if (keyCode == 1) {
            close();
        }
    }

    // ──── Layout / 布局 ────

    /**
     * Recompute the panel size from its items and re-clamp the position below
     * the trigger button to the screen bounds.
     * <p>
     * 根据菜单项重算面板尺寸，并将触发按钮下方的位置重新钳制到屏幕边界。
     */
    private void layout() {
        int h = PADDING * 2;
        int w = MIN_WIDTH;
        FontRenderer fr = TElement.getFontRenderer();
        for (MenuItem item : items) {
            if (item.isSeparator()) {
                h += SEPARATOR_HEIGHT;
                continue;
            }
            h += ITEM_HEIGHT;
            int textW = fr.getStringWidth(item.label.getString());
            w = Math.max(w, textW + PADDING * 2 + (item.hasIcon() ? ICON_SIZE + ICON_GAP : 0));
        }
        w = Math.min(w, MAX_WIDTH);
        setSize(w, h);

        // Position below the trigger, clamped on all four screen edges.
        // 定位在触发按钮下方，并向屏幕四边钳制。
        ScaledResolution sr = new ScaledResolution(MC, MC.displayWidth, MC.displayHeight);
        int screenW = sr.getScaledWidth();
        int screenH = sr.getScaledHeight();
        int px = triggerX;
        int py = triggerY + triggerHeight + TRIGGER_GAP;
        if (px + w > screenW - SCREEN_MARGIN) px = screenW - w - SCREEN_MARGIN;
        if (py + h > screenH - SCREEN_MARGIN) py = triggerY - h - TRIGGER_GAP;
        this.originX = Math.max(SCREEN_MARGIN, px);
        this.originY = Math.max(SCREEN_MARGIN, py);
    }

    // ──── Render / 渲染 ────

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        // Panel background + outline / 面板背景 + 描边
        TElement.fill(originX, originY, originX + width, originY + height, COLOR_BG);
        TElement.drawOutline(originX, originY, originX + width, originY + height, COLOR_OUTLINE);

        int cy = originY + PADDING;
        for (MenuItem item : items) {
            if (item.isSeparator()) {
                // Horizontal separator / 水平分隔线
                int sy = cy + (SEPARATOR_HEIGHT - 1) / 2;
                TElement.fill(originX + PADDING, sy, originX + width - PADDING, sy + 1, COLOR_SEPARATOR);
                cy += SEPARATOR_HEIGHT;
                continue;
            }

            boolean hovered = item.enabled
                    && mouseX >= originX && mouseX < originX + width
                    && mouseY >= cy && mouseY < cy + ITEM_HEIGHT;

            // Left-side icon / 左侧图标
            int textX = originX + PADDING;
            if (item.hasIcon()) {
                int iconY = cy + (ITEM_HEIGHT - ICON_SIZE) / 2;
                renderIcon(item, textX, iconY);
                textX += ICON_SIZE + ICON_GAP;
            }

            // Label — no background; hover feedback via the Text style system.
            // 文本 —— 无背景；悬停反馈由 Text 样式系统提供。
            Text label = item.label;
            if (hovered) {
                label = label.withStyleApplied(item.hoverStyle != null ? item.hoverStyle : DEFAULT_HOVER_STYLE);
            }
            String str = label.getFormattedString();
            int color = item.enabled ? COLOR_TEXT : COLOR_TEXT_DISABLED;
            TElement.getFontRenderer().drawStringWithShadow(str, textX, cy + (ITEM_HEIGHT - 9) / 2, color);

            cy += ITEM_HEIGHT;
        }
    }

    /** Render the item's icon: ItemStack first, then texture icon. */
    private static void renderIcon(MenuItem item, int iconX, int iconY) {
        if (item.icon != null) {
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.enableGUIStandardItemLighting();
            RENDER_ITEM.renderItemIntoGUI(TElement.getFontRenderer(), MC.getTextureManager(),
                    item.icon, iconX, iconY);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        } else if (item.iconTexture != null) {
            // Full-extent quad with 0..1 UVs — the texture is drawn stretched
            // into the 16x16 slot regardless of its intrinsic size.
            // 0..1 UV 全幅四边形 —— 无论纹理原始尺寸如何都拉伸进 16x16 槽位。
            MC.getTextureManager().bindTexture(item.iconTexture);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(iconX, iconY + ICON_SIZE, 0, 0.0D, 1.0D);
            tessellator.addVertexWithUV(iconX + ICON_SIZE, iconY + ICON_SIZE, 0, 1.0D, 1.0D);
            tessellator.addVertexWithUV(iconX + ICON_SIZE, iconY, 0, 1.0D, 0.0D);
            tessellator.addVertexWithUV(iconX, iconY, 0, 0.0D, 0.0D);
            tessellator.draw();
        }
    }

    // ──── Input / 输入 ────

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!visible) return;

        if (mouseButton == 0) {
            int index = hitItem(mouseX, mouseY);
            if (index >= 0) {
                MenuItem item = items.get(index);
                if (item.enabled) {
                    execute(item);
                }
            }
        }
        // Any click closes the menu / 任何点击都会关闭菜单
        close();
    }

    /** Resolve the item index under the cursor, or -1 (separators skipped). */
    private int hitItem(int mouseX, int mouseY) {
        if (mouseX < originX || mouseX >= originX + width) return -1;
        int cy = originY + PADDING;
        for (int i = 0; i < items.size(); i++) {
            MenuItem item = items.get(i);
            int itemH = item.isSeparator() ? SEPARATOR_HEIGHT : ITEM_HEIGHT;
            if (mouseY >= cy && mouseY < cy + itemH) {
                return item.isSeparator() ? -1 : i;
            }
            cy += itemH;
        }
        return -1;
    }

    /**
     * Run the item's callback: a custom handler wins; otherwise the
     * {@link Action} enum is dispatched (the {@link Style.ClickEvent.Action}
     * processing model). / 执行菜单项回调：自定义回调优先；否则按
     * {@link Action} 枚举分发（{@link Style.ClickEvent.Action} 处理模型）。
     */
    private static void execute(MenuItem item) {
        if (item.handler != null) {
            item.handler.run();
            return;
        }
        switch (item.action) {
            case OPEN_URL:
                try {
                    Desktop.getDesktop().browse(java.net.URI.create(item.value));
                } catch (Exception ignored) {
                }
                break;
            case OPEN_FILE:
                try {
                    Desktop.getDesktop().open(new File(item.value));
                } catch (Exception ignored) {
                }
                break;
            case RUN_COMMAND:
                if (MC.thePlayer != null) {
                    MC.thePlayer.sendChatMessage(item.value);
                }
                break;
            case SUGGEST_COMMAND:
                MC.displayGuiScreen(new GuiChat(item.value));
                break;
            case COPY_TO_CLIPBOARD:
                try {
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(new StringSelection(item.value), null);
                } catch (Exception ignored) {
                }
                break;
            case CHANGE_PAGE:
                // No generic page concept — pair with a custom handler.
                // 无通用页面概念 —— 请配合自定义回调使用。
                break;
            case CUSTOM:
            default:
                break;
        }
    }

    // ──── Menu item / 菜单项 ────

    /**
     * A single menu entry: label ({@link Text}, so styles and translation
     * keys work), optional icon, optional separator, and a click callback
     * following the {@link Style.ClickEvent.Action} model.
     * <p>
     * 单个菜单条目：标签（{@link Text}，支持样式与翻译键）、可选图标、
     * 可选分隔线，以及遵循 {@link Style.ClickEvent.Action} 模型的点击回调。
     */
    public static final class MenuItem {

        private final Text label;
        private final Action action;
        private final String value;
        private final Runnable handler;
        private final boolean enabled;
        private final boolean separator;
        private ItemStack icon;
        private ResourceLocation iconTexture;
        private Style hoverStyle;

        private MenuItem(Text label, Action action, String value, Runnable handler, boolean enabled) {
            this.label = label;
            this.action = action;
            this.value = value;
            this.handler = handler;
            this.enabled = enabled;
            this.separator = false;
        }

        private MenuItem(boolean separator) {
            this.label = null;
            this.action = null;
            this.value = null;
            this.handler = null;
            this.enabled = false;
            this.separator = true;
        }

        private static MenuItem separator() {
            return new MenuItem(true);
        }

        /** Whether this entry is a separator line. / 是否为分隔线。 */
        public boolean isSeparator() {
            return separator;
        }

        /** Whether this entry renders an icon. / 是否渲染图标。 */
        public boolean hasIcon() {
            return icon != null || iconTexture != null;
        }

        /** Set an item-texture icon (16x16). / 设置物品纹理图标（16x16）。 */
        public MenuItem icon(ItemStack icon) {
            this.icon = icon;
            this.iconTexture = null;
            return this;
        }

        /** Set a texture icon (16x16). / 设置纹理图标（16x16）。 */
        public MenuItem icon(ResourceLocation iconTexture) {
            this.iconTexture = iconTexture;
            this.icon = null;
            return this;
        }

        /** Override the hover style; defaults to white text. / 覆盖悬停样式；默认白色文本。 */
        public MenuItem hoverStyle(Style hoverStyle) {
            this.hoverStyle = hoverStyle;
            return this;
        }
    }
}
