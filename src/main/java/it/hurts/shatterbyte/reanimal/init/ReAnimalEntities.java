package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.butterfly.ButterflyEntity;
import it.hurts.shatterbyte.reanimal.common.entity.capybara.CapybaraEntity;
import it.hurts.shatterbyte.reanimal.common.entity.dragonfly.DragonflyEntity;
import it.hurts.shatterbyte.reanimal.common.entity.giraffe.GiraffeEntity;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogQuillArrowEntity;
import it.hurts.shatterbyte.reanimal.common.entity.hippopotamus.HippopotamusEntity;
import it.hurts.shatterbyte.reanimal.common.entity.kiwi.KiwiEggEntity;
import it.hurts.shatterbyte.reanimal.common.entity.kiwi.KiwiEntity;
import it.hurts.shatterbyte.reanimal.common.entity.ostrich.OstrichEggEntity;
import it.hurts.shatterbyte.reanimal.common.entity.ostrich.OstrichEntity;
import it.hurts.shatterbyte.reanimal.common.entity.pigeon.PigeonEggEntity;
import it.hurts.shatterbyte.reanimal.common.entity.pigeon.PigeonEntity;
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
                    .sized(0.5F, 0.65F)
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

    public static final DeferredHolder<EntityType<?>, EntityType<GiraffeEntity>> GIRAFFE = ENTITY_TYPES.register("giraffe", () ->
            EntityType.Builder.of(GiraffeEntity::new, MobCategory.CREATURE)
                    .sized(1.75F, 6F)
                    .build(ReAnimal.MODID + ":giraffe"));

    public static final DeferredHolder<EntityType<?>, EntityType<DragonflyEntity>> DRAGONFLY = ENTITY_TYPES.register("dragonfly", () ->
            EntityType.Builder.of(DragonflyEntity::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.45F)
                    .build(ReAnimal.MODID + ":dragonfly"));

    public static final DeferredHolder<EntityType<?>, EntityType<KiwiEggEntity>> KIWI_EGG = ENTITY_TYPES.register("kiwi_egg", () ->
            EntityType.Builder.of(KiwiEggEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(ReAnimal.MODID + ":kiwi_egg"));

    public static final DeferredHolder<EntityType<?>, EntityType<OstrichEggEntity>> OSTRICH_EGG = ENTITY_TYPES.register("ostrich_egg", () ->
            EntityType.Builder.of(OstrichEggEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(ReAnimal.MODID + ":ostrich_egg"));

    public static final DeferredHolder<EntityType<?>, EntityType<PigeonEggEntity>> PIGEON_EGG = ENTITY_TYPES.register("pigeon_egg", () ->
            EntityType.Builder.of(PigeonEggEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(ReAnimal.MODID + ":pigeon_egg"));

    public static final DeferredHolder<EntityType<?>, EntityType<HedgehogQuillArrowEntity>> QUILL_ARROW = ENTITY_TYPES.register("quill_arrow", () ->
            EntityType.Builder.<HedgehogQuillArrowEntity>of(HedgehogQuillArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(ReAnimal.MODID + ":quill_arrow"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HEDGEHOG.get(), HedgehogEntity.createAttributes().build());
        event.put(OSTRICH.get(), OstrichEntity.createAttributes().build());
        event.put(KIWI.get(), KiwiEntity.createAttributes().build());
        event.put(PIGEON.get(), PigeonEntity.createAttributes().build());
        event.put(BUTTERFLY.get(), ButterflyEntity.createAttributes().build());
        event.put(CAPYBARA.get(), CapybaraEntity.createAttributes().build());
        event.put(HIPPOPOTAMUS.get(), HippopotamusEntity.createAttributes().build());
        event.put(GIRAFFE.get(), GiraffeEntity.createAttributes().build());
        event.put(DRAGONFLY.get(), DragonflyEntity.createAttributes().build());
    }
}
