package it.hurts.shatterbyte.reanimal.client.renderer.pigeon;

import com.geckolib.renderer.base.GeoRenderState;
import it.hurts.shatterbyte.reanimal.client.model.pigeon.PigeonModel;
import it.hurts.shatterbyte.reanimal.client.renderer.MobPerch;
import it.hurts.shatterbyte.reanimal.common.entity.pigeon.PigeonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.client.renderer.HeadTracking;
import org.joml.Matrix4f;

public class PigeonRenderer extends GeoEntityRenderer<PigeonEntity, EntityRenderState> {
    public PigeonRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PigeonModel());
    }

    @Override
    public void scaleModelForRender(RenderPassInfo renderPassInfo, float widthScale, float heightScale) {
        var isBaby = renderPassInfo.renderState() instanceof LivingEntityRenderState les && les.isBaby;

        if (isBaby) {
            renderPassInfo.poseStack().scale(0.5F, 0.5F, 0.5F);

            this.shadowRadius = 0.35F * 0.5F;
        } else {
            renderPassInfo.poseStack().scale(1F, 1F, 1F);

            this.shadowRadius = 0.35F;
        }

        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);
    }

    @Override
    public void extractRenderState(PigeonEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        var transform = MobPerch.extract(entity, renderState, partialTick, entity.getPerchFootSpan());

        ((GeoRenderState) renderState).addGeckolibData(MobPerch.TRANSFORM, transform);
        HeadTracking.setMode(renderState, entity.isPerched() ? HeadTracking.RESET : entity.isPecking() ? HeadTracking.KEEP_ANIMATION : HeadTracking.FOLLOW);
    }

    @Override
    public void preRenderPass(RenderPassInfo renderPassInfo, SubmitNodeCollector renderTasks) {
        super.preRenderPass(renderPassInfo, renderTasks);

        var transform = (Matrix4f) renderPassInfo.renderState().getGeckolibData(MobPerch.TRANSFORM);

        if (transform != null)
            renderPassInfo.poseStack().mulPose(transform);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo renderPassInfo) {
        if (renderPassInfo.renderState().getGeckolibData(MobPerch.TRANSFORM) == null)
            super.adjustRenderPose(renderPassInfo);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        HeadTracking.apply(renderPassInfo, snapshots, true);
    }
}
