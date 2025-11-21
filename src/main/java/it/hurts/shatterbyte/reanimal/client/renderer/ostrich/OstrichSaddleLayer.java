package it.hurts.shatterbyte.reanimal.client.renderer.ostrich;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.hurts.shatterbyte.reanimal.client.model.ostrich.OstrichSaddleModel;
import it.hurts.shatterbyte.reanimal.world.entity.ostrich.OstrichEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class OstrichSaddleLayer extends GeoRenderLayer<OstrichEntity> {
    public OstrichSaddleLayer(GeoRenderer<OstrichEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, OstrichEntity animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!animatable.isSaddled())
            return;

        var bone = bakedModel.getBone("saddle").orElse(null);

        if (bone == null)
            return;

        poseStack.pushPose();

        poseStack.translate(0F, -0.5F, 0F);

        var modelPos = bone.getModelPosition();

        poseStack.translate(modelPos.x / 16F, modelPos.y / 16F, modelPos.z / 16F);

        var saddleModel = new OstrichSaddleModel();
        var saddleRenderType = RenderType.entityCutoutNoCull(saddleModel.getTextureResource(animatable));

        this.getRenderer().reRender(saddleModel.getBakedModel(saddleModel.getModelResource(animatable)), poseStack, bufferSource, animatable, saddleRenderType, bufferSource.getBuffer(saddleRenderType), partialTick, packedLight, packedOverlay, 0xFFFFFF);

        poseStack.popPose();
    }
}