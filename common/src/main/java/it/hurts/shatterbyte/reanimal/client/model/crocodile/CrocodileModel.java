package it.hurts.shatterbyte.reanimal.client.model.crocodile;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.crocodile.CrocodileEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class CrocodileModel extends GeoModel<CrocodileEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "crocodile");
    private static final Identifier MODEL_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "crocodile_baby");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/crocodile.png");
    private static final Identifier TEXTURE_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/crocodile_baby.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "crocodile");
    private static final Identifier ANIMATION_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "crocodile_baby");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState living && living.isBaby ? MODEL_BABY : MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState living && living.isBaby ? TEXTURE_BABY : TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(CrocodileEntity entity) {
        return entity.isBaby() ? ANIMATION_BABY : ANIMATION;
    }
}
