package it.hurts.shatterbyte.reanimal.client.renderer.glow_stick;

import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import it.hurts.shatterbyte.reanimal.client.model.glow_stick.GlowStickModel;
import it.hurts.shatterbyte.reanimal.common.entity.glow_stick.GlowStickEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;

public class GlowStickRenderer extends GeoEntityRenderer<GlowStickEntity, EntityRenderState> {
    private static final DataTicket<Float> GLOW_YAW = DataTicket.create("reanimal_glow_yaw", Float.class);
    private static final DataTicket<Float> GLOW_HEIGHT = DataTicket.create("reanimal_glow_height", Float.class);
    private static final DataTicket<Float> GLOW_TUMBLE = DataTicket.create("reanimal_glow_tumble", Float.class);
    private static final DataTicket<Integer> GLOW_FACE = DataTicket.create("reanimal_glow_face", Integer.class);

    public GlowStickRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GlowStickModel());
    }

    @Override
    public void extractRenderState(GlowStickEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        var geoState = (GeoRenderState) renderState;
        geoState.addGeckolibData(GLOW_YAW, entity.getRenderYaw(partialTick));
        geoState.addGeckolibData(GLOW_HEIGHT, entity.getRenderHeight(partialTick));
        geoState.addGeckolibData(GLOW_TUMBLE, entity.getRenderTumble(partialTick));
        geoState.addGeckolibData(GLOW_FACE, entity.getStuckFace() == null ? -1 : entity.getStuckFace().get3DDataValue());
        geoState.addGeckolibData(GlowStickModel.ITEM_NAME, BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()).getPath());
    }

    @Override
    public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutoutCull(texture);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo renderPassInfo) {
        var geoState = (GeoRenderState) renderPassInfo.renderState();
        var yaw = geoState.getOrDefaultGeckolibData(GLOW_YAW, 0F);
        var tumble = geoState.getOrDefaultGeckolibData(GLOW_TUMBLE, 90F);
        var height = geoState.getOrDefaultGeckolibData(GLOW_HEIGHT, 0.07F);

        var face = geoState.getOrDefaultGeckolibData(GLOW_FACE, -1);

        if (face >= 0) {
            var direction = Direction.from3DDataValue(face);
            var normal = direction.getUnitVec3i();

            if (direction == Direction.DOWN)
                renderPassInfo.poseStack().translate(0.0F, GlowStickEntity.SIZE + 0.01F - height, 0.0F);
            else
                renderPassInfo.poseStack().translate(normal.getX() * height, normal.getY() * height, normal.getZ() * height);

            switch (direction) {
                case DOWN -> renderPassInfo.poseStack().rotateDegrees(Axis.XP, 180.0F);
                case SOUTH -> renderPassInfo.poseStack().rotateDegrees(Axis.XP, 90.0F);
                case NORTH -> renderPassInfo.poseStack().rotateDegrees(Axis.XP, -90.0F);
                case EAST -> renderPassInfo.poseStack().rotateDegrees(Axis.ZP, -90.0F);
                case WEST -> renderPassInfo.poseStack().rotateDegrees(Axis.ZP, 90.0F);
                default -> {
                }
            }
        } else
            renderPassInfo.poseStack().translate(0.0F, height, 0.0F);

        renderPassInfo.poseStack().rotateDegrees(Axis.YP, yaw);
        renderPassInfo.poseStack().rotateDegrees(Axis.XP, tumble);
        renderPassInfo.poseStack().translate(0.0F, -0.3125F, 0.0F);
    }
}
