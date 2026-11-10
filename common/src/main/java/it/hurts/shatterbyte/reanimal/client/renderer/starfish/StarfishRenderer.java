package it.hurts.shatterbyte.reanimal.client.renderer.starfish;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.reanimal.client.model.starfish.StarfishModel;
import it.hurts.shatterbyte.reanimal.common.entity.starfish.StarfishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class StarfishRenderer extends GeoEntityRenderer<StarfishEntity, EntityRenderState> {
    private static final DataTicket<Quaternionf> SURFACE_ROTATION = DataTicket.create("reanimal_starfish_surface_rotation", Quaternionf.class);

    public StarfishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new StarfishModel());
    }

    @Override
    public void extractRenderState(StarfishEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        ((GeoRenderState) renderState).addGeckolibData(SURFACE_ROTATION, entity.getSurfaceRotation(partialTick));
    }

    @Override
    protected void applyRotations(RenderPassInfo renderPassInfo, PoseStack poseStack, float nativeScale) {
        var state = (LivingEntityRenderState) renderPassInfo.renderState();
        var rotation = (Quaternionf) ((GeoRenderState) state).getGeckolibData(SURFACE_ROTATION);
        var up = rotation.transformPositiveY(new Vector3f());
        var scale = nativeScale * (state.isBaby ? 0.5F : 1F);
        var halfHeight = state.boundingBoxHeight * 0.5F / scale;
        var extent = halfHeight * Math.abs(up.y) + state.boundingBoxWidth * 0.5F / scale * (1F - Math.abs(up.y));

        poseStack.translate(-up.x * extent, halfHeight - up.y * extent, -up.z * extent);
        poseStack.rotate(rotation);

        super.applyRotations(renderPassInfo, poseStack, nativeScale);
    }

    @Override
    public void scaleModelForRender(RenderPassInfo renderPassInfo, float widthScale, float heightScale) {
        boolean isBaby = renderPassInfo.renderState() instanceof LivingEntityRenderState les && les.isBaby;
        if (isBaby) {
            renderPassInfo.poseStack().scale(0.5F, 0.5F, 0.5F);

            this.shadowRadius = 0.25F * 0.5F;
        } else {
            renderPassInfo.poseStack().scale(1F, 1F, 1F);

            this.shadowRadius = 0.25F;
        }

        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);
    }
}
