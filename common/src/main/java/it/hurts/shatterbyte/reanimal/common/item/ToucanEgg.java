package it.hurts.shatterbyte.reanimal.common.item;

import it.hurts.shatterbyte.reanimal.common.entity.toucan.ToucanEggEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;

public class ToucanEgg extends EggItem<ToucanEggEntity> {
    public ToucanEgg(Properties properties) {
        super(properties, () -> ReAnimalEntities.TOUCAN_EGG);
    }
}
