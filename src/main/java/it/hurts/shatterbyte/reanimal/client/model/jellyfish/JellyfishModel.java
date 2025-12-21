package it.hurts.shatterbyte.reanimal.client.model.jellyfish;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.jellyfish.JellyfishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

public class JellyfishModel extends GeoModel<JellyfishEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/jellyfish.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/jellyfish.animation.json");

    @Override
    public ResourceLocation getModelResource(JellyfishEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(JellyfishEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/jellyfish_" + entity.getVariant() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(JellyfishEntity entity) {
        return ANIMATION;
    }
}
