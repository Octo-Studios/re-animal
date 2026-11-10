package it.hurts.shatterbyte.reanimal.client.renderer.sea_urchin;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.reanimal.client.model.sea_urchin.SeaUrchinModel;
import it.hurts.shatterbyte.reanimal.common.entity.sea_urchin.SeaUrchinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import org.joml.Quaternionf;

public class SeaUrchinRenderer extends GeoEntityRenderer<SeaUrchinEntity, EntityRenderState> {
    private static final DataTicket<Quaternionf> ROLL_ROTATION = DataTicket.create("reanimal_urchin_roll", Quaternionf.class);

    public SeaUrchinRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SeaUrchinModel());
    }

    @Override
    public void extractRenderState(SeaUrchinEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        ((GeoRenderState) renderState).addGeckolibData(ROLL_ROTATION, entity.getRenderRotation(partialTick));
    }

    @Override
    protected void applyRotations(RenderPassInfo renderPassInfo, PoseStack poseStack, float nativeScale) {
        poseStack.translate(0D, 6D / 16D, 0D);
        poseStack.rotate((Quaternionf) renderPassInfo.renderState().getGeckolibData(ROLL_ROTATION));
        poseStack.translate(0D, -6D / 16D, 0D);

        super.applyRotations(renderPassInfo, poseStack, nativeScale);
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
