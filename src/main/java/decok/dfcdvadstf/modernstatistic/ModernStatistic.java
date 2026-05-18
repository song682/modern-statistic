package decok.dfcdvadstf.modernstatistic;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import decok.dfcdvadstf.modernstatistic.config.ModernStatisticConfig;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(name = Tags.NAME, modid = Tags.MODID, version = Tags.VERSION,
     dependencies = "required-after:createworldui", useMetadata = true)
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
        // Register event handler for item pickup and drop tracking
        MinecraftForge.EVENT_BUS.register(new ItemStatsTracker());
    }
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("Initializing ModernStatistic Mod");
    }
}
