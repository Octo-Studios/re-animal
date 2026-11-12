package it.hurts.shatterbyte.reanimal.client.model.hedgehog;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class HedgehogModel extends GeoModel<HedgehogEntity> {
    private static final Identifier MODEL_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hedgehog_adult");
    private static final Identifier MODEL_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hedgehog_baby");
    private static final Identifier TEXTURE_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/hedgehog_adult.png");
    private static final Identifier TEXTURE_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/hedgehog_baby.png");
    private static final Identifier ANIMATION_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hedgehog_adult");
    private static final Identifier ANIMATION_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hedgehog_baby");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState living && living.isBaby ? MODEL_BABY : MODEL_ADULT;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState living && living.isBaby ? TEXTURE_BABY : TEXTURE_ADULT;
    }

    @Override
    public Identifier getAnimationResource(HedgehogEntity entity) {
        return entity.isBaby() ? ANIMATION_BABY : ANIMATION_ADULT;
    }
}
