package it.hurts.shatterbyte.reanimal.client.renderer.dragonfly;

import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.client.model.dragonfly.DragonflyModel;
import it.hurts.shatterbyte.reanimal.client.renderer.InsectRenderer;
import it.hurts.shatterbyte.reanimal.common.entity.dragonfly.DragonflyEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class DragonflyRenderer extends InsectRenderer<DragonflyEntity> {
    public DragonflyRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DragonflyModel());
    }

    @Override
    public void scaleModelForRender(RenderPassInfo renderPassInfo, float widthScale, float heightScale) {
        var isBaby = renderPassInfo.renderState() instanceof LivingEntityRenderState state && state.isBaby;

        this.shadowRadius = isBaby ? 0.125F : 0.25F;

        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);
    }

    @Override
    public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }
}
