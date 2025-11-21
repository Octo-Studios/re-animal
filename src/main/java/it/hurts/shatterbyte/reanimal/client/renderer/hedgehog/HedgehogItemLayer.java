package it.hurts.shatterbyte.reanimal.client.renderer.hedgehog;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.hurts.shatterbyte.reanimal.world.entity.hedgehog.HedgehogEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class HedgehogItemLayer extends GeoRenderLayer<HedgehogEntity> {
    public HedgehogItemLayer(GeoRenderer<HedgehogEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, HedgehogEntity animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (animatable.isDeadOrDying())
            return;

        var stack = animatable.getStack();

        if (stack.isEmpty())
            return;

        var bone = bakedModel.getBone("item").orElse(null);

        if (bone == null)
            return;

        poseStack.pushPose();

        poseStack.translate(0F, -0.525F, 0F);

        var modelPos = bone.getModelPosition();

        poseStack.translate(modelPos.x / 16.0F, modelPos.y / 16.0F, modelPos.z / 16.0F);

        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot)));
        poseStack.mulPose(bone.getModelRotationMatrix());

        poseStack.scale(0.5F, 0.5F, 0.5F);

        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, animatable.level(), animatable.getId());

        poseStack.popPose();
    }
}