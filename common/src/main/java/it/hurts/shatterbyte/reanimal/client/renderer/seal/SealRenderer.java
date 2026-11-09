package it.hurts.shatterbyte.reanimal.client.renderer.seal;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import it.hurts.shatterbyte.reanimal.client.model.seal.SealModel;
import it.hurts.shatterbyte.reanimal.client.renderer.MobPerch;
import it.hurts.shatterbyte.reanimal.common.entity.seal.SealEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.client.renderer.HeadTracking;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SealRenderer extends GeoEntityRenderer<SealEntity, EntityRenderState> {
    private static final DataTicket<Float> SWIM_PITCH = DataTicket.create("reanimal_seal_swim_pitch", Float.class);
    private static final DataTicket<Float> SWIM_ROLL = DataTicket.create("reanimal_seal_swim_roll", Float.class);

    public SealRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SealModel());
    }

    @Override
    public void scaleModelForRender(RenderPassInfo renderPassInfo, float widthScale, float heightScale) {
        var isBaby = renderPassInfo.renderState() instanceof LivingEntityRenderState les && les.isBaby;
        if (isBaby) {
            renderPassInfo.poseStack().scale(0.5F, 0.5F, 0.5F);

            this.shadowRadius = 0.5F * 0.5F;
        } else {
            renderPassInfo.poseStack().scale(1F, 1F, 1F);

            this.shadowRadius = 0.5F;
        }

        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);
    }

    @Override
    public void extractRenderState(SealEntity entity, EntityRenderState renderState, float partialTick) {
        ((GeoRenderState) renderState).addGeckolibData(SealEntity.SLAP_PARTICLE, false);

        super.extractRenderState(entity, renderState, partialTick);

        ((GeoRenderState) renderState).addGeckolibData(SWIM_PITCH, entity.getSwimPitch(partialTick));
        ((GeoRenderState) renderState).addGeckolibData(SWIM_ROLL, entity.getSwimRoll(partialTick));
        ((GeoRenderState) renderState).addGeckolibData(MobPerch.TRANSFORM,
                entity.getVehicle() instanceof SealEntity ? MobPerch.extract(entity, renderState, partialTick, 0F, "body_1") : null);
    }

    @Override
    public void preRenderPass(RenderPassInfo renderPassInfo, SubmitNodeCollector renderTasks) {
        super.preRenderPass(renderPassInfo, renderTasks);

        var transform = (Matrix4f) renderPassInfo.renderState().getGeckolibData(MobPerch.TRANSFORM);

        if (transform != null)
            renderPassInfo.poseStack().mulPose(transform);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo renderPassInfo) {
        if (renderPassInfo.renderState().getGeckolibData(MobPerch.TRANSFORM) == null)
            super.adjustRenderPose(renderPassInfo);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        if (renderPassInfo.renderState().getGeckolibData(MobPerch.TRANSFORM) != null)
            return;

        if (renderPassInfo.renderState().getOrDefaultGeckolibData(SealEntity.SLAP_PARTICLE, false))
            renderPassInfo.addLocatorPositionListener("slap", (worldPos, modelPos, localPos) ->
                    Minecraft.getInstance().level.addParticle(ReAnimalParticleTypes.BALL_IMPACT,
                            worldPos.x, worldPos.y, worldPos.z, 0D, 0.25D, 0D));

        var pitch = (float) renderPassInfo.renderState().getGeckolibData(SWIM_PITCH);
        var roll = (float) renderPassInfo.renderState().getGeckolibData(SWIM_ROLL) * Mth.DEG_TO_RAD;

        snapshots.ifPresent("body_1", snapshot -> {
            var rotation = new Quaternionf().rotationX(pitch * Mth.DEG_TO_RAD).rotateZ(roll)
                    .rotateZYX(snapshot.getRotZ(), snapshot.getRotY(), snapshot.getRotX()).getEulerAnglesZYX(new Vector3f());

            snapshot.setRotX(rotation.x);
            snapshot.setRotY(rotation.y);
            snapshot.setRotZ(rotation.z);
        });

        if (renderPassInfo.renderState() instanceof LivingEntityRenderState living && living.isInWater) {
            snapshots.ifPresent("head", snapshot -> {
                var yaw = -Mth.clamp(living.yRot * 0.5F, -25F, 25F) * Mth.DEG_TO_RAD;
                var headPitch = Mth.clamp((-living.xRot - pitch) * 0.5F, -12F, 12F) * Mth.DEG_TO_RAD;

                snapshot.setRotY(snapshot.getRotY() + yaw * Mth.cos(roll) - headPitch * Mth.sin(roll));
                snapshot.setRotX(snapshot.getRotX() + headPitch * Mth.cos(roll) + yaw * Mth.sin(roll));
            });
        } else
            HeadTracking.apply(renderPassInfo, snapshots, true);
    }
}
