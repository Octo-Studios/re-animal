package it.hurts.shatterbyte.reanimal.client.model.giraffe;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.world.entity.capybara.CapybaraEntity;
import it.hurts.shatterbyte.reanimal.world.entity.giraffe.GiraffeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

public class GiraffeModel extends GeoModel<GiraffeEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/giraffe.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/giraffe.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/giraffe.animation.json");

    @Override
    public ResourceLocation getModelResource(GiraffeEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GiraffeEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(GiraffeEntity entity) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(GiraffeEntity animatable, long instanceId, AnimationState<GiraffeEntity> state) {
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
