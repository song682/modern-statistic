package decok.dfcdvadstf.modernstatistic.gui.panel.stats;

import java.util.*;

import decok.dfcdvadstf.modernstatistic.ModernStatistic;
import decok.dfcdvadstf.modernstatistic.gui.panel.BSPanel;
import decok.dfcdvadstf.modernstatistic.gui.screen.TBetterStatsScreen;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityList;

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

    protected class MobStatWidget extends decok.dfcdvadstf.modernstatistic.gui.TElement {

        protected final MobStatEntry entry;

        public MobStatWidget(MobStatEntry entry, int x, int y, int size) {
            super(x, y, size, size);
            this.entry = entry;
        }

        @Override
        protected void renderSelf(int mouseX, int mouseY, float partialTicks) {
            // Cell background
            fill(x, y, getEndX(), getEndY(), 0x80000000);

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
                drawTooltip(mouseX, mouseY);
            }
        }

        private void drawTooltip(int mouseX, int mouseY) {
            List<String> lines = new ArrayList<>();
            lines.add(entry.name);

            String killsText;
            if (entry.kills == 0) {
                killsText = I18n.format("stat.entityKills.none", entry.name);
            } else {
                killsText = I18n.format("stat.entityKills", entry.kills, entry.name);
            }
            lines.add(killsText);

            String killedByText;
            if (entry.killedBy == 0) {
                killedByText = I18n.format("stat.entityKilledBy.none", entry.name);
            } else {
                killedByText = I18n.format("stat.entityKilledBy", entry.name, entry.killedBy);
            }
            lines.add(killedByText);

            BSStatPanel_Items.drawHoverTooltip(lines, mouseX, mouseY);
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
}
