package it.hurts.shatterbyte.reanimal.mixin.client;

import it.hurts.shatterbyte.reanimal.client.light.ColoredGlow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(QuadParticleRenderState.class)
public abstract class QuadParticleRenderStateMixin {
    @ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int reanimal$tintGlowLight(int color, SingleQuadParticle.Layer layer, float x, float y, float z, float qx, float qy, float qz, float qw, float size, float u0, float u1, float v0, float v1, int unused, int light) {
        var camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        var tint = ColoredGlow.tint(camera.x + x, camera.y + y, camera.z + z, light);

        return tint == ColoredGlow.WHITE ? color : ColoredGlow.multiply(color, tint);
    }
}
