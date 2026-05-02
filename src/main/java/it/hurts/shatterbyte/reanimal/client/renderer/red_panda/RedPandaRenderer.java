package it.hurts.shatterbyte.reanimal.client.renderer.red_panda;

import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.reanimal.client.model.red_panda.RedPandaModel;
import it.hurts.shatterbyte.reanimal.common.entity.red_panda.RedPandaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RedPandaRenderer extends GeoEntityRenderer<RedPandaEntity> {
    public RedPandaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RedPandaModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, RedPandaEntity animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(widthScale, heightScale, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);

        if (animatable.isBaby())
            this.shadowRadius = 0.5F * 0.5F;
        else
            this.shadowRadius = 0.5F;
    }
}
