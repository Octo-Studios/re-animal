package it.hurts.shatterbyte.reanimal.client.renderer.toucan;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.hurts.shatterbyte.reanimal.common.entity.toucan.ToucanEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.List;

public class ToucanFoodLayer<R extends EntityRenderState & GeoRenderState> extends ItemInHandGeoLayer<ToucanEntity, Void, R> {
    public ToucanFoodLayer(EntityRendererProvider.Context context, GeoRenderer<ToucanEntity, Void, R> renderer) {
        super(context, renderer, "food", null);
    }

    @Override
    protected List<RenderData> getRelevantBones(ToucanEntity entity, Void relatedObject, R renderState, float partialTick) {
        if (!entity.isEating() || entity.getMainHandItem().isEmpty())
            return List.of();

        return List.of(RenderData.item("food", ItemDisplayContext.NONE,
                RenderUtil.createRenderStateForItem(entity.getMainHandItem(), this.itemModelResolver, ItemDisplayContext.NONE, entity)));
    }

    @Override
    protected void submitItemStackRender(PoseStack poseStack, GeoBone bone, ItemStackRenderState stackState, ItemDisplayContext displayContext, R renderState, SubmitNodeCollector renderTasks, int packedLight) {
        var bounds = stackState.getModelBoundingBox();
        var center = bounds.getCenter();
        var scale = (float)(0.25D / Math.max(bounds.getXsize(), Math.max(bounds.getYsize(), bounds.getZsize())));

        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 90F);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-center.x, -center.y, -center.z);

        super.submitItemStackRender(poseStack, bone, stackState, displayContext, renderState, renderTasks, packedLight);

        poseStack.popPose();
    }
}
