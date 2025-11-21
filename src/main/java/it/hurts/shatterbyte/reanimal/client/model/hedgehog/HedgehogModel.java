package it.hurts.shatterbyte.reanimal.client.model.hedgehog;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.world.entity.HedgehogEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

public class HedgehogModel extends GeoModel<HedgehogEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/hedgehog.geo.json");
    private static final ResourceLocation TEXTURE_ADULT = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/hedgehog_adult.png");
    private static final ResourceLocation TEXTURE_BABY = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/hedgehog_baby.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/hedgehog.animation.json");

    @Override
    public ResourceLocation getModelResource(HedgehogEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(HedgehogEntity entity) {
        return entity.isBaby() ? TEXTURE_BABY : TEXTURE_ADULT;
    }

    @Override
    public ResourceLocation getAnimationResource(HedgehogEntity entity) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(HedgehogEntity animatable, long instanceId, AnimationState<HedgehogEntity> state) {
        super.setCustomAnimations(animatable, instanceId, state);

        var head = getAnimationProcessor().getBone("head");

        if (head == null)
            return;

        var modelData = state.getData(DataTickets.ENTITY_MODEL_DATA);

        var headYaw = modelData.netHeadYaw() * ((float) Math.PI / 180F);
        var headPitch = modelData.headPitch() * ((float) Math.PI / 180F);

        head.setRotY(headYaw);
        head.setRotX(headPitch);
    }
}
