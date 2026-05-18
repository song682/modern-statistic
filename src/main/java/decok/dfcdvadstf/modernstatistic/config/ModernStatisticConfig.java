package decok.dfcdvadstf.modernstatistic.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * Configuration for ModernStatistic mod.
 * <p>Controls UI layout mode (Tabbed vs Paneled) and other display options.</p>
 */
public class ModernStatisticConfig {

    public final Configuration configFile;

    // === UI Layout Mode ===
    /** "TABBED" = top-positioned tabs (default), "PANELED" = BetterStats-style left sidebar, "VANILLA" = original vanilla screen */
    public String uiLayoutMode;

    // === Default UI Layout (for other mods) ===
    /** Default UI layout mode for other mods that depend on ModernStatistic. One of: TABBED, PANELED, VANILLA */
    public String defaultUILayout;

    // === Display Options ===
    public boolean showEmptyStats;
    public String defaultTab;
    public boolean enableBalancedDietTab;
    public boolean enableMonsterHunterTab;
    public boolean enableWikiLinks;
    public String itemWikiBaseUrl;
    public String mobWikiBaseUrl;
    public boolean inputFocusHighlight;

    public ModernStatisticConfig(File file) {
        configFile = new Configuration(file);

        configFile.addCustomCategoryComment("ui", "UI layout and display options for the statistics screen.");
        configFile.addCustomCategoryComment("wiki", "Wiki integration options.");

        configFile.load();
        loadOptions();
        save();
    }

    private void loadOptions() {
        // --- UI category ---
        uiLayoutMode = configFile.getString("uiLayoutMode", "ui", "TABBED",
                "UI layout mode: TABBED (top-positioned tabs), PANELED (BetterStats-style left sidebar panel), or VANILLA (original vanilla screen).",
                new String[]{"TABBED", "PANELED", "VANILLA"});

        defaultUILayout = configFile.getString("defaultUILayout", "ui", "TABBED",
                "Default UI layout mode for other mods that depend on ModernStatistic. One of: TABBED, PANELED, VANILLA.",
                new String[]{"TABBED", "PANELED", "VANILLA"});

        showEmptyStats = configFile.getBoolean("showEmptyStats", "ui", false,
                "If true, statistics with a value of zero will still be shown.");

        defaultTab = configFile.getString("defaultTab", "ui", "General",
                "The tab shown by default when opening the stats screen. One of: General, Items, Mobs, BalancedDiet, MonsterHunter.",
                new String[]{"General", "Items", "Mobs", "BalancedDiet", "MonsterHunter"});

        enableBalancedDietTab = configFile.getBoolean("enableBalancedDietTab", "ui", true,
                "If true, the Balanced Diet tab (food items only) is available.");

        enableMonsterHunterTab = configFile.getBoolean("enableMonsterHunterTab", "ui", true,
                "If true, the Monster Hunter tab (monster kills only) is available.");

        inputFocusHighlight = configFile.getBoolean("inputFocusHighlight", "ui", true,
                "If true, the search text field shows a bright border when focused.");

        // --- Wiki category ---
        enableWikiLinks = configFile.getBoolean("enableWikiLinks", "wiki", true,
                "If true, middle-clicking an item or mob opens its wiki page in the browser.");

        itemWikiBaseUrl = configFile.getString("itemWikiBaseUrl", "wiki",
                "https://minecraft.fandom.com/wiki/",
                "Base URL for item wiki lookups. The item name is appended to this URL.");

        mobWikiBaseUrl = configFile.getString("mobWikiBaseUrl", "wiki",
                "https://minecraft.fandom.com/wiki/",
                "Base URL for mob wiki lookups. The mob name is appended to this URL.");
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
}
