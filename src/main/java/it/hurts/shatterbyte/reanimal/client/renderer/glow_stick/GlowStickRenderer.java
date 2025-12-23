package it.hurts.shatterbyte.reanimal.client.renderer.glow_stick;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.hurts.shatterbyte.reanimal.client.model.glow_stick.GlowStickModel;
import it.hurts.shatterbyte.reanimal.common.entity.glow_stick.GlowStickEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GlowStickRenderer extends GeoEntityRenderer<GlowStickEntity> {
    public GlowStickRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GlowStickModel());
    }

    @Override
    public RenderType getRenderType(GlowStickEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, GlowStickEntity animatable, BakedGeoModel model, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay, colour);
    }

    @Override
    protected void applyRotations(GlowStickEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float scale) {
        poseStack.translate(0.0F, 0.3125F, 0.0F);

        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getRenderYaw(partialTick)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getRenderPitch(partialTick)));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getRenderRoll(partialTick)));

        poseStack.translate(0.0F, -0.3125F, 0.0F);
    }
}
