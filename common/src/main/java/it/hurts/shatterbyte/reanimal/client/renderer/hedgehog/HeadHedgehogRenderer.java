package it.hurts.shatterbyte.reanimal.client.renderer.hedgehog;

import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.client.model.hedgehog.HeadHedgehogModel;
import it.hurts.shatterbyte.reanimal.client.model.hedgehog.HedgehogModel;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HeadHedgehogData;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class HeadHedgehogRenderer extends GeoObjectRenderer<HeadHedgehogAnimatable, HeadHedgehogData, GeoRenderState> {
    public HeadHedgehogRenderer(EntityRendererProvider.Context context) {
        super(new HeadHedgehogModel());

        this.withRenderLayer(new HeadHedgehogItemLayer(context, this));
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<GeoRenderState> renderPassInfo, float widthScale, float heightScale) {
        var scale = renderPassInfo.getOrDefaultGeckolibData(HedgehogModel.IS_BABY, false) ? 0.5F : 1F;

        super.scaleModelForRender(renderPassInfo, widthScale * scale, heightScale * scale);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> renderPassInfo, BoneSnapshots snapshots) {
        snapshots.ifPresent("body_2", snapshot -> snapshot.setScale(0F, 0F, 0F));
    }
}
