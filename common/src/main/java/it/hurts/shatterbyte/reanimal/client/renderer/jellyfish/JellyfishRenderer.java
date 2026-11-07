package it.hurts.shatterbyte.reanimal.client.renderer.jellyfish;

import com.mojang.math.Axis;
import it.hurts.shatterbyte.reanimal.client.model.jellyfish.JellyfishModel;
import it.hurts.shatterbyte.reanimal.client.renderer.SoftGlowLayer;
import it.hurts.shatterbyte.reanimal.common.entity.jellyfish.JellyfishEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;

public class JellyfishRenderer extends GeoEntityRenderer<JellyfishEntity, EntityRenderState> {
    public JellyfishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new JellyfishModel());

        this.withRenderLayer(new SoftGlowLayer(this,
                new SoftGlowLayer.Glow("body", 0F, 1.2F, 0.44F),
                new SoftGlowLayer.Glow("body", 0F, 0.7F, 0.28F),
                new SoftGlowLayer.Glow("main_tentacles", -0.3125F, 0.55F, 0.07F),
                new SoftGlowLayer.Glow("main_tentacles", -0.4375F, 0.55F, 0.07F),
                new SoftGlowLayer.Glow("main_tentacles", -0.5625F, 0.55F, 0.07F),
                new SoftGlowLayer.Glow("main_tentacles", -0.6875F, 0.55F, 0.07F),
                new SoftGlowLayer.Glow("main_tentacles", -0.8125F, 0.55F, 0.07F),
                new SoftGlowLayer.Glow("main_tentacles", -0.9375F, 0.55F, 0.07F),
                new SoftGlowLayer.Glow("main_tentacles", -1.0625F, 0.55F, 0.07F),
                new SoftGlowLayer.Glow("main_tentacles", -1.1875F, 0.55F, 0.07F),
                new SoftGlowLayer.Glow("main_tentacles", -1.3125F, 0.55F, 0.07F)));

        this.shadowRadius = 0.3F;
    }

    @Override
    public void extractRenderState(JellyfishEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        renderState.lightCoords = LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo renderPassInfo) {
        super.adjustRenderPose(renderPassInfo);

        renderPassInfo.poseStack().translate(0, 0.5F, 0);

        if (renderPassInfo.renderState() instanceof LivingEntityRenderState les)
            renderPassInfo.poseStack().rotateDegrees(Axis.XP, -les.xRot - 90F);
    }
}
