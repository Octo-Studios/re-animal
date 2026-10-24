package it.hurts.shatterbyte.reanimal.client.renderer;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

public final class HeadTracking {
    public static final int FOLLOW = 0;
    public static final int KEEP_ANIMATION = 1;
    public static final int RESET = 2;

    private static final DataTicket<Integer> MODE = DataTicket.create("reanimal_head_mode", Integer.class);

    private HeadTracking() {
    }

    public static void setMode(EntityRenderState renderState, int mode) {
        ((GeoRenderState) renderState).addGeckolibData(MODE, mode);
    }

    public static void apply(RenderPassInfo<?> info, BoneSnapshots snapshots, boolean withPitch) {
        var renderState = info.renderState();

        if (!(renderState instanceof LivingEntityRenderState living))
            return;

        var mode = renderState.getOrDefaultGeckolibData(MODE, FOLLOW);

        if (mode == KEEP_ANIMATION)
            return;

        var yaw = mode == RESET ? 0F : -living.yRot * Mth.DEG_TO_RAD;
        var pitch = mode == RESET ? 0F : -living.xRot * Mth.DEG_TO_RAD;

        snapshots.ifPresent("head", snapshot -> {
            snapshot.setRotY(yaw);

            if (withPitch)
                snapshot.setRotX(pitch);
        });
    }
}
