package it.hurts.shatterbyte.reanimal.client.renderer.hedgehog;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.List;

public class HedgehogItemLayer<R extends EntityRenderState & GeoRenderState> extends BlockAndItemGeoLayer<HedgehogEntity, Void, R> {
    public HedgehogItemLayer(EntityRendererProvider.Context context, GeoRenderer<HedgehogEntity, Void, R> renderer) {
        super(context, renderer);
    }

    @Override
    protected List<RenderData> getRelevantBones(HedgehogEntity animatable, Void relatedObject, R renderState, float partialTick) {
        var stack = animatable.getStack();

        if (animatable.isDeadOrDying() || stack.isEmpty())
            return List.of();

        var itemState = new ItemStackRenderState();

        this.itemModelResolver.updateForLiving(itemState, stack, ItemDisplayContext.FIXED, animatable);

        return List.of(RenderData.item("item", ItemDisplayContext.FIXED, itemState));
    }

    @Override
    public void addRenderData(HedgehogEntity animatable, Void relatedObject, R renderState, float partialTick) {
        renderState.addGeckolibData(CONTENTS, this.getRelevantBones(animatable, relatedObject, renderState, partialTick));
    }

    @Override
    protected void submitItemStackRender(PoseStack poseStack, GeoBone bone, ItemStackRenderState itemState, ItemDisplayContext displayContext, R renderState, SubmitNodeCollector renderTasks, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);

        super.submitItemStackRender(poseStack, bone, itemState, displayContext, renderState, renderTasks, packedLight);

        poseStack.popPose();
    }
}
