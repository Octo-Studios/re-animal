package it.hurts.shatterbyte.reanimal.client.renderer.jellyfish;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.hurts.shatterbyte.reanimal.client.model.jellyfish.JellyfishModel;
import it.hurts.shatterbyte.reanimal.common.entity.jellyfish.JellyfishEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class JellyfishRenderer extends GeoEntityRenderer<JellyfishEntity> {
    public JellyfishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new JellyfishModel());

        this.shadowRadius = 0.3F;
    }

    @Override
    public RenderType getRenderType(JellyfishEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, JellyfishEntity animatable, BakedGeoModel model, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay, colour);
    }

    @Override
    protected void applyRotations(JellyfishEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float scale) {
        super.applyRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick, scale);

        var look = entity.getViewVector(partialTick);
        var hor = Math.sqrt(look.x * look.x + look.z * look.z);

        var pitch = (float) Math.toDegrees(Math.atan2(look.y, hor));

        poseStack.translate(0,0.5F,0);

        poseStack.mulPose(Axis.XP.rotationDegrees(pitch - 90F));
    }
}
