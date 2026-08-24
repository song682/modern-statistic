package decok.dfcdvadstf.modernstatistic;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import decok.dfcdvadstf.catframe.ui.components.tab.TabRegistry;
import decok.dfcdvadstf.catframe.ui.overlay.OverlayManager;
import decok.dfcdvadstf.modernstatistic.command.CommandHudStats;
import decok.dfcdvadstf.modernstatistic.config.ModernStatisticConfig;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.overlay.StatsHudOverlay;
import decok.dfcdvadstf.modernstatistic.gui.tabbed.tab.StatsGeneralTab;
import decok.dfcdvadstf.modernstatistic.gui.tabbed.tab.StatsItemsTab;
import decok.dfcdvadstf.modernstatistic.gui.tabbed.tab.StatsMobsTab;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(name = Tags.NAME, modid = Tags.MODID, version = Tags.VERSION, dependencies = "required-after:catframe@[0.6.2,)", useMetadata = true)
public class ModernStatistic {
    public static final Logger logger = LogManager.getLogger(Tags.NAME);

    /** Mod configuration, loaded during preInit. */
    public static ModernStatisticConfig config;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger.info("Initializing ModernStatistic Mod");
        // Load configuration
        config = new ModernStatisticConfig(event.getSuggestedConfigurationFile());
        logger.info("Config loaded: uiLayoutMode=" + config.uiLayoutMode);

        // Register stats tabs into CatFrame TabRegistry with our bar ID
        // 将统计标签页注册到 CatFrame TabRegistry，带上我们的 barId
        TabRegistry.registerTab("modern_statistic", StatsGeneralTab::new, 105, "stat.generalButton", 0);
        TabRegistry.registerTab("modern_statistic", StatsItemsTab::new, 106, "stat.itemsButton", 1);
        TabRegistry.registerTab("modern_statistic", StatsMobsTab::new, 107, "stat.mobsButton", 2);
        logger.info("Registered stats tabs: General(105), Items(106), Mobs(107)");

        // Register event handler for item pickup and drop tracking
        MinecraftForge.EVENT_BUS.register(new ItemStatsTracker());

        // Register the HUD statistics overlay singleton — CatFrame's
        // ClientOverlayHandler renders HUD-context overlays automatically.
        // 注册 HUD 统计叠加层单例——CatFrame 的 ClientOverlayHandler 会自动渲染 HUD 上下文 Overlay。
        OverlayManager.INSTANCE.register(StatsHudOverlay.INSTANCE);
        logger.info("Registered HUD stats overlay");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("Initializing ModernStatistic Mod");

        // Register the client-side command for managing the pinned stats HUD
        // 注册管理固定统计 HUD 的客户端命令
        ClientCommandHandler.instance.registerCommand(new CommandHudStats());
        logger.info("Registered /hudstats command");
    }
}
