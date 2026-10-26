package it.hurts.shatterbyte.reanimal.client.model.meerkat;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.meerkat.MeerkatEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class MeerkatModel extends GeoModel<MeerkatEntity> {
    private static final Identifier MODEL_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "meerkat_baby");
    private static final Identifier MODEL_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "meerkat_adult");
    private static final Identifier TEXTURE_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/meerkat_baby.png");
    private static final Identifier TEXTURE_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/meerkat_adult.png");
    private static final Identifier ANIMATION_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "meerkat_baby");
    private static final Identifier ANIMATION_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "meerkat_adult");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState les && les.isBaby ? MODEL_BABY : MODEL_ADULT;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState les && les.isBaby ? TEXTURE_BABY : TEXTURE_ADULT;
    }

    @Override
    public Identifier getAnimationResource(MeerkatEntity entity) {
        return entity.isBaby() ? ANIMATION_BABY : ANIMATION_ADULT;
    }
}
