package it.hurts.shatterbyte.reanimal.client.renderer.butterfly;

import com.geckolib.renderer.GeoEntityRenderer;
import it.hurts.shatterbyte.reanimal.client.model.butterfly.ButterflyModel;
import it.hurts.shatterbyte.reanimal.common.entity.butterfly.ButterflyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class ButterflyRenderer extends GeoEntityRenderer<ButterflyEntity, EntityRenderState> {
    public ButterflyRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ButterflyModel());

        this.shadowRadius = 0.25F;
    }

    @Override
    public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
