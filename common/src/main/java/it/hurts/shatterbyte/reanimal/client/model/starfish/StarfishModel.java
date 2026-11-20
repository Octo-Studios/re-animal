package it.hurts.shatterbyte.reanimal.client.model.starfish;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.starfish.StarfishEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class StarfishModel extends GeoModel<StarfishEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "starfish");
    private static final Identifier MODEL_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "starfish_baby");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "starfish");
    private static final DataTicket<Integer> VARIANT = DataTicket.create("starfish_variant", Integer.class);

    @Override
    public void addAdditionalStateData(StarfishEntity entity, Object extraData, GeoRenderState renderState) {
        renderState.addGeckolibData(VARIANT, entity.getVariant());
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return renderState instanceof LivingEntityRenderState living && living.isBaby ? MODEL_BABY : MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        var prefix = renderState instanceof LivingEntityRenderState living && living.isBaby ? "starfish_baby_" : "starfish_";
        return Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/" + prefix + renderState.getOrDefaultGeckolibData(VARIANT, 0) + ".png");
    }

    @Override
    public Identifier getAnimationResource(StarfishEntity entity) {
        return ANIMATION;
    }
}
