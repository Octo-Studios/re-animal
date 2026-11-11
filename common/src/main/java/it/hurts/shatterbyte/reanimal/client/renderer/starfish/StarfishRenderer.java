package it.hurts.shatterbyte.reanimal.client.renderer.starfish;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.reanimal.client.model.starfish.StarfishModel;
import it.hurts.shatterbyte.reanimal.common.entity.starfish.StarfishEntity;
import it.hurts.shatterbyte.reanimal.common.entity.starfish.StarfishNavigation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.Vec3;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class StarfishRenderer extends GeoEntityRenderer<StarfishEntity, EntityRenderState> {
    private static final DataTicket<Quaternionf> SURFACE_ROTATION = DataTicket.create("reanimal_starfish_surface_rotation", Quaternionf.class);
    private static final DataTicket<Vec3> WALL_OFFSET = DataTicket.create("reanimal_starfish_wall_offset", Vec3.class);

    public StarfishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new StarfishModel());
    }

    @Override
    public void extractRenderState(StarfishEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        var geoState = (GeoRenderState) renderState;
        var rotation = entity.getSurfaceRotation(partialTick);
        var side = entity.getClimbingSide();
        var wallOffset = Vec3.ZERO;

        if (side.getAxis().isHorizontal()) {
            var position = new Vec3(renderState.x, renderState.y, renderState.z);
            var alignment = -rotation.transformPositiveY(new Vector3f()).dot(side.getUnitVec3f());
            wallOffset = ((StarfishNavigation) entity.getNavigation()).getSupportOffset(position, side).scale(Math.max(0F, alignment));
        }

        geoState.addGeckolibData(SURFACE_ROTATION, rotation);
        geoState.addGeckolibData(WALL_OFFSET, wallOffset);
    }

    @Override
    protected void applyRotations(RenderPassInfo renderPassInfo, PoseStack poseStack, float nativeScale) {
        var state = (LivingEntityRenderState) renderPassInfo.renderState();
        var geoState = (GeoRenderState) state;
        var rotation = geoState.getGeckolibData(SURFACE_ROTATION);
        var wallOffset = geoState.getGeckolibData(WALL_OFFSET);
        var up = rotation.transformPositiveY(new Vector3f());
        var scale = nativeScale * (state.isBaby ? 0.5F : 1F);
        var halfHeight = state.boundingBoxHeight * 0.5F / scale;
        var centerHeight = halfHeight * Math.abs(up.y) + state.boundingBoxWidth * 0.5F / scale * (1F - Math.abs(up.y));

        poseStack.translate(wallOffset.x / scale - up.x * halfHeight, centerHeight - up.y * halfHeight, wallOffset.z / scale - up.z * halfHeight);
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
