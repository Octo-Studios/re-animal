package it.hurts.shatterbyte.reanimal.event;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.client.renderer.butterfly.ButterflyRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.capybara.CapybaraRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.dragonfly.DragonflyRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.giraffe.GiraffeRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.hedgehog.HedgehogRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.hippopotamus.HippopotamusRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.kiwi.KiwiRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.ostrich.OstrichRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.pigeon.PigeonRenderer;
import it.hurts.shatterbyte.reanimal.registry.ReAnimalEntities;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = ReAnimal.MODID)
public class ReAnimalModEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        ReAnimalEntities.registerAttributes(event);
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

        event.register(
                ReAnimalEntities.BUTTERFLY.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ReAnimalEntities.CAPYBARA.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ReAnimalEntities.HIPPOPOTAMUS.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ReAnimalEntities.GIRAFFE.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ReAnimalEntities.DRAGONFLY.get(),
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
            event.registerEntityRenderer(ReAnimalEntities.BUTTERFLY.get(), ButterflyRenderer::new);
            event.registerEntityRenderer(ReAnimalEntities.CAPYBARA.get(), CapybaraRenderer::new);
            event.registerEntityRenderer(ReAnimalEntities.HIPPOPOTAMUS.get(), HippopotamusRenderer::new);
            event.registerEntityRenderer(ReAnimalEntities.GIRAFFE.get(), GiraffeRenderer::new);
            event.registerEntityRenderer(ReAnimalEntities.DRAGONFLY.get(), DragonflyRenderer::new);
        }
    }
}
