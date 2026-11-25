package it.hurts.shatterbyte.reanimal.client.renderer.vulture;

import it.hurts.shatterbyte.reanimal.client.model.vulture.VultureModel;
import it.hurts.shatterbyte.reanimal.common.entity.vulture.VultureEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.client.renderer.HeadTracking;

public class VultureRenderer extends GeoEntityRenderer<VultureEntity, EntityRenderState> {
    private static final DataTicket<Float> BANK = DataTicket.create("reanimal_vulture_bank", Float.class);

    public VultureRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new VultureModel());
    }

    @Override
    protected float getShadowRadius(EntityRenderState renderState) {
        return renderState instanceof LivingEntityRenderState les && les.isBaby ? 0.2F : 0.5F;
    }

    @Override
    public void extractRenderState(VultureEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        ((GeoRenderState) renderState).addGeckolibData(BANK, entity.getBank(partialTick));
        HeadTracking.setMode(renderState, entity.isGliding() ? HeadTracking.KEEP_ANIMATION : HeadTracking.FOLLOW);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        HeadTracking.apply(renderPassInfo, snapshots, true);

        snapshots.ifPresent("body", snapshot -> snapshot.setRotZ(snapshot.getRotZ() + renderPassInfo.renderState().getOrDefaultGeckolibData(BANK, 0F) * Mth.DEG_TO_RAD));
    }
}
