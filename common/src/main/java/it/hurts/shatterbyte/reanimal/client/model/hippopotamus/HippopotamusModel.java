package it.hurts.shatterbyte.reanimal.client.model.hippopotamus;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.hippopotamus.HippopotamusEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class HippopotamusModel extends GeoModel<HippopotamusEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hippopotamus");
    private static final Identifier MODEL_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hippopotamus_baby");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/hippopotamus.png");
    private static final Identifier TEXTURE_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/hippopotamus_baby.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hippopotamus");
    private static final Identifier ANIMATION_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hippopotamus_baby");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState living && living.isBaby ? MODEL_BABY : MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState living && living.isBaby ? TEXTURE_BABY : TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(HippopotamusEntity entity) {
        return entity.isBaby() ? ANIMATION_BABY : ANIMATION;
    }
}
