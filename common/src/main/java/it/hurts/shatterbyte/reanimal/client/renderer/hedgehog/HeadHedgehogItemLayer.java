package it.hurts.shatterbyte.reanimal.client.renderer.hedgehog;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HeadHedgehogData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class HeadHedgehogItemLayer extends BlockAndItemGeoLayer<HeadHedgehogAnimatable, HeadHedgehogData, GeoRenderState> {
    public HeadHedgehogItemLayer(EntityRendererProvider.Context context, GeoRenderer<HeadHedgehogAnimatable, HeadHedgehogData, GeoRenderState> renderer) {
        super(context, renderer);
    }

    @Override
    protected List<RenderData> getRelevantBones(HeadHedgehogAnimatable animatable, @Nullable HeadHedgehogData data, GeoRenderState renderState, float partialTick) {
        if (data == null || data.stack().isEmpty())
            return List.of();

        var itemState = new ItemStackRenderState();

        this.itemModelResolver.updateForTopItem(itemState, data.stack(), ItemDisplayContext.FIXED, Minecraft.getInstance().level, null, 0);

        return List.of(RenderData.item("item", ItemDisplayContext.FIXED, itemState));
    }

    @Override
    public void addRenderData(HeadHedgehogAnimatable animatable, @Nullable HeadHedgehogData data, GeoRenderState renderState, float partialTick) {
        renderState.addGeckolibData(CONTENTS, this.getRelevantBones(animatable, data, renderState, partialTick));
    }

    @Override
    protected void submitItemStackRender(PoseStack poseStack, GeoBone bone, ItemStackRenderState itemState, ItemDisplayContext displayContext, GeoRenderState renderState, SubmitNodeCollector renderTasks, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);

        super.submitItemStackRender(poseStack, bone, itemState, displayContext, renderState, renderTasks, packedLight);

        poseStack.popPose();
    }
}
