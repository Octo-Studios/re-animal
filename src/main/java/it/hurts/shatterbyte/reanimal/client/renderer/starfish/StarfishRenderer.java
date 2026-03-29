package it.hurts.shatterbyte.reanimal.client.renderer.starfish;

import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.reanimal.client.model.starfish.StarfishModel;
import it.hurts.shatterbyte.reanimal.common.entity.starfish.StarfishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class StarfishRenderer extends GeoEntityRenderer<StarfishEntity> {
    public StarfishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new StarfishModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, StarfishEntity animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        if (animatable.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);

            this.shadowRadius = 0.25F * 0.5F;
        } else {
            poseStack.scale(1F, 1F, 1F);

            this.shadowRadius = 0.25F;
        }

        super.scaleModelForRender(widthScale, heightScale, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }
}
