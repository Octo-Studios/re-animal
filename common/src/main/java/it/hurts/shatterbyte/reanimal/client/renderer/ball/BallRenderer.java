package it.hurts.shatterbyte.reanimal.client.renderer.ball;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.CustomBoneTextureGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.client.model.ball.BallModel;
import it.hurts.shatterbyte.reanimal.common.entity.ball.BallEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class BallRenderer extends GeoEntityRenderer<BallEntity, EntityRenderState> {
    private static final DataTicket<BallRenderData> BALL = DataTicket.create("reanimal_ball", BallRenderData.class);

    public BallRenderer(EntityRendererProvider.Context context) {
        super(context, new BallModel());

        this.shadowRadius = 0.2F;

        for (var part = 0; part < 4; part++)
            this.withRenderLayer(new PanelLayer(this, part));
    }

    @Override
    public void extractRenderState(BallEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        if (entity.getVehicle() instanceof Wolf wolf) {
            var position = entity.getCarriedPosition(wolf, partialTick);

            renderState.x = position.x;
            renderState.y = position.y;
            renderState.z = position.z;
        }

        var colors = new int[4];

        for (var part = 0; part < 4; part++)
            colors[part] = entity.getPanelColor(part);

        ((GeoRenderState) renderState).addGeckolibData(BALL, new BallRenderData(entity.getRenderRotation(partialTick),
                entity.getImpact().normalize(), entity.getSquash(partialTick), entity.getItem(), colors));
    }

    @Override
    public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutoutCull(texture);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo renderPassInfo) {
        var data = (BallRenderData) renderPassInfo.getGeckolibData(BALL);
        var poseStack = renderPassInfo.poseStack();

        poseStack.translate(0D, 0.25D, 0D);

        if (data.squash() != 0F) {
            var normal = data.normal();
            var axial = 1F - data.squash();
            var radial = 1F / (float) Math.sqrt(axial);
            var alignment = new Quaternionf().rotationTo(0F, 1F, 0F, (float) normal.x, (float) normal.y, (float) normal.z);

            poseStack.translate(-normal.x * data.squash() * 0.25D, -normal.y * data.squash() * 0.25D, -normal.z * data.squash() * 0.25D);
            poseStack.rotate(alignment);
            poseStack.scale(radial, axial, radial);
            poseStack.rotate(alignment.conjugate());
        }

        poseStack.rotate(data.rotation());
        poseStack.scale(0.5F, 0.5F, 0.5F);
    }

    private record BallRenderData(Quaternionf rotation, Vec3 normal, float squash, ItemStack item, int[] colors) {
    }

    private static class PanelLayer extends CustomBoneTextureGeoLayer {
        private final int part;

        private PanelLayer(BallRenderer renderer, int part) {
            super(renderer, "panel_" + part, Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/item/ball/white.png"));

            this.part = part;
        }

        @Override
        protected void renderBone(RenderPassInfo renderPassInfo, GeoBone bone, SubmitNodeCollector renderTasks) {
            var data = (BallRenderData) renderPassInfo.getGeckolibData(BALL);
            var modelData = data.item().get(DataComponents.CUSTOM_MODEL_DATA);
            var name = modelData == null ? null : modelData.getString(this.part);
            var texture = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/item/ball/" + (name == null ? "white" : name) + ".png");
            var color = name == null ? 0xFF000000 | data.colors()[this.part] : 0xFFFFFFFF;
            var light = renderPassInfo.packedLight();
            var overlay = renderPassInfo.packedOverlay();

            renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), RenderTypes.entityCutoutCull(texture), (pose, buffer) -> {
                var poseStack = new PoseStack();

                poseStack.last().set(pose);
                bone.translateAwayFromPivotPoint(poseStack);

                for (var cube : ((CuboidGeoBone) bone).cubes) {
                    poseStack.pushPose();
                    cube.render(poseStack, buffer, light, overlay, color);
                    poseStack.popPose();
                }
            });
        }
    }
}
