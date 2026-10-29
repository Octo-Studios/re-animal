package it.hurts.shatterbyte.reanimal.client.model.hedgehog;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import it.hurts.shatterbyte.reanimal.client.renderer.hedgehog.HeadHedgehogAnimatable;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HeadHedgehogData;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class HeadHedgehogModel extends GeoModel<HeadHedgehogAnimatable> {
    @Override
    public void addAdditionalStateData(HeadHedgehogAnimatable animatable, @Nullable Object relatedObject, GeoRenderState renderState) {
        renderState.addGeckolibData(HedgehogModel.IS_BABY, relatedObject instanceof HeadHedgehogData data && data.baby());
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return HedgehogModel.MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState.getOrDefaultGeckolibData(HedgehogModel.IS_BABY, false) ? HedgehogModel.TEXTURE_BABY : HedgehogModel.TEXTURE_ADULT;
    }

    @Override
    public Identifier getAnimationResource(HeadHedgehogAnimatable animatable) {
        return HedgehogModel.ANIMATION;
    }
}
