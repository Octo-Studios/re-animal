package it.hurts.shatterbyte.reanimal.client.renderer.toucan;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.client.model.toucan.ToucanModel;
import it.hurts.shatterbyte.reanimal.client.renderer.HeadTracking;
import it.hurts.shatterbyte.reanimal.common.entity.toucan.ToucanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class ToucanRenderer extends GeoEntityRenderer<ToucanEntity, EntityRenderState> {
    public ToucanRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ToucanModel());

        this.withRenderLayer(new ToucanFoodLayer(renderManager, this));
    }

    @Override
    public void scaleModelForRender(RenderPassInfo renderPassInfo, float widthScale, float heightScale) {
        var isBaby = renderPassInfo.renderState() instanceof LivingEntityRenderState living && living.isBaby;

        this.shadowRadius = isBaby ? 0.175F : 0.35F;

        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);
    }

    @Override
    public void extractRenderState(ToucanEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        HeadTracking.setMode(renderState, entity.isEating() || entity.isFlying() ? HeadTracking.KEEP_ANIMATION : HeadTracking.FOLLOW);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        snapshots.ifPresent("food", snapshot -> snapshot.skipRender(true));

        HeadTracking.apply(renderPassInfo, snapshots, true);
    }
}
