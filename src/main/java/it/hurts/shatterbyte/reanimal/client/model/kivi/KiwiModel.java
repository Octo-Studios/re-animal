package it.hurts.shatterbyte.reanimal.client.model.kivi;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.world.entity.hedgehog.HedgehogEntity;
import it.hurts.shatterbyte.reanimal.world.entity.kiwi.KiwiEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

public class KiwiModel extends GeoModel<KiwiEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/kiwi.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/kiwi.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/kiwi.animation.json");

    @Override
    public ResourceLocation getModelResource(KiwiEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(KiwiEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(KiwiEntity entity) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(KiwiEntity animatable, long instanceId, AnimationState<KiwiEntity> state) {
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
