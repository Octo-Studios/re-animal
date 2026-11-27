package it.hurts.shatterbyte.reanimal.client.renderer.giraffe;

import it.hurts.shatterbyte.reanimal.client.model.giraffe.GiraffeModel;
import it.hurts.shatterbyte.reanimal.common.entity.giraffe.GiraffeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.client.renderer.HeadTracking;

public class GiraffeRenderer extends GeoEntityRenderer<GiraffeEntity, EntityRenderState> {
    public GiraffeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GiraffeModel());
    }

    @Override
    public void extractRenderState(GiraffeEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        HeadTracking.setMode(renderState, entity.isGrazing() || entity.isKicking() ? HeadTracking.KEEP_ANIMATION : HeadTracking.FOLLOW);
    }

    @Override
    protected float getShadowRadius(EntityRenderState renderState) {
        return renderState instanceof LivingEntityRenderState living && living.isBaby ? 0.4F : 1F;
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        HeadTracking.apply(renderPassInfo, snapshots, true);
    }
}
