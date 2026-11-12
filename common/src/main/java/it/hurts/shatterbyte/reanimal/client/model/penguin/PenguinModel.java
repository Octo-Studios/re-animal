package it.hurts.shatterbyte.reanimal.client.model.penguin;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.penguin.PenguinEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class PenguinModel extends GeoModel<PenguinEntity> {
    private static final Identifier MODEL_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "penguin_adult");
    private static final Identifier MODEL_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "penguin_baby");
    private static final Identifier TEXTURE_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/penguin_adult.png");
    private static final Identifier TEXTURE_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/penguin_baby.png");
    private static final Identifier ANIMATION_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "penguin_adult");
    private static final Identifier ANIMATION_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "penguin_baby");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState les && les.isBaby ? MODEL_BABY : MODEL_ADULT;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState les && les.isBaby ? TEXTURE_BABY : TEXTURE_ADULT;
    }

    @Override
    public Identifier getAnimationResource(PenguinEntity entity) {
        return entity.isBaby() ? ANIMATION_BABY : ANIMATION_ADULT;
    }
}
