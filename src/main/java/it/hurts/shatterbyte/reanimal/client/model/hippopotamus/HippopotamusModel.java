package it.hurts.shatterbyte.reanimal.client.model.hippopotamus;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.hippopotamus.HippopotamusEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

public class HippopotamusModel extends GeoModel<HippopotamusEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/hippopotamus.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/hippopotamus.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/hippopotamus.animation.json");

    @Override
    public ResourceLocation getModelResource(HippopotamusEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(HippopotamusEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(HippopotamusEntity entity) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(HippopotamusEntity animatable, long instanceId, AnimationState<HippopotamusEntity> state) {
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
