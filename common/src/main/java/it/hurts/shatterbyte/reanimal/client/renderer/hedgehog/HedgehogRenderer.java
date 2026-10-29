package it.hurts.shatterbyte.reanimal.client.renderer.hedgehog;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import it.hurts.shatterbyte.reanimal.client.model.hedgehog.HedgehogModel;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.client.renderer.HeadTracking;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class HedgehogRenderer extends GeoEntityRenderer<HedgehogEntity, EntityRenderState> {
    public HedgehogRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new HedgehogModel());

        this.withRenderLayer(new HedgehogItemLayer(renderManager, this));
    }

    @Override
    public boolean shouldRender(HedgehogEntity entity, Frustum frustum, double x, double y, double z, float partialTick) {
        if (entity.getVehicle() instanceof Player)
            return false;

        return super.shouldRender(entity, frustum, x, y, z, partialTick);
    }

    public EntityRenderState createHeadRenderState(HedgehogEntity entity, float partialTick, int packedLight) {
        var renderState = this.createRenderState(entity, partialTick);

        renderState.lightCoords = packedLight;
        renderState.nameTag = null;
        renderState.scoreText = null;
        renderState.leashStates = List.of();
        renderState.shadowPieces.clear();
        renderState.shadowRadius = 0F;

        ((GeoRenderState) renderState).addGeckolibData(DataTickets.ENTITY_BODY_YAW, 180F);

        return renderState;
    }

    @Override
    public void scaleModelForRender(RenderPassInfo renderPassInfo, float widthScale, float heightScale) {
        boolean isBaby = renderPassInfo.renderState() instanceof LivingEntityRenderState les && les.isBaby;
        if (isBaby) {
            renderPassInfo.poseStack().scale(0.5F, 0.5F, 0.5F);

            this.shadowRadius = 0.35F * 0.5F;
        } else {
            renderPassInfo.poseStack().scale(1F, 1F, 1F);

            this.shadowRadius = 0.35F;
        }

        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);
    }

    @Override
    public void extractRenderState(HedgehogEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        HeadTracking.setMode(renderState, entity.getVehicle() instanceof Player ? HeadTracking.RESET : HeadTracking.FOLLOW);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        HeadTracking.apply(renderPassInfo, snapshots, true);
    }
}
