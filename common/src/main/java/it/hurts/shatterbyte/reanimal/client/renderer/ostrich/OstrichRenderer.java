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
    public void extractRenderState(OstrichEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        HeadTracking.setMode(renderState, entity.isKicking() || entity.isCourting() ? HeadTracking.KEEP_ANIMATION : HeadTracking.FOLLOW);
    }

    @Override
    protected float getShadowRadius(EntityRenderState renderState) {
        return renderState instanceof LivingEntityRenderState les && les.isBaby ? 0.25F : 0.75F;
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        HeadTracking.apply(renderPassInfo, snapshots, true);
    }
}
