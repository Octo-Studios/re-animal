package it.hurts.shatterbyte.reanimal.event;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.client.renderer.hedgehog.HedgehogRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.kiwi.KiwiRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.ostrich.OstrichRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.pigeon.PigeonRenderer;
import it.hurts.shatterbyte.reanimal.registry.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.registry.ReAnimalItems;
import it.hurts.shatterbyte.reanimal.world.entity.ostrich.OstrichEntity;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = ReAnimal.MODID)
public class ReAnimalModEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        ReAnimalEntities.registerAttributes(event);
    }

    @SubscribeEvent
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ReAnimalItems.HEDGEHOG_SPAWN_EGG.get());
            event.accept(ReAnimalItems.OSTRICH_SPAWN_EGG.get());
            event.accept(ReAnimalItems.KIWI_SPAWN_EGG.get());
            event.accept(ReAnimalItems.PIGEON_SPAWN_EGG.get());
        }
    }

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                ReAnimalEntities.HEDGEHOG.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ReAnimalEntities.OSTRICH.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ReAnimalEntities.KIWI.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ReAnimalEntities.PIGEON.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    @EventBusSubscriber(modid = ReAnimal.MODID, value = Dist.CLIENT)
    public static class ClientOnlyEvents {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ReAnimalEntities.HEDGEHOG.get(), HedgehogRenderer::new);
            event.registerEntityRenderer(ReAnimalEntities.OSTRICH.get(), OstrichRenderer::new);
            event.registerEntityRenderer(ReAnimalEntities.KIWI.get(), KiwiRenderer::new);
            event.registerEntityRenderer(ReAnimalEntities.PIGEON.get(), PigeonRenderer::new);
        }
    }
}
