package it.hurts.shatterbyte.reanimal.mixin.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.hurts.shatterbyte.reanimal.client.light.ColoredGlow;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidRenderer.class)
public abstract class FluidRendererMixin {
    @Unique
    private static final ThreadLocal<BlockPos> reanimal$origin = new ThreadLocal<>();

    @Inject(method = "tesselate", at = @At("HEAD"))
    private void reanimal$rememberOrigin(BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        reanimal$origin.set(pos);
    }

    @Inject(method = "tesselate", at = @At("RETURN"))
    private void reanimal$forgetOrigin(BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        reanimal$origin.remove();
    }

    @ModifyVariable(method = "vertex", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int reanimal$tintGlowLight(int color, VertexConsumer consumer, float x, float y, float z, int unused, float u, float v, int light) {
        var origin = reanimal$origin.get();

        if (origin == null)
            return color;

        return ColoredGlow.apply(color, ColoredGlow.tint(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D, light));
    }
}
