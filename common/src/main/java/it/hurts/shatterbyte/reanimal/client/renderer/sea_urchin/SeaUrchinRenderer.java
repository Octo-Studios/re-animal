package it.hurts.shatterbyte.reanimal.client.renderer.sea_urchin;

import it.hurts.shatterbyte.reanimal.client.model.sea_urchin.SeaUrchinModel;
import it.hurts.shatterbyte.reanimal.common.entity.sea_urchin.SeaUrchinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;

public class SeaUrchinRenderer extends GeoEntityRenderer<SeaUrchinEntity, EntityRenderState> {
    public SeaUrchinRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SeaUrchinModel());
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
