package it.hurts.shatterbyte.reanimal.client.renderer.dragonfly;

import it.hurts.shatterbyte.reanimal.client.model.dragonfly.DragonflyModel;
import it.hurts.shatterbyte.reanimal.common.entity.dragonfly.DragonflyEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.GeoEntityRenderer;

public class DragonflyRenderer extends GeoEntityRenderer<DragonflyEntity, EntityRenderState> {
    public DragonflyRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DragonflyModel());

        this.shadowRadius = 0.25F;
    }

    @Override
    public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }
}
