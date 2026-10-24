package it.hurts.shatterbyte.reanimal.fabric;

import it.hurts.shatterbyte.reanimal.client.renderer.butterfly.ButterflyRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.capybara.CapybaraRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.crocodile.CrocodileRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.dragonfly.DragonflyRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.giraffe.GiraffeRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.glow_stick.GlowStickRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.hedgehog.HedgehogRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.hippopotamus.HippopotamusRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.jellyfish.JellyfishRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.kiwi.KiwiRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.ostrich.OstrichRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.penguin.PenguinRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.pigeon.PigeonRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.red_panda.RedPandaRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.sea_urchin.SeaUrchinRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.seal.SealRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.starfish.StarfishRenderer;
import it.hurts.shatterbyte.reanimal.client.renderer.vulture.VultureRenderer;
import it.hurts.shatterbyte.reanimal.client.sound.ButterflySoundInstance;
import it.hurts.shatterbyte.reanimal.client.sound.DragonflySoundInstance;
import it.hurts.shatterbyte.reanimal.common.entity.butterfly.ButterflyEntity;
import it.hurts.shatterbyte.reanimal.common.entity.dragonfly.DragonflyEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import it.hurts.shatterbyte.reanimal.client.renderer.quill_arrow.QuillArrowRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class ReAnimalFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ReAnimalEntities.HEDGEHOG, HedgehogRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.OSTRICH, OstrichRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.KIWI, KiwiRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.PIGEON, PigeonRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.BUTTERFLY, ButterflyRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.CAPYBARA, CapybaraRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.RED_PANDA, RedPandaRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.SEAL, SealRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.HIPPOPOTAMUS, HippopotamusRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.CROCODILE, CrocodileRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.GIRAFFE, GiraffeRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.DRAGONFLY, DragonflyRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.VULTURE, VultureRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.PENGUIN, PenguinRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.SEA_URCHIN, SeaUrchinRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.STARFISH, StarfishRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.JELLYFISH, JellyfishRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.KIWI_EGG, ThrownItemRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.OSTRICH_EGG, ThrownItemRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.PIGEON_EGG, ThrownItemRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.VULTURE_EGG, ThrownItemRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.PENGUIN_EGG, ThrownItemRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.CROCODILE_EGG, ThrownItemRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.GLOW_STICK, GlowStickRenderer::new);
        EntityRendererRegistry.register(ReAnimalEntities.QUILL_ARROW, QuillArrowRenderer::new);

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ButterflyEntity butterfly)
                Minecraft.getInstance().getSoundManager().queueTickingSound(new ButterflySoundInstance(butterfly));
            if (entity instanceof DragonflyEntity dragonfly)
                Minecraft.getInstance().getSoundManager().queueTickingSound(new DragonflySoundInstance(dragonfly));
        });
    }
}
