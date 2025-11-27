package it.hurts.shatterbyte.reanimal.common.item;

import it.hurts.shatterbyte.reanimal.common.entity.ostrich.OstrichEggEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;

public class OstrichEgg extends EggItem<OstrichEggEntity> {
    public OstrichEgg(Properties properties) {
        super(properties, ReAnimalEntities.OSTRICH_EGG);
    }
}
