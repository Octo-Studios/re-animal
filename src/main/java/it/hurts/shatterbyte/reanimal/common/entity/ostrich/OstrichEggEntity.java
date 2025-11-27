package it.hurts.shatterbyte.reanimal.common.entity.ostrich;

import it.hurts.shatterbyte.reanimal.common.entity.egg.ThrowableEggEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class OstrichEggEntity extends ThrowableEggEntity {
    public OstrichEggEntity(EntityType<? extends OstrichEggEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected Item getEggItem() {
        return ReAnimalItems.OSTRICH_EGG.get();
    }

    @Override
    protected EntityType<? extends AgeableMob> getHatchlingType() {
        return ReAnimalEntities.OSTRICH.get();
    }
}
