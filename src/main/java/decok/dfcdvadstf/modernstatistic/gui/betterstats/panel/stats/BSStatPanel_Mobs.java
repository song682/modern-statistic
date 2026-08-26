package decok.dfcdvadstf.modernstatistic.gui.betterstats.panel.stats;

import java.util.*;

import decok.dfcdvadstf.catframe.ui.components.Tooltip;
import decok.dfcdvadstf.catframe.ui.components.WidgetTooltipHolder;
import decok.dfcdvadstf.catframe.ui.navigation.ScreenRectangle;
import decok.dfcdvadstf.modernstatistic.ModernStatistic;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.TElement;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.panel.BSPanel;
import decok.dfcdvadstf.modernstatistic.gui.TBetterStatsScreen;
import decok.dfcdvadstf.modernstatistic.render.EntityModelRenderer;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;

/**
 * Mob/entity statistics panel — grid layout grouped by mod.
 * <p>50x50 cells showing entity kills and deaths.</p>
 */
public class BSStatPanel_Mobs extends BSStatPanel {

    protected static final int CELL_SIZE = 50;

    protected final List<MobStatEntry> entries = new ArrayList<>();

    public BSStatPanel_Mobs(BSPanel parentToFill, TBetterStatsScreen screen) {
        super(parentToFill, screen);
    }

    // ==================== Data ====================

    protected static class MobStatEntry {
        final EntityList.EntityEggInfo eggInfo;
        final String name;
        int kills, killedBy;

        MobStatEntry(EntityList.EntityEggInfo eggInfo) {
            this.eggInfo = eggInfo;
            this.name = I18n.format("entity." + EntityList.getStringFromID(eggInfo.spawnedID) + ".name");
        }

        boolean isEmpty() { return kills == 0 && killedBy == 0; }
    }

    // ==================== Init ====================

    @Override
    public void init() {
        clearChildren();
        entries.clear();

        // Collect mob entries with stats
        for (Object obj : EntityList.entityEggs.values()) {
            EntityList.EntityEggInfo info = (EntityList.EntityEggInfo) obj;
            int kills = screen.getStatFileWriter().writeStat(info.field_151512_d);
            int killedBy = screen.getStatFileWriter().writeStat(info.field_151513_e);

            if (!screen.isShowEmptyStats() && kills == 0 && killedBy == 0) continue;

            MobStatEntry entry = new MobStatEntry(info);
            entry.kills = kills;
            entry.killedBy = killedBy;
            entries.add(entry);
        }

        // Apply search filter
        String search = screen.getSearchTerm().toLowerCase().trim();
        if (!search.isEmpty()) {
            entries.removeIf(e -> !e.name.toLowerCase().contains(search));
        }

        // Apply entity type filter (subclasses override)
        filterEntries();

        if (entries.isEmpty()) {
            showNoResults();
            return;
        }

        // Grid layout
        int sp = getScrollPadding();
        int nextX = getX() + sp;
        int nextY = getY() + sp;

        for (MobStatEntry entry : entries) {
            MobStatWidget widget = new MobStatWidget(entry, nextX, nextY, CELL_SIZE);
            addChild(widget, false);

            nextX += CELL_SIZE + 2;
            if (nextX + CELL_SIZE > getEndX() - sp) {
                nextX = getX() + sp;
                nextY += CELL_SIZE + 2;
            }
        }

        recalculateContentHeight();
    }

    /** Override in subclasses to filter entries by type. */
    protected void filterEntries() {
        // Default: no additional filtering
    }

    // ==================== Widget ====================

    protected class MobStatWidget extends TElement {

        protected final MobStatEntry entry;
        private final WidgetTooltipHolder tooltipHolder = new WidgetTooltipHolder();

        public MobStatWidget(MobStatEntry entry, int x, int y, int size) {
            super(x, y, size, size);
            this.entry = entry;
        }

