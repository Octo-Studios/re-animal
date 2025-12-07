package it.hurts.shatterbyte.reanimal.common.item;

import it.hurts.shatterbyte.reanimal.common.entity.kiwi.KiwiEggEntity;
import it.hurts.shatterbyte.reanimal.common.entity.penguin.PenguinEggEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;

public class PenguinEgg extends EggItem<PenguinEggEntity> {
    public PenguinEgg(Properties properties) {
        super(properties, ReAnimalEntities.PENGUIN_EGG);
    }
}
