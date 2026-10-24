package it.hurts.shatterbyte.reanimal.client.model.seal;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.seal.SealEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class SealModel extends GeoModel<SealEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "seal");
    private static final Identifier TEXTURE_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/seal_adult.png");
    private static final Identifier TEXTURE_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/seal_baby.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "seal");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState les && les.isBaby ? TEXTURE_BABY : TEXTURE_ADULT;
    }

    @Override
    public Identifier getAnimationResource(SealEntity entity) {
        return ANIMATION;
    }
}
