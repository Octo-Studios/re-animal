package it.hurts.shatterbyte.reanimal.client.model.glow_stick;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.glow_stick.GlowStickEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public class GlowStickModel extends GeoModel<GlowStickEntity> {
    public static final DataTicket<String> ITEM_NAME = DataTicket.create("reanimal_glow_item", String.class);

    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "glow_stick");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "glow_stick");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/glow_stick/" + renderState.getOrDefaultGeckolibData(ITEM_NAME, "glow_stick") + ".png");
    }

    @Override
    public Identifier getAnimationResource(GlowStickEntity entity) {
        return ANIMATION;
    }
}
