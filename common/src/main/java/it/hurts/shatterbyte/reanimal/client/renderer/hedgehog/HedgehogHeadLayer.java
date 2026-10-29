package it.hurts.shatterbyte.reanimal.client.renderer.hedgehog;

import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.player.Player;

public class HedgehogHeadLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final float HEAD_TOP_OFFSET = 0.5F;

    public HedgehogHeadLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, AvatarRenderState state, float yRot, float xRot) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;

        if (level == null || !(level.getEntity(state.id) instanceof Player player))
            return;

        var hedgehog = HedgehogEntity.getHeadPassenger(player);

        if (hedgehog == null || !(minecraft.getEntityRenderDispatcher().getRenderer(hedgehog) instanceof HedgehogRenderer renderer))
            return;

        var partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var hedgehogState = renderer.createHeadRenderState(hedgehog, partialTick, packedLight);

        poseStack.pushPose();

        this.getParentModel().translateToHead(poseStack);

        poseStack.scale(-1F, -1F, 1F);
        poseStack.translate(0F, HEAD_TOP_OFFSET, 0F);

        renderer.submit(hedgehogState, poseStack, collector, new CameraRenderState());

        poseStack.popPose();
    }
}
