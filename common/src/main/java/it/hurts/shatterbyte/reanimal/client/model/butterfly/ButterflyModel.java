package it.hurts.shatterbyte.reanimal.client.model.butterfly;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.butterfly.ButterflyEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class ButterflyModel extends GeoModel<ButterflyEntity> {
    private static final Identifier ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "butterfly");
    private static final Identifier BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "butterfly_baby");
    private static final DataTicket<Integer> VARIANT = DataTicket.create("butterfly_variant", Integer.class);

    @Override
    public void addAdditionalStateData(ButterflyEntity entity, Object extraData, GeoRenderState renderState) {
        renderState.addGeckolibData(VARIANT, entity.getVariant());
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState living && living.isBaby ? BABY : ADULT;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        var prefix = renderState instanceof LivingEntityRenderState living && living.isBaby ? "butterfly_baby_" : "butterfly_";
        return Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/" + prefix + renderState.getOrDefaultGeckolibData(VARIANT, 0) + ".png");
    }

    @Override
    public Identifier getAnimationResource(ButterflyEntity entity) {
        return entity.isBaby() ? BABY : ADULT;
    }
}
