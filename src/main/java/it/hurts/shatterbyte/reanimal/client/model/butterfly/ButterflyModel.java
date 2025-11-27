package it.hurts.shatterbyte.reanimal.client.model.butterfly;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.butterfly.ButterflyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ButterflyModel extends GeoModel<ButterflyEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/butterfly.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/butterfly.animation.json");

    @Override
    public ResourceLocation getModelResource(ButterflyEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ButterflyEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/butterfly_" + entity.getVariant() +".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ButterflyEntity entity) {
        return ANIMATION;
    }
}
