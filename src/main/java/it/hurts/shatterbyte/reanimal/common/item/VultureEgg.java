package it.hurts.shatterbyte.reanimal.common.item;

import it.hurts.shatterbyte.reanimal.common.entity.vulture.VultureEggEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;

public class VultureEgg extends EggItem<VultureEggEntity> {
    public VultureEgg(Properties properties) {
        super(properties, ReAnimalEntities.VULTURE_EGG);
    }
}
