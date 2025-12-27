package it.hurts.shatterbyte.reanimal.client.renderer.crocodile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.hurts.shatterbyte.reanimal.client.model.crocodile.CrocodileModel;
import it.hurts.shatterbyte.reanimal.common.entity.crocodile.CrocodileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CrocodileRenderer extends GeoEntityRenderer<CrocodileEntity> {
    public CrocodileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CrocodileModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, CrocodileEntity animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        if (animatable.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);

            this.shadowRadius = 0.6F * 0.5F;
        } else {
            poseStack.scale(1F, 1F, 1F);

            this.shadowRadius = 0.6F;
        }

        super.scaleModelForRender(widthScale, heightScale, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected void applyRotations(CrocodileEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float scale) {
        super.applyRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick, scale);

        if (!entity.isInWaterOrBubble())
            return;

        var look = entity.getViewVector(partialTick);
        var hor = Math.sqrt(look.x * look.x + look.z * look.z);

        var pitch = (float) Math.toDegrees(Math.atan2(look.y, hor));

        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }
}
