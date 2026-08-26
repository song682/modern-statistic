package decok.dfcdvadstf.modernstatistic.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration for ModernStatistic mod.
 * <p>Controls UI layout mode (Tabbed vs Paneled) and other display options.</p>
 */
public class ModernStatisticConfig {

    public final Configuration configFile;

    public String uiLayoutMode;
    public String defaultUILayout;
    public boolean showEmptyStats;
    public boolean tabbedLayoutClear;
    public String defaultTab;
    public boolean enableBalancedDietTab;
    public boolean enableMonsterHunterTab;
    public boolean enableWikiLinks;
    public boolean mobModelFollowCursor;
    public String itemWikiBaseUrl;
    public String mobWikiBaseUrl;
    public boolean inputFocusHighlight;
    public boolean enableHudOverlay;
    public String pinnedStats;

    public ModernStatisticConfig(File file) {
        configFile = new Configuration(file);

        configFile.addCustomCategoryComment("tab", "UI layout and display options for the statistics screen.");
        configFile.addCustomCategoryComment("betterstats", "Wiki integration options.");

        configFile.load();
        loadOptions();
        save();
    }

    private void loadOptions() {
        uiLayoutMode = configFile.getString("uiLayoutMode", Configuration.CATEGORY_GENERAL, "TABBED",
                "UI layout mode: TABBED (top-positioned tabs), PANELED (BetterStats-style left sidebar panel), or VANILLA (original vanilla screen).",
                new String[]{"TABBED", "PANELED", "VANILLA"});

        defaultUILayout = configFile.getString("defaultUILayout", "betterstats", "VANILLA",
                "Default UI layout mode for BetterStats that depend on ModernStatistic. One of: TABBED and VANILLA.",
                new String[]{"TABBED", "VANILLA"});

        showEmptyStats = configFile.getBoolean("showEmptyStats", "betterstats", false,
                "If true, statistics with a value of zero will still be shown.");

        defaultTab = configFile.getString("defaultTab", "betterstats", "General",
                "The tab shown by default when opening the stats screen. One of: General, Blocks, Items, Mobs, BalancedDiet, MonsterHunter.",
                new String[]{"General", "Blocks", "Items", "Mobs", "BalancedDiet", "MonsterHunter"});

        enableBalancedDietTab = configFile.getBoolean("enableBalancedDietTab", "betterstats", true,
                "If true, the Balanced Diet tab (food items only) is available.");

        enableMonsterHunterTab = configFile.getBoolean("enableMonsterHunterTab", "betterstats", true,
                "If true, the Monster Hunter tab (monster kills only) is available.");

        inputFocusHighlight = configFile.getBoolean("inputFocusHighlight", "betterstats", true,
                "If true, the search text field shows a bright border when focused.");

        enableWikiLinks = configFile.getBoolean("enableWikiLinks", "betterstats", true,
                "If true, middle-clicking an item or mob opens its wiki page in the browser.");

        mobModelFollowCursor = configFile.getBoolean("mobModelFollowCursor", "betterstats", true,
                "If true, mob models in the stats grid turn to follow the cursor.");

        itemWikiBaseUrl = configFile.getString("itemWikiBaseUrl", "betterstats",
                "https://minecraft.fandom.com/wiki/",
                "Base URL for item wiki lookups. The item name is appended to this URL.");

        mobWikiBaseUrl = configFile.getString("mobWikiBaseUrl", "betterstats",
                "https://minecraft.fandom.com/wiki/",
                "Base URL for mob wiki lookups. The mob name is appended to this URL.");

        tabbedLayoutClear = configFile.getBoolean("tabbedLayoutClear", "tab",
                false, "Enable this to have the cleared tab and background.");

        enableHudOverlay = configFile.getBoolean("enableHudOverlay", "betterstats", true,
                "If true, the statistics HUD overlay is shown on the game screen when stats are pinned.");

        pinnedStats = configFile.getString("pinnedStats", "betterstats", "",
                "Comma-separated list of stat IDs pinned to the HUD overlay. "
                        + "Example: stat.jump,stat.mineBlock.1. Managed via the /hudstats command or the right-click menu.");
    }

    public void save() {
        configFile.save();
    }

    // === Convenience methods ===

    public boolean isTabbedMode() {
        return "TABBED".equalsIgnoreCase(uiLayoutMode);
    }

    public boolean isPaneledMode() {
        return "PANELED".equalsIgnoreCase(uiLayoutMode);
    }

    public boolean isVanillaMode() {
        return "VANILLA".equalsIgnoreCase(uiLayoutMode);
    }

    // === Default UI Layout Mode for BetterStats ===
    public boolean isBackToVanillaMode() {
        return "VANILLA".equalsIgnoreCase(defaultUILayout);
    }
    
    public boolean isBackToTabbedMode() {
        return "TABBED".equalsIgnoreCase(defaultUILayout);
    }


    // === HUD convenience methods ===

    /**
     * Parse the pinned stats string into a list of stat IDs.
     * <p>解析固定统计字符串为统计 ID 列表。</p>
     */
    public List<String> getPinnedStatIds() {
        if (pinnedStats == null || pinnedStats.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<String> ids = new ArrayList<>(Arrays.asList(pinnedStats.split(",")));
        ids.removeIf(String::isEmpty);
        return ids;
    }

    /**
     * Pin a stat to the HUD overlay and persist it.
     * <p>将一个统计固定到 HUD 叠加层并持久化。</p>
     */
    public void addPinnedStat(String statId) {
        List<String> ids = getPinnedStatIds();
        if (statId == null || statId.isEmpty() || ids.contains(statId)) {
            return;
        }
        ids.add(statId);
        pinnedStats = String.join(",", ids);
        save();
    }

    /**
     * Remove a stat from the HUD overlay and persist it.
     * <p>从 HUD 叠加层移除一个统计并持久化。</p>
     */
    public void removePinnedStat(String statId) {
        List<String> ids = getPinnedStatIds();
        if (!ids.remove(statId)) {
            return;
        }
        pinnedStats = String.join(",", ids);
        save();
    }

    /**
     * Clear all pinned stats from the HUD overlay and persist it.
     * <p>清空 HUD 叠加层的所有统计并持久化。</p>
     */
    public void clearPinnedStats() {
        pinnedStats = "";
        save();
    }
}
