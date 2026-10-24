package it.hurts.shatterbyte.reanimal.client.renderer.glow_stick;

import com.mojang.math.Axis;
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
    private static final DataTicket<Float> GLOW_PITCH = DataTicket.create("reanimal_glow_pitch", Float.class);
    private static final DataTicket<Float> GLOW_ROLL = DataTicket.create("reanimal_glow_roll", Float.class);

    public GlowStickRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GlowStickModel());
    }

    @Override
    public void extractRenderState(GlowStickEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        var geoState = (GeoRenderState) renderState;
        geoState.addGeckolibData(GLOW_YAW, entity.getRenderYaw(partialTick));
        geoState.addGeckolibData(GLOW_PITCH, entity.getRenderPitch(partialTick));
        geoState.addGeckolibData(GLOW_ROLL, entity.getRenderRoll(partialTick));
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
        var pitch = geoState.getOrDefaultGeckolibData(GLOW_PITCH, 0F);
        var roll = geoState.getOrDefaultGeckolibData(GLOW_ROLL, 0F);

        renderPassInfo.poseStack().translate(0.0F, 0.3125F, 0.0F);
        renderPassInfo.poseStack().rotateDegrees(Axis.YP, yaw);
        renderPassInfo.poseStack().rotateDegrees(Axis.ZP, roll);
        renderPassInfo.poseStack().rotateDegrees(Axis.XP, pitch);
        renderPassInfo.poseStack().translate(0.0F, -0.3125F, 0.0F);
    }
}
