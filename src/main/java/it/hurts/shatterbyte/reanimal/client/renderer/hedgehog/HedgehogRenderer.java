package it.hurts.shatterbyte.reanimal.client.renderer.hedgehog;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.hurts.shatterbyte.reanimal.client.model.hedgehog.HedgehogModel;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class HedgehogRenderer extends GeoEntityRenderer<HedgehogEntity> {
    public HedgehogRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new HedgehogModel());

        this.addRenderLayer(new HedgehogItemLayer(this));
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, HedgehogEntity animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        if (animatable.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);

            this.shadowRadius = 0.35F * 0.5F;
        } else {
            poseStack.scale(1F, 1F, 1F);

            this.shadowRadius = 0.35F;
        }

        super.scaleModelForRender(widthScale, heightScale, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected void applyRotations(HedgehogEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        if (animatable.getVehicle() instanceof Player player) {
            var headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
            var headPitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());

            super.applyRotations(animatable, poseStack, ageInTicks, headYaw, partialTick, nativeScale);
            poseStack.mulPose(Axis.XP.rotationDegrees(-headPitch));

            return;
        }

        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }
}