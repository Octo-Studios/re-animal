package it.hurts.shatterbyte.reanimal.common.entity.toucan;

import it.hurts.shatterbyte.reanimal.common.entity.egg.ThrowableEggEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ToucanEggEntity extends ThrowableEggEntity {
    public ToucanEggEntity(EntityType<? extends ToucanEggEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected Item getEggItem() {
        return ReAnimalItems.TOUCAN_EGG;
    }

    @Override
    protected EntityType<? extends AgeableMob> getHatchlingType() {
        return ReAnimalEntities.TOUCAN;
    }
}
