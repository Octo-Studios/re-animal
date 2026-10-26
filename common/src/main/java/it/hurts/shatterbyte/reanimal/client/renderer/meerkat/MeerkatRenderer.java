package it.hurts.shatterbyte.reanimal.client.renderer.meerkat;

import it.hurts.shatterbyte.reanimal.client.model.meerkat.MeerkatModel;
import it.hurts.shatterbyte.reanimal.common.entity.meerkat.MeerkatEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.client.renderer.HeadTracking;

public class MeerkatRenderer extends GeoEntityRenderer<MeerkatEntity, EntityRenderState> {
    public MeerkatRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MeerkatModel());
    }

    @Override
    public void scaleModelForRender(RenderPassInfo renderPassInfo, float widthScale, float heightScale) {
        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);

        boolean isBaby = renderPassInfo.renderState() instanceof LivingEntityRenderState les && les.isBaby;
        if (isBaby)
            this.shadowRadius = 0.35F * 0.5F;
        else
            this.shadowRadius = 0.35F;
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        HeadTracking.apply(renderPassInfo, snapshots, false);
    }
}
