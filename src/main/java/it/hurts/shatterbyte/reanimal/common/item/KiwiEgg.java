package it.hurts.shatterbyte.reanimal.common.item;

import it.hurts.shatterbyte.reanimal.common.entity.kiwi.KiwiEggEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;

public class KiwiEgg extends EggItem<KiwiEggEntity> {
    public KiwiEgg(Properties properties) {
        super(properties, ReAnimalEntities.KIWI_EGG);
    }
}
