package it.hurts.shatterbyte.reanimal.client.renderer.jellyfish;

import com.mojang.math.Axis;
import it.hurts.shatterbyte.reanimal.client.model.jellyfish.JellyfishModel;
import it.hurts.shatterbyte.reanimal.common.entity.jellyfish.JellyfishEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;

public class JellyfishRenderer extends GeoEntityRenderer<JellyfishEntity, EntityRenderState> {
    public JellyfishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new JellyfishModel());

        this.shadowRadius = 0.3F;
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
