package it.hurts.shatterbyte.reanimal.neoforge;

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
import it.hurts.shatterbyte.reanimal.common.entity.glow_stick.GlowStickEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.client.renderer.quill_arrow.QuillArrowRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public class ReAnimalNeoForgeClient {
    public ReAnimalNeoForgeClient(IEventBus modBus) {
        modBus.addListener(this::registerRenderers);
        NeoForge.EVENT_BUS.addListener(this::onEntityJoin);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ReAnimalEntities.HEDGEHOG, HedgehogRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.OSTRICH, OstrichRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.KIWI, KiwiRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.PIGEON, PigeonRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.BUTTERFLY, ButterflyRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.CAPYBARA, CapybaraRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.RED_PANDA, RedPandaRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.SEAL, SealRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.HIPPOPOTAMUS, HippopotamusRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.CROCODILE, CrocodileRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.GIRAFFE, GiraffeRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.DRAGONFLY, DragonflyRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.VULTURE, VultureRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.PENGUIN, PenguinRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.SEA_URCHIN, SeaUrchinRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.STARFISH, StarfishRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.JELLYFISH, JellyfishRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.KIWI_EGG, ThrownItemRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.OSTRICH_EGG, ThrownItemRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.PIGEON_EGG, ThrownItemRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.VULTURE_EGG, ThrownItemRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.PENGUIN_EGG, ThrownItemRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.CROCODILE_EGG, ThrownItemRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.GLOW_STICK, GlowStickRenderer::new);
        event.registerEntityRenderer(ReAnimalEntities.QUILL_ARROW, QuillArrowRenderer::new);
    }

    private void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof ButterflyEntity butterfly)
            Minecraft.getInstance().getSoundManager().queueTickingSound(new ButterflySoundInstance(butterfly));

        if (event.getEntity() instanceof DragonflyEntity dragonfly)
            Minecraft.getInstance().getSoundManager().queueTickingSound(new DragonflySoundInstance(dragonfly));
    }
}
