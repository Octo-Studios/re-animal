package it.hurts.shatterbyte.reanimal.client.renderer.pigeon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.pigeon.PigeonDroppingEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;

public class PigeonDroppingRenderer extends EntityRenderer<PigeonDroppingEntity, EntityRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/pigeon_dropping.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(TEXTURE);
    private static final float HALF_WIDTH = 0.03F;
    private static final float HALF_HEIGHT = 0.06F;

    public PigeonDroppingRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.1F, 0.0F);
        poseStack.rotate(new Quaternionf(0.0F, camera.orientation.y, 0.0F, camera.orientation.w).normalize());

        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, -HALF_WIDTH, -HALF_HEIGHT, 0.0F, 1.0F, state.lightCoords);
            vertex(buffer, pose, HALF_WIDTH, -HALF_HEIGHT, 1.0F, 1.0F, state.lightCoords);
            vertex(buffer, pose, HALF_WIDTH, HALF_HEIGHT, 1.0F, 0.0F, state.lightCoords);
            vertex(buffer, pose, -HALF_WIDTH, HALF_HEIGHT, 0.0F, 0.0F, state.lightCoords);
        });

        poseStack.popPose();

        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int lightCoords) {
        buffer.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightCoords)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
