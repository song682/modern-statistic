package decok.dfcdvadstf.modernstatistic;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(name = Tags.NAME, modid = Tags.MODID, version = Tags.VERSION,
     dependencies = "required-after:createworldui", useMetadata = true)
public class ModernStatistic {
    public static final Logger logger = LogManager.getLogger(Tags.NAME);
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger.info("Initializing ModernStatistic Mod");

    }
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("Initializing ModernStatistic Mod");
    }
}
