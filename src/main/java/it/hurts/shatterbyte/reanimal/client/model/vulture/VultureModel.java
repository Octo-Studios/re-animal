package it.hurts.shatterbyte.reanimal.client.model.vulture;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.seal.SealEntity;
import it.hurts.shatterbyte.reanimal.common.entity.vulture.VultureEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

public class VultureModel extends GeoModel<VultureEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/vulture.geo.json");
    private static final ResourceLocation TEXTURE_ADULT = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/vulture_adult.png");
    private static final ResourceLocation TEXTURE_BABY = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/vulture_baby.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/vulture.animation.json");

    @Override
    public ResourceLocation getModelResource(VultureEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(VultureEntity entity) {
        return entity.isBaby() ? TEXTURE_BABY : TEXTURE_ADULT;
    }

    @Override
    public ResourceLocation getAnimationResource(VultureEntity entity) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(VultureEntity animatable, long instanceId, AnimationState<VultureEntity> state) {
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
