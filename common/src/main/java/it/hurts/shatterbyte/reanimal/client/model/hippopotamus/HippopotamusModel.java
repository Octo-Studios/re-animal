package it.hurts.shatterbyte.reanimal.client.model.hippopotamus;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.hippopotamus.HippopotamusEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class HippopotamusModel extends GeoModel<HippopotamusEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hippopotamus");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/hippopotamus.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hippopotamus");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(HippopotamusEntity entity) {
        return ANIMATION;
    }
}
