package it.hurts.shatterbyte.reanimal.client.renderer.crocodile;

import com.mojang.math.Axis;
import it.hurts.shatterbyte.reanimal.client.model.crocodile.CrocodileModel;
import it.hurts.shatterbyte.reanimal.common.entity.crocodile.CrocodileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.client.renderer.HeadTracking;

public class CrocodileRenderer extends GeoEntityRenderer<CrocodileEntity, EntityRenderState> {
    public CrocodileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CrocodileModel());
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

        renderPassInfo.poseStack().rotateDegrees(Axis.XP, -les.xRot);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        HeadTracking.apply(renderPassInfo, snapshots, true);
    }
}
