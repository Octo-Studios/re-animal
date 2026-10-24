package it.hurts.shatterbyte.reanimal.client.model.red_panda;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.red_panda.RedPandaEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class RedPandaModel extends GeoModel<RedPandaEntity> {
    private static final Identifier MODEL_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "red_panda_baby");
    private static final Identifier MODEL_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "red_panda_adult");
    private static final Identifier TEXTURE_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/red_panda_baby.png");
    private static final Identifier TEXTURE_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/red_panda_adult.png");
    private static final Identifier ANIMATION_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "red_panda_baby");
    private static final Identifier ANIMATION_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "red_panda_adult");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState les && les.isBaby ? MODEL_BABY : MODEL_ADULT;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState les && les.isBaby ? TEXTURE_BABY : TEXTURE_ADULT;
    }

    @Override
    public Identifier getAnimationResource(RedPandaEntity entity) {
        return entity.isBaby() ? ANIMATION_BABY : ANIMATION_ADULT;
    }
}
