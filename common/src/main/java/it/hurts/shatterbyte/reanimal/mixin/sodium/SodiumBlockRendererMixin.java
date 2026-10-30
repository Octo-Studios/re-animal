package it.hurts.shatterbyte.reanimal.mixin.sodium;

import it.hurts.shatterbyte.reanimal.client.light.compat.SodiumGlow;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockRenderer.class, remap = false)
public abstract class SodiumBlockRendererMixin {
    @Unique
    private SodiumGlow.Cell reanimal$cell;

    @Inject(method = "renderModel", at = @At("HEAD"))
    private void reanimal$rememberOrigin(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        this.reanimal$cell = SodiumGlow.cellOf(pos, origin);
    }

    @ModifyArg(method = "bufferQuad", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/builder/ChunkMeshBufferBuilder;push([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;I)V"), index = 0)
    private ChunkVertexEncoder.Vertex[] reanimal$tintGlowLight(ChunkVertexEncoder.Vertex[] vertices) {
        return SodiumGlow.tint(vertices, this.reanimal$cell);
    }
}
