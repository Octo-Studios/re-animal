package it.hurts.shatterbyte.reanimal.client.model.glow_stick;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.glow_stick.GlowStickEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.model.GeoModel;

public class GlowStickModel extends GeoModel<GlowStickEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/glow_stick.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/glow_stick.animation.json");

    @Override
    public ResourceLocation getModelResource(GlowStickEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GlowStickEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/glow_stick/" + BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()).getPath() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(GlowStickEntity entity) {
        return ANIMATION;
    }
}
