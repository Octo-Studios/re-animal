package it.hurts.shatterbyte.reanimal.registry;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.world.entity.butterfly.ButterflyEntity;
import it.hurts.shatterbyte.reanimal.world.entity.capybara.CapybaraEntity;
import it.hurts.shatterbyte.reanimal.world.entity.hedgehog.HedgehogEntity;
import it.hurts.shatterbyte.reanimal.world.entity.hippopotamus.HippopotamusEntity;
import it.hurts.shatterbyte.reanimal.world.entity.kiwi.KiwiEntity;
import it.hurts.shatterbyte.reanimal.world.entity.ostrich.OstrichEntity;
import it.hurts.shatterbyte.reanimal.world.entity.pigeon.PigeonEntity;
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
                    .sized(0.6F, 0.5F)
                    .build(ReAnimal.MODID + ":hedgehog"));

    public static final DeferredHolder<EntityType<?>, EntityType<OstrichEntity>> OSTRICH = ENTITY_TYPES.register("ostrich", () ->
            EntityType.Builder.of(OstrichEntity::new, MobCategory.CREATURE)
                    .sized(1.25F, 2.75F)
                    .build(ReAnimal.MODID + ":ostrich"));

    public static final DeferredHolder<EntityType<?>, EntityType<KiwiEntity>> KIWI = ENTITY_TYPES.register("kiwi", () ->
            EntityType.Builder.of(KiwiEntity::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.6F)
                    .build(ReAnimal.MODID + ":kiwi"));

    public static final DeferredHolder<EntityType<?>, EntityType<PigeonEntity>> PIGEON = ENTITY_TYPES.register("pigeon", () ->
            EntityType.Builder.of(PigeonEntity::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.5F)
                    .build(ReAnimal.MODID + ":pigeon"));

    public static final DeferredHolder<EntityType<?>, EntityType<ButterflyEntity>> BUTTERFLY = ENTITY_TYPES.register("butterfly", () ->
            EntityType.Builder.of(ButterflyEntity::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.45F)
                    .build(ReAnimal.MODID + ":butterfly"));

    public static final DeferredHolder<EntityType<?>, EntityType<CapybaraEntity>> CAPYBARA = ENTITY_TYPES.register("capybara", () ->
            EntityType.Builder.of(CapybaraEntity::new, MobCategory.CREATURE)
                    .sized(1.1F, 1F)
                    .build(ReAnimal.MODID + ":capybara"));

    public static final DeferredHolder<EntityType<?>, EntityType<HippopotamusEntity>> HIPPOPOTAMUS = ENTITY_TYPES.register("hippopotamus", () ->
            EntityType.Builder.of(HippopotamusEntity::new, MobCategory.CREATURE)
                    .sized(1.5F, 1.75F)
                    .build(ReAnimal.MODID + ":hippopotamus"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HEDGEHOG.get(), HedgehogEntity.createAttributes().build());
        event.put(OSTRICH.get(), OstrichEntity.createAttributes().build());
        event.put(KIWI.get(), KiwiEntity.createAttributes().build());
        event.put(PIGEON.get(), PigeonEntity.createAttributes().build());
        event.put(BUTTERFLY.get(), ButterflyEntity.createAttributes().build());
        event.put(CAPYBARA.get(), CapybaraEntity.createAttributes().build());
        event.put(HIPPOPOTAMUS.get(), HippopotamusEntity.createAttributes().build());
    }
}
