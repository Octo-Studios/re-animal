package it.hurts.shatterbyte.reanimal.common.item;

import it.hurts.shatterbyte.reanimal.common.entity.pigeon.PigeonEggEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;

public class PigeonEgg extends EggItem<PigeonEggEntity> {
    public PigeonEgg(Properties properties) {
        super(properties, ReAnimalEntities.PIGEON_EGG);
    }
}
