package it.hurts.shatterbyte.reanimal.client.model.ostrich;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.world.entity.ostrich.OstrichEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OstrichSaddleModel extends GeoModel<OstrichEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/ostrich_saddle.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/ostrich_saddle.png");

    @Override
    public ResourceLocation getModelResource(OstrichEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(OstrichEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(OstrichEntity animatable) {
        return null;
    }
}
