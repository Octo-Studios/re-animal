package it.hurts.shatterbyte.reanimal.client.model.jellyfish;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.jellyfish.JellyfishEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class JellyfishModel extends GeoModel<JellyfishEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "jellyfish");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "jellyfish");
    private static final DataTicket<Integer> VARIANT = DataTicket.create("jellyfish_variant", Integer.class);

    @Override
    public void addAdditionalStateData(JellyfishEntity entity, Object extraData, GeoRenderState renderState) {
        renderState.addGeckolibData(VARIANT, entity.getVariant());
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/jellyfish_" + renderState.getOrDefaultGeckolibData(VARIANT, 0) + ".png");
    }

    @Override
    public Identifier getAnimationResource(JellyfishEntity entity) {
        return ANIMATION;
    }
}
