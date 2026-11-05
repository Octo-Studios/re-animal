package it.hurts.shatterbyte.reanimal.client.renderer;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.common.entity.insect.FlyingInsectEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;

public abstract class InsectRenderer<T extends FlyingInsectEntity & GeoEntity> extends GeoEntityRenderer<T, EntityRenderState> {
    private static final DataTicket<Integer> RESTING_FACE = DataTicket.create("reanimal_insect_resting_face", Integer.class);
    private static final DataTicket<Float> REST_PROGRESS = DataTicket.create("reanimal_insect_rest_progress", Float.class);

    protected InsectRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    @Override
    public void extractRenderState(T entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        var geoState = (GeoRenderState) renderState;

        geoState.addGeckolibData(RESTING_FACE, entity.getRenderRestingFace().get3DDataValue());
        geoState.addGeckolibData(REST_PROGRESS, entity.getRestProgress(partialTick));
    }

    @Override
    public void adjustRenderPose(RenderPassInfo renderPassInfo) {
        var geoState = (GeoRenderState) renderPassInfo.renderState();
        var progress = geoState.getOrDefaultGeckolibData(REST_PROGRESS, 0.0F);

        if (progress > 0.0F) {
            var direction = Direction.from3DDataValue(geoState.getGeckolibData(RESTING_FACE));
            var state = (EntityRenderState) renderPassInfo.renderState();
            var halfHeight = state.boundingBoxHeight * 0.5F;
            var extent = direction.getAxis() == Direction.Axis.Y ? halfHeight : state.boundingBoxWidth * 0.5F;

            renderPassInfo.poseStack().translate(-direction.getStepX() * extent * progress, (halfHeight - direction.getStepY() * extent) * progress, -direction.getStepZ() * extent * progress);
            renderPassInfo.poseStack().rotate(new Quaternionf().slerp(direction.getRotation(), progress));
        }

        super.adjustRenderPose(renderPassInfo);
    }
}
