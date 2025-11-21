package it.hurts.shatterbyte.reanimal.client.renderer.kiwi;

import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.reanimal.client.model.kivi.KiwiModel;
import it.hurts.shatterbyte.reanimal.world.entity.kiwi.KiwiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KiwiRenderer extends GeoEntityRenderer<KiwiEntity> {
    public KiwiRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new KiwiModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, KiwiEntity animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        if (animatable.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);

            this.shadowRadius = 0.35F * 0.5F;
        } else {
            poseStack.scale(1F, 1F, 1F);

            this.shadowRadius = 0.35F;
        }

        super.scaleModelForRender(widthScale, heightScale, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }
}