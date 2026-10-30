package it.hurts.shatterbyte.reanimal.mixin.sodium;

import it.hurts.shatterbyte.reanimal.client.light.compat.SodiumGlow;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DefaultFluidRenderer.class, remap = false)
public abstract class SodiumFluidRendererMixin {
    @Unique
    private BlockPos reanimal$section;

    @Inject(method = "render", at = @At("HEAD"))
    private void reanimal$rememberOrigin(LevelSlice slice, BlockState blockState, FluidState fluidState, BlockPos pos, BlockPos origin, TranslucentGeometryCollector collector, ChunkModelBuilder builder, Material material, ColorProvider<FluidState> colorProvider, FluidModel model, CallbackInfo ci) {
        this.reanimal$section = SodiumGlow.sectionOf(pos, origin);
    }

    @ModifyArg(method = "writeQuad", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/builder/ChunkMeshBufferBuilder;push([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;)V"), index = 0)
    private ChunkVertexEncoder.Vertex[] reanimal$tintGlowLight(ChunkVertexEncoder.Vertex[] vertices) {
        return SodiumGlow.tint(vertices, this.reanimal$section);
    }
}
