package it.hurts.shatterbyte.reanimal.client.renderer.starfish;

import it.hurts.shatterbyte.reanimal.client.model.starfish.StarfishModel;
import it.hurts.shatterbyte.reanimal.common.entity.starfish.StarfishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;

public class StarfishRenderer extends GeoEntityRenderer<StarfishEntity, EntityRenderState> {
    public StarfishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new StarfishModel());
    }

    @Override
    public void scaleModelForRender(RenderPassInfo renderPassInfo, float widthScale, float heightScale) {
        boolean isBaby = renderPassInfo.renderState() instanceof LivingEntityRenderState les && les.isBaby;
        if (isBaby) {
            renderPassInfo.poseStack().scale(0.5F, 0.5F, 0.5F);

            this.shadowRadius = 0.25F * 0.5F;
        } else {
            renderPassInfo.poseStack().scale(1F, 1F, 1F);

            this.shadowRadius = 0.25F;
        }

        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);
    }
}
