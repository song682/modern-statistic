package decok.dfcdvadstf.modernstatistic.gui.panel.stats;

import decok.dfcdvadstf.modernstatistic.gui.panel.BSPanel;
import decok.dfcdvadstf.modernstatistic.gui.screen.TBetterStatsScreen;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.IMob;

/**
 * Monster Hunter tab — shows only hostile mobs (monsters).
 * <p>Monsters that have been killed get a gold border.</p>
 */
public class BSStatPanel_MonsterHunter extends BSStatPanel_Mobs {

    public BSStatPanel_MonsterHunter(BSPanel parentToFill, TBetterStatsScreen screen) {
        super(parentToFill, screen);
    }

    @Override
    protected void filterEntries() {
        // Only keep monster-type entities
        entries.removeIf(entry -> {
            // Check if the entity class implements IMob (monster interface in 1.7.10)
            Class<?> entityClass = EntityList.getClassFromID(entry.eggInfo.spawnedID);
            if (entityClass == null) return true;
            return !IMob.class.isAssignableFrom(entityClass);
        });
    }
}
