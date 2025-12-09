package it.hurts.shatterbyte.reanimal.client.model.sea_urchin;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.pigeon.PigeonEntity;
import it.hurts.shatterbyte.reanimal.common.entity.sea_urchin.SeaUrchinEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

public class SeaUrchinModel extends GeoModel<SeaUrchinEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/sea_urchin.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/sea_urchin.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/sea_urchin.animation.json");

    @Override
    public ResourceLocation getModelResource(SeaUrchinEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SeaUrchinEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SeaUrchinEntity entity) {
        return ANIMATION;
    }
}
