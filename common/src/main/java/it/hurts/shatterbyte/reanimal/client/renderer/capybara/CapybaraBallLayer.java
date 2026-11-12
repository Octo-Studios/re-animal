package it.hurts.shatterbyte.reanimal.client.renderer.capybara;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.reanimal.common.entity.capybara.CapybaraEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.List;

public class CapybaraBallLayer<R extends EntityRenderState & GeoRenderState> extends ItemInHandGeoLayer<CapybaraEntity, Void, R> {
    public CapybaraBallLayer(EntityRendererProvider.Context context, GeoRenderer<CapybaraEntity, Void, R> renderer) {
        super(context, renderer, "nose_ball", null);
    }

    @Override
    protected List<RenderData> getRelevantBones(CapybaraEntity entity, Void relatedObject, R renderState, float partialTick) {
        if (entity.isDeadOrDying() || entity.getMainHandItem().isEmpty())
            return List.of();

        return List.of(RenderData.item("nose_ball", ItemDisplayContext.NONE,
                RenderUtil.createRenderStateForItem(entity.getMainHandItem(), this.itemModelResolver, ItemDisplayContext.NONE, entity)));
    }

    @Override
    protected void submitItemStackRender(PoseStack poseStack, GeoBone bone, ItemStackRenderState stackState, ItemDisplayContext displayContext, R renderState, SubmitNodeCollector renderTasks, int packedLight) {
        var scale = renderState instanceof LivingEntityRenderState living && living.isBaby ? 0.15F : 0.35F;

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);

        super.submitItemStackRender(poseStack, bone, stackState, displayContext, renderState, renderTasks, packedLight);

        poseStack.popPose();
    }
}
