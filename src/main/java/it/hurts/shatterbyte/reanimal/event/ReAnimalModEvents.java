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
import it.hurts.shatterbyte.reanimal.common.entity.butterfly.ButterflyEntity;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.QuillArrowEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalBlocks;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import it.hurts.shatterbyte.reanimal.init.ReAnimalPotions;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
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
                ButterflyEntity::checkButterflySpawnRules,
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

    @SubscribeEvent
    public static void registerBrewing(RegisterBrewingRecipesEvent event) {
        var registry = event.getRegistryAccess().registryOrThrow(Registries.POTION);

        var quill = registry.getHolder(ReAnimalPotions.QUILL.getKey()).orElseThrow();
        var longQuill = registry.getHolder(ReAnimalPotions.LONG_QUILL.getKey()).orElseThrow();
        var strongQuill = registry.getHolder(ReAnimalPotions.STRONG_QUILL.getKey()).orElseThrow();

        event.getBuilder().addMix(Potions.AWKWARD, ReAnimalItems.QUILL.get(), quill);
        event.getBuilder().addMix(quill, Items.REDSTONE, longQuill);
        event.getBuilder().addMix(quill, Items.GLOWSTONE_DUST, strongQuill);
    }

    @EventBusSubscriber(modid = ReAnimal.MODID, value = Dist.CLIENT)
    public static class ClientOnlyEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(ReAnimalBlocks.QUILL_PLATFORM.get(), RenderType.cutout()));
        }

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
            event.registerEntityRenderer(ReAnimalEntities.KIWI_EGG.get(), ThrownItemRenderer::new);
            event.registerEntityRenderer(ReAnimalEntities.OSTRICH_EGG.get(), ThrownItemRenderer::new);
            event.registerEntityRenderer(ReAnimalEntities.PIGEON_EGG.get(), ThrownItemRenderer::new);
            event.registerEntityRenderer(ReAnimalEntities.QUILL_ARROW.get(), context -> new ArrowRenderer<QuillArrowEntity>(context) {
                @Override
                public ResourceLocation getTextureLocation(QuillArrowEntity entity) {
                    return ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/quill_arrow.png");
                }
            });
        }
    }
}
