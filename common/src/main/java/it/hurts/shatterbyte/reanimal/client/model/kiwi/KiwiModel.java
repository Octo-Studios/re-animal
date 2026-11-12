package it.hurts.shatterbyte.reanimal.client.model.kiwi;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.kiwi.KiwiEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class KiwiModel extends GeoModel<KiwiEntity> {
    private static final Identifier ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "kiwi_adult");
    private static final Identifier BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "kiwi_baby");
    private static final Identifier TEXTURE_ADULT = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/kiwi_adult.png");
    private static final Identifier TEXTURE_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/kiwi_baby.png");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState living && living.isBaby ? BABY : ADULT;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState living && living.isBaby ? TEXTURE_BABY : TEXTURE_ADULT;
    }

    @Override
    public Identifier getAnimationResource(KiwiEntity entity) {
        return entity.isBaby() ? BABY : ADULT;
    }
}
