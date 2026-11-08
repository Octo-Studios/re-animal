package it.hurts.shatterbyte.reanimal.client.model.ball;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.ball.BallEntity;
import net.minecraft.resources.Identifier;

public class BallModel extends GeoModel<BallEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "ball");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/item/ball/white.png");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(BallEntity entity) {
        return MODEL;
    }
}
