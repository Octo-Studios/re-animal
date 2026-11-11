package it.hurts.shatterbyte.reanimal.client.renderer;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.common.entity.insect.FlyingInsectEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class InsectRenderer<T extends FlyingInsectEntity & GeoEntity> extends GeoEntityRenderer<T, EntityRenderState> {
    private static final DataTicket<Quaternionf> REST_ROTATION = DataTicket.create("reanimal_insect_rest_rotation", Quaternionf.class);
    private static final DataTicket<Float> REST_PROGRESS = DataTicket.create("reanimal_insect_rest_progress", Float.class);
    private static final DataTicket<Float> REST_YAW = DataTicket.create("reanimal_insect_rest_yaw", Float.class);

    protected InsectRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    @Override
    public void extractRenderState(T entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        var geoState = (GeoRenderState) renderState;

        geoState.addGeckolibData(REST_ROTATION, entity.getRestRotation(partialTick));
        geoState.addGeckolibData(REST_PROGRESS, entity.getRestProgress(partialTick));
        geoState.addGeckolibData(REST_YAW, entity.getRestYaw(partialTick));
        geoState.addGeckolibData(MobPerch.TRANSFORM, MobPerch.extract(entity, renderState, partialTick, entity.getPerchFootSpan()));
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
        var geoState = (GeoRenderState) renderPassInfo.renderState();

        if (geoState.getGeckolibData(MobPerch.TRANSFORM) != null)
            return;

        var progress = geoState.getOrDefaultGeckolibData(REST_PROGRESS, 0.0F);

        if (progress > 0.0F) {
            var rotation = geoState.getGeckolibData(REST_ROTATION);
            var up = rotation.transformPositiveY(new Vector3f());
            var state = (LivingEntityRenderState) renderPassInfo.renderState();
            var halfHeight = state.boundingBoxHeight * 0.5F;
            var extent = Mth.lerp(Math.abs(up.y), state.boundingBoxWidth * 0.5F, halfHeight);
            var bodyYaw = (180.0F - state.bodyRot) * Mth.DEG_TO_RAD;
            var restingRotation = new Quaternionf(rotation).rotateY((180.0F - geoState.getGeckolibData(REST_YAW)) * Mth.DEG_TO_RAD);

            renderPassInfo.poseStack().translate(-up.x * extent * progress, (halfHeight - up.y * extent) * progress, -up.z * extent * progress);
            renderPassInfo.poseStack().rotate(new Quaternionf().rotationY(bodyYaw).slerp(restingRotation, progress).rotateY(-bodyYaw));
        }

        super.adjustRenderPose(renderPassInfo);
    }
}
