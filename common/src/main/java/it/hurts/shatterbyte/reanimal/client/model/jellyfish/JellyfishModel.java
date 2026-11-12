package it.hurts.shatterbyte.reanimal.client.model.jellyfish;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.jellyfish.JellyfishEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class JellyfishModel extends GeoModel<JellyfishEntity> {
    private static final Identifier ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "jellyfish_adult");
    private static final Identifier BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "jellyfish_baby");
    private static final DataTicket<Integer> VARIANT = DataTicket.create("jellyfish_variant", Integer.class);

    @Override
    public void addAdditionalStateData(JellyfishEntity entity, Object extraData, GeoRenderState renderState) {
        renderState.addGeckolibData(VARIANT, entity.getVariant());
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState living && living.isBaby ? BABY : ADULT;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        var prefix = renderState instanceof LivingEntityRenderState living && living.isBaby ? "jellyfish_baby_" : "jellyfish_";
        return Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/" + prefix + renderState.getOrDefaultGeckolibData(VARIANT, 0) + ".png");
    }

    @Override
    public Identifier getAnimationResource(JellyfishEntity entity) {
        return entity.isBaby() ? BABY : ADULT;
    }
}
