package it.hurts.shatterbyte.reanimal.client.renderer.crocodile;

import com.mojang.math.Axis;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import it.hurts.shatterbyte.reanimal.client.model.crocodile.CrocodileModel;
import it.hurts.shatterbyte.reanimal.common.entity.crocodile.CrocodileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.util.Mth;

public class CrocodileRenderer extends GeoEntityRenderer<CrocodileEntity, EntityRenderState> {
    private static final DataTicket<Float> SWIM_PITCH = DataTicket.create("reanimal_crocodile_swim_pitch", Float.class);
    private static final DataTicket<Boolean> HEAD_TRACKING = DataTicket.create("reanimal_crocodile_head_tracking", Boolean.class);

    public CrocodileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CrocodileModel());
    }

    @Override
    public void extractRenderState(CrocodileEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        ((GeoRenderState) renderState).addGeckolibData(SWIM_PITCH, entity.getSwimPitch(partialTick));

        ((GeoRenderState) renderState).addGeckolibData(HEAD_TRACKING, !entity.isAttacking() && !entity.isAmbushing()
                && !entity.isBasking() && !entity.isDashing() && entity.getGrabbedPrey() == null);
    }

    @Override
    public void scaleModelForRender(RenderPassInfo renderPassInfo, float widthScale, float heightScale) {
        boolean isBaby = renderPassInfo.renderState() instanceof LivingEntityRenderState les && les.isBaby;
        if (isBaby) {
            renderPassInfo.poseStack().scale(0.5F, 0.5F, 0.5F);

            this.shadowRadius = 0.6F * 0.5F;
        } else {
            renderPassInfo.poseStack().scale(1F, 1F, 1F);

            this.shadowRadius = 0.6F;
        }

        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo renderPassInfo) {
        super.adjustRenderPose(renderPassInfo);

        if (!(renderPassInfo.renderState() instanceof LivingEntityRenderState les) || !les.isInWater)
            return;

        renderPassInfo.poseStack().rotateDegrees(Axis.XP, (float) renderPassInfo.renderState().getGeckolibData(SWIM_PITCH));
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        if (!(renderPassInfo.renderState() instanceof LivingEntityRenderState living)
                || !(boolean) renderPassInfo.renderState().getGeckolibData(HEAD_TRACKING))
            return;

        var pitch = living.isInWater ? (float) renderPassInfo.renderState().getGeckolibData(SWIM_PITCH) : 0F;

        snapshots.ifPresent("head", snapshot -> {
            snapshot.setRotY(snapshot.getRotY() - Mth.clamp(living.yRot * 0.5F, -20F, 20F) * Mth.DEG_TO_RAD);
            snapshot.setRotX(snapshot.getRotX() + Mth.clamp((-living.xRot - pitch) * 0.5F, -8F, 8F) * Mth.DEG_TO_RAD);
        });
    }
}
