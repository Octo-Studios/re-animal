package it.hurts.shatterbyte.reanimal.client.renderer.butterfly;

import it.hurts.shatterbyte.reanimal.client.model.butterfly.ButterflyModel;
import it.hurts.shatterbyte.reanimal.world.entity.butterfly.ButterflyEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ButterflyRenderer extends GeoEntityRenderer<ButterflyEntity> {
    public ButterflyRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ButterflyModel());

        this.shadowRadius = 0.25F;
    }

    @Override
    public RenderType getRenderType(ButterflyEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}