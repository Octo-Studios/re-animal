package it.hurts.shatterbyte.reanimal.client.model.hedgehog;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class HedgehogModel extends GeoModel<HedgehogEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hedgehog");
    private static final Identifier TEXTURE_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/hedgehog_adult.png");
    private static final Identifier TEXTURE_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/hedgehog_baby.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hedgehog");
    private static final DataTicket<Boolean> IS_BABY = DataTicket.create("hedgehog_is_baby", Boolean.class);

    @Override
    public void addAdditionalStateData(HedgehogEntity entity, Object extraData, GeoRenderState renderState) {
        renderState.addGeckolibData(IS_BABY, entity.isBaby());
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState.getOrDefaultGeckolibData(IS_BABY, false) ? TEXTURE_BABY : TEXTURE_ADULT;
    }

    @Override
    public Identifier getAnimationResource(HedgehogEntity entity) {
        return ANIMATION;
    }
}
