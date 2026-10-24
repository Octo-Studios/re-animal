package it.hurts.shatterbyte.reanimal.client.renderer.red_panda;

import it.hurts.shatterbyte.reanimal.client.model.red_panda.RedPandaModel;
import it.hurts.shatterbyte.reanimal.common.entity.red_panda.RedPandaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.client.renderer.HeadTracking;

public class RedPandaRenderer extends GeoEntityRenderer<RedPandaEntity, EntityRenderState> {
    public RedPandaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RedPandaModel());
    }

    @Override
    public void scaleModelForRender(RenderPassInfo renderPassInfo, float widthScale, float heightScale) {
        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);

        boolean isBaby = renderPassInfo.renderState() instanceof LivingEntityRenderState les && les.isBaby;
        if (isBaby)
            this.shadowRadius = 0.5F * 0.5F;
        else
            this.shadowRadius = 0.5F;
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        HeadTracking.apply(renderPassInfo, snapshots, false);
    }
}
