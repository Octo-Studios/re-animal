package it.hurts.shatterbyte.reanimal.client.model.capybara;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.capybara.CapybaraEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class CapybaraModel extends GeoModel<CapybaraEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "capybara");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/capybara.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "capybara");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(CapybaraEntity entity) {
        return ANIMATION;
    }
}
