package it.hurts.shatterbyte.reanimal.client.model.starfish;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.starfish.StarfishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class StarfishModel extends GeoModel<StarfishEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/starfish.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/starfish.animation.json");

    @Override
    public ResourceLocation getModelResource(StarfishEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(StarfishEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/starfish_" + entity.getVariant() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(StarfishEntity entity) {
        return ANIMATION;
    }
}
