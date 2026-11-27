package it.hurts.shatterbyte.reanimal.client.model.giraffe;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.giraffe.GiraffeEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class GiraffeModel extends GeoModel<GiraffeEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "giraffe");
    private static final Identifier MODEL_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "giraffe_baby");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/giraffe.png");
    private static final Identifier TEXTURE_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/giraffe_baby.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "giraffe");
    private static final Identifier ANIMATION_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "giraffe_baby");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState living && living.isBaby ? MODEL_BABY : MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState living && living.isBaby ? TEXTURE_BABY : TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(GiraffeEntity entity) {
        return entity.isBaby() ? ANIMATION_BABY : ANIMATION;
    }
}
