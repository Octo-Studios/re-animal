package it.hurts.shatterbyte.reanimal.mixin.client;

import it.hurts.shatterbyte.reanimal.client.light.ColoredGlow;
import it.hurts.shatterbyte.reanimal.client.light.GlowTintOutput;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {
    @ModifyVariable(method = "tesselateBlock", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private BlockQuadOutput reanimal$tintGlowLight(BlockQuadOutput output, BlockQuadOutput unused, float x, float y, float z, BlockAndTintGetter level, BlockPos pos, BlockState state, BlockStateModel model, long seed) {
        return ColoredGlow.isNear(pos) ? new GlowTintOutput(output, pos) : output;
    }
}
