package it.hurts.shatterbyte.reanimal.client.renderer.vulture;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.hurts.shatterbyte.reanimal.client.model.vulture.VultureModel;
import it.hurts.shatterbyte.reanimal.common.entity.seal.SealEntity;
import it.hurts.shatterbyte.reanimal.common.entity.vulture.VultureEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VultureRenderer extends GeoEntityRenderer<VultureEntity> {
    public VultureRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new VultureModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, VultureEntity animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        if (animatable.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);

            this.shadowRadius = 0.5F * 0.5F;
        } else {
            poseStack.scale(1F, 1F, 1F);

            this.shadowRadius = 0.5F;
        }

        super.scaleModelForRender(widthScale, heightScale, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected void applyRotations(VultureEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float scale) {
        super.applyRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick, scale);

        if (entity.onGround())
            return;

        var look = entity.getViewVector(partialTick);
        var hor = Math.sqrt(look.x * look.x + look.z * look.z);

        var pitch = (float) Math.toDegrees(Math.atan2(look.y, hor));

        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }
}
