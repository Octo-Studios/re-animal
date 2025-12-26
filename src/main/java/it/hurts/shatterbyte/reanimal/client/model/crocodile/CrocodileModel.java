package it.hurts.shatterbyte.reanimal.client.model.crocodile;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.crocodile.CrocodileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

public class CrocodileModel extends GeoModel<CrocodileEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "geo/crocodile.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/crocodile.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "animations/crocodile.animation.json");

    @Override
    public ResourceLocation getModelResource(CrocodileEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CrocodileEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CrocodileEntity entity) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(CrocodileEntity animatable, long instanceId, AnimationState<CrocodileEntity> state) {
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
