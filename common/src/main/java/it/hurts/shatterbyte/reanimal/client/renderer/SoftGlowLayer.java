package it.hurts.shatterbyte.reanimal.client.renderer;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.client.light.GlowColors;
import it.hurts.shatterbyte.reanimal.common.entity.glow_stick.GlowStickEntity;
import it.hurts.shatterbyte.reanimal.common.entity.jellyfish.JellyfishEntity;
import it.hurts.shatterbyte.reanimal.common.item.GlowStickItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Vector3f;

import java.util.function.BiConsumer;

public class SoftGlowLayer<R extends EntityRenderState & GeoRenderState> extends GeoRenderLayer<GeoAnimatable, Void, R> {
    private static final DataTicket<Integer> COLOR = DataTicket.create("reanimal_glow_color", Integer.class);
    private static final RenderType RENDER_TYPE = RenderTypes.eyes(Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/misc/soft_glow.png"));

    private final Glow[] glows;

    public SoftGlowLayer(GeoRenderer renderer, Glow... glows) {
        super(renderer);

        this.glows = glows;
    }

    @Override
    public void addRenderData(GeoAnimatable animatable, Void relatedObject, R renderState, float partialTick) {
        var color = animatable instanceof JellyfishEntity jellyfish ? GlowColors.of(jellyfish)
                : GlowColors.of((GlowStickItem) ((GlowStickEntity) animatable).getItem().getItem());

        renderState.addGeckolibData(COLOR, color);
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<R> renderPassInfo, BiConsumer<GeoBone, PerBoneRender<R>> consumer) {
        if (renderPassInfo.renderState().isInvisible)
            return;

        for (var glow : this.glows)
            renderPassInfo.model().getBone(glow.bone()).ifPresent(bone -> consumer.accept(bone,
                    (pass, ignored, renderTasks) -> this.submitGlow(pass, renderTasks, glow)));
    }

    private void submitGlow(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks, Glow glow) {
        var poseStack = renderPassInfo.poseStack();
        var matrix = poseStack.last().pose();
        var center = matrix.transformPosition(0F, glow.y(), 0F, new Vector3f());
        var radius = glow.radius() * matrix.getScale(new Vector3f()).x;
        var rgb = renderPassInfo.getGeckolibData(COLOR);
        var color = Math.round(glow.opacity() * 255F) << 24 | rgb;

        poseStack.pushPose();
        poseStack.last().pose().identity().translate(center).rotate(renderPassInfo.cameraState().orientation).scale(radius);

        renderTasks.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, -1F, -1F, 0F, 1F, color);
            vertex(buffer, pose, 1F, -1F, 1F, 1F, color);
            vertex(buffer, pose, 1F, 1F, 1F, 0F, color);
            vertex(buffer, pose, -1F, 1F, 0F, 0F, color);
        });

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int color) {
        buffer.addVertex(pose, x, y, 0F)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(0F, 0F, 1F);
    }

    public record Glow(String bone, float y, float radius, float opacity) {
    }
}
