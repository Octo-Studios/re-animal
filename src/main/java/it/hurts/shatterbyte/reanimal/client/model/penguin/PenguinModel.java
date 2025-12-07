package it.hurts.shatterbyte.reanimal.client.model.penguin;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.kiwi.KiwiEntity;
import it.hurts.shatterbyte.reanimal.common.entity.penguin.PenguinEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

public class PenguinModel extends GeoModel<PenguinEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/penguin.geo.json");
    private static final ResourceLocation TEXTURE_ADULT = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/penguin_adult.png");
    private static final ResourceLocation TEXTURE_BABY = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/penguin_baby.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/penguin.animation.json");

    @Override
    public ResourceLocation getModelResource(PenguinEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PenguinEntity entity) {
        return entity.isBaby() ? TEXTURE_BABY : TEXTURE_ADULT;
    }

    @Override
    public ResourceLocation getAnimationResource(PenguinEntity entity) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(PenguinEntity animatable, long instanceId, AnimationState<PenguinEntity> state) {
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