        @Override
        protected void renderSelf(int mouseX, int mouseY, float partialTicks) {
            // Cell background
            fill(x, y, getEndX(), getEndY(), 0x80000000);

            // Entity model preview rendered in the cell center
            // 在格子中央渲染实体模型预览
            EntityLivingBase entity = EntityModelRenderer.getEntity(entry.eggInfo.spawnedID);
            if (entity != null) {
                // Scale the model so its height fills the middle band of the cell
                // (between the name at the top and the kills line at the bottom)
                // 按实体身高缩放模型，使其填满格子中部区域（顶部名字与底部统计之间）
                int modelScale = Math.max(8, Math.min(30,
                        (int) (24.0F / Math.max(0.1F, entity.height))));
                // Pass mouse offsets relative to cell center so the model turns
                // to follow the cursor, mirroring the vanilla inventory player preview
                // 传入鼠标相对格子中心的偏移，使模型随光标转动（与原版背包玩家预览一致）
                // Respect the mobModelFollowCursor config option
                // 受配置项 mobModelFollowCursor 控制
                int cellCenterX = x + width / 2;
                int cellCenterY = y + height / 2;
                boolean followCursor = ModernStatistic.config.mobModelFollowCursor;
                EntityModelRenderer.renderEntity(entity, cellCenterX, y + height - 14,
                        modelScale,
                        followCursor ? (float) (mouseX - cellCenterX) : 0.0F,
                        followCursor ? (float) (mouseY - cellCenterY) : 0.0F);
            }

            // Entity name
            String name = entry.name;
            if (getFontRenderer().getStringWidth(name) > width - 4) {
                // Truncate if too long
                while (getFontRenderer().getStringWidth(name + "...") > width - 4 && name.length() > 1) {
                    name = name.substring(0, name.length() - 1);
                }
                name += "...";
            }
            drawCenteredString(getFontRenderer(), name, x + width / 2, y + 2, 0xFFFFFF);

            // Kills / Deaths
            String kd = entry.kills + " / " + entry.killedBy;
            drawCenteredString(getFontRenderer(), kd, x + width / 2, y + height - 12, 0xAAAAAA);
        }

        @Override
        protected void postRenderSelf(int mouseX, int mouseY, float partialTicks) {
            if (hovered) {
                drawOutline(x, y, getEndX(), getEndY(), COLOR_NORMAL_HOVERED);
                showTooltip(mouseX, mouseY);
            }
        }

        private void showTooltip(int mouseX, int mouseY) {
            List<String> lines = buildTooltipLines(entry);

            // Build tooltip text (newline-joined for Tooltip.create)
            // 构建 tooltip 文本（用换行符连接以供 Tooltip.create 使用）
            String tooltipText = String.join("\n", lines);
            Tooltip tooltip = Tooltip.create(tooltipText);
            tooltipHolder.set(tooltip);

            // Use widget's screen rectangle for positioning
            // 使用控件的屏幕矩形进行定位
            ScreenRectangle widgetRect = new ScreenRectangle(x, y, width, height);
            tooltipHolder.refreshTooltipForNextRenderPass(mouseX, mouseY, hovered, focused, widgetRect);
        }

        @Override
        protected boolean onMouseClicked(int mouseX, int mouseY, int button) {
            if (button == 2 && ModernStatistic.config.enableWikiLinks) {
                try {
                    String encoded = java.net.URLEncoder.encode(entry.name, "UTF-8").replace("+", "_");
                    String url = ModernStatistic.config.mobWikiBaseUrl + encoded;
                    screen.showWikiConfirm(url);
                } catch (java.io.UnsupportedEncodingException ignored) {}
                return true;
            }
            return false;
        }
    }

    // ==================== Tooltip helper ====================

    /**
     * Build tooltip lines for a mob widget.
     * <p>显示实体名称、击杀数、被击杀数。</p>
     *
     * @param entry the mob stat entry
     * @return list of tooltip lines
     */
    protected List<String> buildTooltipLines(MobStatEntry entry) {
        List<String> lines = new ArrayList<>();
        lines.add(entry.name);
        lines.add(I18n.format("stat.entityKills") + ": " + entry.kills);
        lines.add(I18n.format("stat.entityKilledBy") + ": " + entry.killedBy);
        return lines;
    }
}
