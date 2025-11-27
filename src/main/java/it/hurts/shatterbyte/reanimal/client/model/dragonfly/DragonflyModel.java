package it.hurts.shatterbyte.reanimal.client.model.dragonfly;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.dragonfly.DragonflyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DragonflyModel extends GeoModel<DragonflyEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/dragonfly.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/dragonfly.animation.json");

    @Override
    public ResourceLocation getModelResource(DragonflyEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DragonflyEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/dragonfly_" + entity.getVariant() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(DragonflyEntity entity) {
        return ANIMATION;
    }
}
