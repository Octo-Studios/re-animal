package it.hurts.shatterbyte.reanimal.client.model.pigeon;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.pigeon.PigeonEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class PigeonModel extends GeoModel<PigeonEntity> {
    private static final DataTicket<Integer> VARIANT = DataTicket.create("pigeon_variant", Integer.class);
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "pigeon");
    private static final Identifier[] TEXTURES_ADULT = {
            Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/pigeon_adult_gray.png"),
            Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/pigeon_adult_black.png"),
            Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/pigeon_adult_brown.png"),
            Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/pigeon_adult_white.png")
    };
    private static final Identifier TEXTURE_BABY = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/pigeon_baby.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "pigeon");

    @Override
    public void addAdditionalStateData(PigeonEntity entity, Object extraData, GeoRenderState renderState) {
        renderState.addGeckolibData(VARIANT, entity.getVariant());
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        if (renderState instanceof LivingEntityRenderState les && les.isBaby)
            return TEXTURE_BABY;

        var variant = renderState.getOrDefaultGeckolibData(VARIANT, 0);

        return TEXTURES_ADULT[Math.floorMod(variant, TEXTURES_ADULT.length)];
    }

    @Override
    public Identifier getAnimationResource(PigeonEntity entity) {
        return ANIMATION;
    }
}
