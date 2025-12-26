package it.hurts.shatterbyte.reanimal.common.entity.crocodile;

import it.hurts.shatterbyte.reanimal.common.entity.egg.ThrowableEggEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class CrocodileEggEntity extends ThrowableEggEntity {
    public CrocodileEggEntity(EntityType<? extends CrocodileEggEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected Item getEggItem() {
        return ReAnimalItems.CROCODILE_EGG.get();
    }

    @Override
    protected EntityType<? extends AgeableMob> getHatchlingType() {
        return ReAnimalEntities.CROCODILE.get();
    }
}
