package it.hurts.shatterbyte.reanimal.client.renderer.ostrich;

import it.hurts.shatterbyte.reanimal.client.model.ostrich.OstrichModel;
import it.hurts.shatterbyte.reanimal.common.entity.ostrich.OstrichEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.client.renderer.HeadTracking;

public class OstrichRenderer extends GeoEntityRenderer<OstrichEntity, EntityRenderState> {
    public OstrichRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new OstrichModel());

        this.withRenderLayer(new OstrichSaddleLayer(this));
    }

    @Override
    public void scaleModelForRender(RenderPassInfo renderPassInfo, float widthScale, float heightScale) {
        boolean isBaby = renderPassInfo.renderState() instanceof LivingEntityRenderState les && les.isBaby;
        if (isBaby) {
            renderPassInfo.poseStack().scale(0.5F, 0.5F, 0.5F);

            this.shadowRadius = 0.75F * 0.5F;
        } else {
            renderPassInfo.poseStack().scale(1F, 1F, 1F);

            this.shadowRadius = 0.75F;
        }

        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        HeadTracking.apply(renderPassInfo, snapshots, true);
    }
}
