package it.hurts.shatterbyte.reanimal.client.model.ostrich;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.ostrich.OstrichEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

public class OstrichModel extends GeoModel<OstrichEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/ostrich.geo.json");
    private static final ResourceLocation TEXTURE_ADULT = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/ostrich_adult.png");
    private static final ResourceLocation TEXTURE_BABY = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/ostrich_baby.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/ostrich.animation.json");

    @Override
    public ResourceLocation getModelResource(OstrichEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(OstrichEntity entity) {
        return entity.isBaby() ? TEXTURE_BABY : TEXTURE_ADULT;
    }

    @Override
    public ResourceLocation getAnimationResource(OstrichEntity entity) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(OstrichEntity animatable, long instanceId, AnimationState<OstrichEntity> state) {
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
