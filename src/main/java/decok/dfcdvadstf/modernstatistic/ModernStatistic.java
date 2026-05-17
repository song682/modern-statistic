package decok.dfcdvadstf.modernstatistic;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(name = Tags.NAME, modid = Tags.MODID, version = Tags.VERSION,
     dependencies = "required-after:createworldui", useMetadata = true)
public class ModernStatistic {
    public static final Logger logger = LogManager.getLogger(Tags.NAME);
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger.info("Initializing ModernStatistic Mod");
        // Register event handler for item pickup and drop tracking
        MinecraftForge.EVENT_BUS.register(new ItemStatsTracker());
    }
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("Initializing ModernStatistic Mod");
    }
}
