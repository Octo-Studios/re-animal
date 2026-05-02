package it.hurts.shatterbyte.reanimal.client.model.red_panda;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.red_panda.RedPandaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

public class RedPandaModel extends GeoModel<RedPandaEntity> {
    private static final ResourceLocation MODEL_BABY = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/red_panda_baby.geo.json");
    private static final ResourceLocation MODEL_ADULT = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/red_panda_adult.geo.json");
    private static final ResourceLocation TEXTURE_BABY = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/red_panda_baby.png");
    private static final ResourceLocation TEXTURE_ADULT = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/red_panda_adult.png");
    private static final ResourceLocation ANIMATION_BABY = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/red_panda_baby.animation.json");
    private static final ResourceLocation ANIMATION_ADULT = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/red_panda_adult.animation.json");

    @Override
    public ResourceLocation getModelResource(RedPandaEntity entity) {
        return entity.isBaby() ? MODEL_BABY : MODEL_ADULT;
    }

    @Override
    public ResourceLocation getTextureResource(RedPandaEntity entity) {
        return entity.isBaby() ? TEXTURE_BABY : TEXTURE_ADULT;
    }

    @Override
    public ResourceLocation getAnimationResource(RedPandaEntity entity) {
        return entity.isBaby() ? ANIMATION_BABY : ANIMATION_ADULT;
    }

    @Override
    public void setCustomAnimations(RedPandaEntity animatable, long instanceId, AnimationState<RedPandaEntity> state) {
        super.setCustomAnimations(animatable, instanceId, state);

        var head = getAnimationProcessor().getBone("head");

        if (head == null)
            return;

        var modelData = state.getData(DataTickets.ENTITY_MODEL_DATA);

        var headYaw = modelData.netHeadYaw() * ((float) Math.PI / 180F);
        var headPitch = modelData.headPitch() * ((float) Math.PI / 180F);

        head.setRotY(headYaw);
    }
}
