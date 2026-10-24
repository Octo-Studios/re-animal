package it.hurts.shatterbyte.reanimal.client.model.butterfly;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.butterfly.ButterflyEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class ButterflyModel extends GeoModel<ButterflyEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "butterfly");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "butterfly");
    private static final DataTicket<Integer> VARIANT = DataTicket.create("butterfly_variant", Integer.class);

    @Override
    public void addAdditionalStateData(ButterflyEntity entity, Object extraData, GeoRenderState renderState) {
        renderState.addGeckolibData(VARIANT, entity.getVariant());
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/butterfly_" + renderState.getOrDefaultGeckolibData(VARIANT, 0) + ".png");
    }

    @Override
    public Identifier getAnimationResource(ButterflyEntity entity) {
        return ANIMATION;
    }
}
