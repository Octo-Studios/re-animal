package it.hurts.shatterbyte.reanimal.registry;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.world.entity.HedgehogEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ReAnimalEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ReAnimal.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<HedgehogEntity>> HEDGEHOG = ENTITY_TYPES.register("hedgehog", () ->
            EntityType.Builder.of(HedgehogEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.45F)
                    .build(ReAnimal.MODID + ":hedgehog"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HEDGEHOG.get(), HedgehogEntity.createAttributes().build());
    }
}
