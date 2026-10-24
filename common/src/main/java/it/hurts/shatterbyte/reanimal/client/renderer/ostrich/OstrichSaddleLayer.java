package it.hurts.shatterbyte.reanimal.client.renderer.ostrich;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import it.hurts.shatterbyte.reanimal.client.model.ostrich.OstrichSaddleModel;
import it.hurts.shatterbyte.reanimal.common.entity.ostrich.OstrichEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import java.util.function.BiConsumer;

public class OstrichSaddleLayer<R extends EntityRenderState & GeoRenderState> extends GeoRenderLayer<OstrichEntity, Void, R> {
    private static final DataTicket<Boolean> SADDLED = DataTicket.create("reanimal_ostrich_saddled", Boolean.class);

    private final OstrichSaddleModel saddleModel = new OstrichSaddleModel();

    public OstrichSaddleLayer(GeoRenderer<OstrichEntity, Void, R> renderer) {
        super(renderer);
    }

    @Override
    public void addRenderData(OstrichEntity animatable, Void relatedObject, R renderState, float partialTick) {
        renderState.addGeckolibData(SADDLED, animatable.isSaddled());
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<R> renderPassInfo, BiConsumer<GeoBone, PerBoneRender<R>> consumer) {
        if (!renderPassInfo.getOrDefaultGeckolibData(SADDLED, false))
            return;

        renderPassInfo.model().getBone("saddle").ifPresent(bone -> consumer.accept(bone, this::submitSaddle));
    }

    private void submitSaddle(RenderPassInfo<R> renderPassInfo, GeoBone bone, SubmitNodeCollector renderTasks) {
        var renderState = renderPassInfo.renderState();
        var bakedModel = this.saddleModel.getBakedModel(this.saddleModel.getModelResource(renderState));
        var renderType = RenderTypes.entityCutout(this.saddleModel.getTextureResource(renderState));
        var poseStack = renderPassInfo.poseStack();
        var packedLight = renderPassInfo.packedLight();
        var packedOverlay = renderPassInfo.packedOverlay();
        var renderColor = renderPassInfo.renderColor();

        poseStack.pushPose();
        poseStack.translate(0F, -0.5F, 0F);

        renderTasks.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
            poseStack.pushPose();
            poseStack.last().set(pose);

            bakedModel.render(renderPassInfo, vertexConsumer, packedLight, packedOverlay, renderColor);

            poseStack.popPose();
        });

        poseStack.popPose();
    }
}
