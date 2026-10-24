package it.hurts.shatterbyte.reanimal.client.model.ostrich;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.ostrich.OstrichEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class OstrichModel extends GeoModel<OstrichEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "ostrich");
    private static final Identifier TEXTURE_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/ostrich_adult.png");
    private static final Identifier TEXTURE_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/ostrich_baby.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "ostrich");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState les && les.isBaby ? TEXTURE_BABY : TEXTURE_ADULT;
    }

    @Override
    public Identifier getAnimationResource(OstrichEntity entity) {
        return ANIMATION;
    }
}
