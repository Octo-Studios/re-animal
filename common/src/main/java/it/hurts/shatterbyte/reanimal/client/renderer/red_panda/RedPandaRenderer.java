package it.hurts.shatterbyte.reanimal.client.renderer.red_panda;

import it.hurts.shatterbyte.reanimal.client.model.red_panda.RedPandaModel;
import it.hurts.shatterbyte.reanimal.common.entity.red_panda.RedPandaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.client.renderer.HeadTracking;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RedPandaRenderer extends GeoEntityRenderer<RedPandaEntity, EntityRenderState> {
    private static final DataTicket<Quaternionf> SURFACE_ROTATION = DataTicket.create("reanimal_red_panda_surface_rotation", Quaternionf.class);

    public RedPandaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RedPandaModel());
    }

    @Override
    public void extractRenderState(RedPandaEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        ((GeoRenderState) renderState).addGeckolibData(SURFACE_ROTATION, entity.getSurfaceRotation(partialTick));
        HeadTracking.setMode(renderState, entity.isClimbing() || entity.isInLeaves(entity.position()) ? HeadTracking.RESET : HeadTracking.FOLLOW);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo renderPassInfo) {
        var state = (LivingEntityRenderState) renderPassInfo.renderState();
        var rotation = new Quaternionf((Quaternionf) ((GeoRenderState) state).getGeckolibData(SURFACE_ROTATION));
        var up = rotation.transform(new Vector3f(0F, 1F, 0F));
        var halfHeight = state.boundingBoxHeight * 0.5F;
        var centerHeight = halfHeight * Math.abs(up.y) + state.boundingBoxWidth * 0.5F * (1F - Math.abs(up.y));

        renderPassInfo.poseStack().translate(-up.x * halfHeight, centerHeight - up.y * halfHeight, -up.z * halfHeight);
        renderPassInfo.poseStack().rotate(rotation.rotateY(-(180F - state.bodyRot) * Mth.DEG_TO_RAD));

        super.adjustRenderPose(renderPassInfo);
    }

    @Override
    public void scaleModelForRender(RenderPassInfo renderPassInfo, float widthScale, float heightScale) {
        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);

        var isBaby = renderPassInfo.renderState() instanceof LivingEntityRenderState les && les.isBaby;

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
