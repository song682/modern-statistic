package decok.dfcdvadstf.modernstatistic.gui.tab;

import decok.dfcdvadstf.catframe.ui.components.ContainerObjectSelectionList;
import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;
import decok.dfcdvadstf.catframe.ui.components.tab.AbstractScreenTab;
import decok.dfcdvadstf.catframe.ui.navigation.ScreenRectangle;
import decok.dfcdvadstf.modernstatistic.gui.list.ModernSelectionList;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityList;
import net.minecraft.stats.StatFileWriter;

import java.util.Collections;
import java.util.List;

/**
 * Tab for mob/entity statistics — kills and deaths per entity type.
 */
public class StatsMobsTab extends AbstractScreenTab {

    private StatFileWriter statFileWriter;
    private MobsSelectionList list;

    public StatsMobsTab() {
        super(107, "stat.mobsButton");
    }

    public void initGui(int width, int height, List<GuiButton> buttonList,
            StatFileWriter writer) {
        this.statFileWriter = writer;
        this.list = new MobsSelectionList(width, height);
        setVisible(false);
    }

    /**
     * The list component, registered as a render-only component by
     * {@code GuiStatics.initTabs}; input events (clicks, scroll wheel, drags)
     * are forwarded to the visible list explicitly by {@code GuiStatics}.
     * <p>
     * 列表组件，由 {@code GuiStatics.initTabs} 注册为仅渲染组件；
     * 输入事件（点击 / 滚轮 / 拖动）由 {@code GuiStatics} 显式转发给可见列表。
     * </p>
     */
    public GuiEventListener getList() {
        return list;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // The list is rendered by the screen's renderable pipeline (addRenderableOnly)
        // 列表由界面的渲染管线渲染（addRenderableOnly）
    }

    @Override
    public void actionPerformed(GuiButton button) {
        // No interactive buttons in this tab
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // Mouse events are forwarded to the visible list explicitly by GuiStatics
        // 鼠标事件由 GuiStatics 显式转发给可见列表
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
            list.layoutInContentZone(rectangle.left(), rectangle.top(),
                    rectangle.width, rectangle.bottom(), 0);
        }
    }

    /** @return true if the mob list is empty (used to disable the tab button) */
    public boolean isEmpty() {
        return list == null || list.size() == 0;
    }

    // ---- Inner selection list ----

    private class MobsSelectionList
            extends ModernSelectionList<MobsSelectionList.MobsEntry> {

        MobsSelectionList(int width, int height) {
            // Row height = 4 lines of text (name + two stat lines with spacing),
            // mirroring the vanilla mobs slot
            // 行高 = 4 行文本（名称 + 两行统计并留空行），与原版生物槽一致
            super(width, height, 22,
                    StatsMobsTab.this.mc.fontRenderer.FONT_HEIGHT * 4);

            for (Object obj : EntityList.entityEggs.values()) {
                EntityList.EntityEggInfo info = (EntityList.EntityEggInfo) obj;
                if (statFileWriter.writeStat(info.field_151512_d) > 0
                        || statFileWriter.writeStat(info.field_151513_e) > 0) {
                    addEntry(new MobsEntry(info));
                }
            }
        }

        private class MobsEntry extends ContainerObjectSelectionList.Entry<MobsEntry> {

            private final EntityList.EntityEggInfo info;

            MobsEntry(EntityList.EntityEggInfo info) {
                this.info = info;
            }

            @Override
            public List<? extends GuiEventListener> children() {
                // This entry has no child components
                // 本条目没有子组件
                return Collections.emptyList();
            }

            @Override
            public void renderContent(int mouseX, int mouseY, boolean hovered,
                    float partialTicks) {
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

                int fh = minecraft.fontRenderer.FONT_HEIGHT;
                // GuiScreen.drawString is protected and the host is not necessarily a GuiStats;
                // drawStringWithShadow is its exact equivalent
                // GuiScreen.drawString 是 protected 且宿主不一定是 GuiStats；drawStringWithShadow 与之完全等价
                minecraft.fontRenderer.drawStringWithShadow(name,
                        getX() + 2 - 10, getY() + 1, 16777215);
                minecraft.fontRenderer.drawStringWithShadow(killsText, getX() + 2,
                        getY() + 1 + fh, kills == 0 ? 6316128 : 9474192);
                minecraft.fontRenderer.drawStringWithShadow(killedByText, getX() + 2,
                        getY() + 1 + fh * 2, killedBy == 0 ? 6316128 : 9474192);
            }
        }
    }
}
