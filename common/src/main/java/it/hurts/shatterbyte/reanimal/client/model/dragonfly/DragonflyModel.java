package it.hurts.shatterbyte.reanimal.client.model.dragonfly;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.dragonfly.DragonflyEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class DragonflyModel extends GeoModel<DragonflyEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "dragonfly");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "dragonfly");
    private static final DataTicket<Integer> VARIANT = DataTicket.create("dragonfly_variant", Integer.class);

    @Override
    public void addAdditionalStateData(DragonflyEntity entity, Object extraData, GeoRenderState renderState) {
        renderState.addGeckolibData(VARIANT, entity.getVariant());
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/dragonfly_" + renderState.getOrDefaultGeckolibData(VARIANT, 0) + ".png");
    }

    @Override
    public Identifier getAnimationResource(DragonflyEntity entity) {
        return ANIMATION;
    }
}
