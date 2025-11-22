package it.hurts.shatterbyte.reanimal.client.renderer.pigeon;

import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.reanimal.client.model.kivi.KiwiModel;
import it.hurts.shatterbyte.reanimal.client.model.pigeon.PigeonModel;
import it.hurts.shatterbyte.reanimal.world.entity.kiwi.KiwiEntity;
import it.hurts.shatterbyte.reanimal.world.entity.pigeon.PigeonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PigeonRenderer extends GeoEntityRenderer<PigeonEntity> {
    public PigeonRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PigeonModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, PigeonEntity animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
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