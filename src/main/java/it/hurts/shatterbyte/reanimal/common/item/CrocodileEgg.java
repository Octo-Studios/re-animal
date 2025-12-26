package it.hurts.shatterbyte.reanimal.common.item;

import it.hurts.shatterbyte.reanimal.common.entity.crocodile.CrocodileEggEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;

public class CrocodileEgg extends EggItem<CrocodileEggEntity> {
    public CrocodileEgg(Properties properties) {
        super(properties, ReAnimalEntities.CROCODILE_EGG);
    }
}
