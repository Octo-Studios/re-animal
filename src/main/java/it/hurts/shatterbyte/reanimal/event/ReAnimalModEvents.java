package it.hurts.shatterbyte.reanimal.event;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.registry.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.registry.ReAnimalItems;
import it.hurts.shatterbyte.reanimal.client.renderer.HedgehogRenderer;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = ReAnimal.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ReAnimalModEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        ReAnimalEntities.registerAttributes(event);
    }

    @SubscribeEvent
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ReAnimalItems.HEDGEHOG_SPAWN_EGG.get());
        }
    }

    @EventBusSubscriber(modid = ReAnimal.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ClientOnlyEvents {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ReAnimalEntities.HEDGEHOG.get(), HedgehogRenderer::new);
        }
    }
}
