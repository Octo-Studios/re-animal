package it.hurts.shatterbyte.reanimal.client.model.giraffe;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.giraffe.GiraffeEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class GiraffeModel extends GeoModel<GiraffeEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "giraffe");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/giraffe.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "giraffe");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(GiraffeEntity entity) {
        return ANIMATION;
    }
}
