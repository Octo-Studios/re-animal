package it.hurts.shatterbyte.reanimal.common.entity.penguin;

import it.hurts.shatterbyte.reanimal.common.entity.egg.ThrowableEggEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class PenguinEggEntity extends ThrowableEggEntity {
    public PenguinEggEntity(EntityType<? extends PenguinEggEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected Item getEggItem() {
        return ReAnimalItems.PENGUIN_EGG.get();
    }

    @Override
    protected EntityType<? extends AgeableMob> getHatchlingType() {
        return ReAnimalEntities.PENGUIN.get();
    }
}
