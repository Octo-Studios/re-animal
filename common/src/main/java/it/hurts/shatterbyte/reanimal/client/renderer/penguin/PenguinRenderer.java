package it.hurts.shatterbyte.reanimal.client.renderer.penguin;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import it.hurts.shatterbyte.reanimal.client.model.penguin.PenguinModel;
import it.hurts.shatterbyte.reanimal.common.entity.penguin.PenguinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.client.renderer.HeadTracking;

public class PenguinRenderer extends GeoEntityRenderer<PenguinEntity, EntityRenderState> {
    private static final DataTicket<Float> BODY_PITCH = DataTicket.create("reanimal_penguin_body_pitch", Float.class);

    public PenguinRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PenguinModel());
    }

    @Override
    public void scaleModelForRender(RenderPassInfo renderPassInfo, float widthScale, float heightScale) {
        var isBaby = renderPassInfo.renderState() instanceof LivingEntityRenderState les && les.isBaby;
        this.shadowRadius = isBaby ? 0.175F : 0.35F;

        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);
    }

    @Override
    public void extractRenderState(PenguinEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        ((GeoRenderState) renderState).addGeckolibData(BODY_PITCH, entity.getBodyPitch(partialTick));
        HeadTracking.setMode(renderState, entity.isSliding() || entity.isDiving() || entity.isInWater() ? HeadTracking.KEEP_ANIMATION : HeadTracking.FOLLOW);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        var pitch = (float) renderPassInfo.renderState().getGeckolibData(BODY_PITCH);

        snapshots.ifPresent("body", snapshot -> snapshot.setRotX(snapshot.getRotX() - pitch * Mth.DEG_TO_RAD));

        HeadTracking.apply(renderPassInfo, snapshots, true);
    }
}
