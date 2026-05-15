package decok.dfcdvadstf.modernstatistic.tab;

import decok.dfcdvadstf.createworldui.api.tab.AbstractScreenTab;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.stats.StatFileWriter;

import java.util.List;

/**
 * <p>Bridge between {@link AbstractScreenTab} and {@link GuiStats}.</p>
 * <p>Since stats tabs don't need a full-blown {@code TabManager} (no world-creation state),
 * this base holds a direct reference to the parent {@code GuiStats} and manages button
 * addition without delegating to a tab manager.</p>
 *
 * <p>We pass the button list and stat writer explicitly during {@code initGui} to avoid
 * compile-time dependency on methods that are only added later via Mixin.</p>
 *
 * <p>{@link AbstractScreenTab} 与 {@link GuiStats} 之间的桥接。</p>
 * <p>通过显式传参 buttonList 和 StatFileWriter 来避免编译时依赖 Mixin 才注入的方法。</p>
 */
public abstract class StatsTab extends AbstractScreenTab {

    protected GuiStats parentScreen;
    protected StatFileWriter statFileWriter;
    protected List<GuiButton> screenButtonList;

    public StatsTab(int tabId, String tabNameKey) {
        super(tabId, tabNameKey);
    }

    /**
     * Initialize this tab with the parent GuiStats context and shared resources.
     * Does NOT call {@code super.initGui(TabManager, ...)} — we manage buttons ourselves.
     */
    public void initGui(GuiStats parent, int width, int height,
                        List<GuiButton> buttonList, StatFileWriter writer) {
        this.parentScreen = parent;
        this.statFileWriter = writer;
        this.screenButtonList = buttonList;
        this.tabButtons.clear();
    }

    @Override
    protected void addButton(GuiButton button) {
        tabButtons.add(button);
        if (screenButtonList != null) {
            screenButtonList.add(button);
        }
    }

    // ---- Override world-state getters from AbstractScreenTab to avoid NPE ----
    @Override protected String  getWorldName()          { return ""; }
    @Override protected String  getGameMode()            { return ""; }
    @Override protected String  getSeed()                { return ""; }
    @Override protected int     getWorldTypeIndex()      { return 0; }
    @Override protected boolean getGenerateStructures()  { return false; }
    @Override protected boolean getBonusChest()          { return false; }
    @Override protected boolean getAllowCheats()         { return false; }
    @Override protected boolean getHardcore()            { return false; }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // Default: no-op. Tabs that need mouse handling override this.
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        // Default: no-op. Tabs that need keyboard handling override this.
    }
}
