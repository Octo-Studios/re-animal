package it.hurts.shatterbyte.reanimal.client.model.sea_urchin;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.pigeon.PigeonEntity;
import it.hurts.shatterbyte.reanimal.common.entity.sea_urchin.SeaUrchinEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class SeaUrchinModel extends GeoModel<SeaUrchinEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "sea_urchin");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/sea_urchin.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "sea_urchin");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(SeaUrchinEntity entity) {
        return ANIMATION;
    }
}
