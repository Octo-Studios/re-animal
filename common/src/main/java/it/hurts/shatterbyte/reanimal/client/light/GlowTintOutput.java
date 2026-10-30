package it.hurts.shatterbyte.reanimal.client.light;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;

public final class GlowTintOutput implements BlockQuadOutput {
    private final BlockQuadOutput delegate;
    private final BlockPos origin;

    public GlowTintOutput(BlockQuadOutput delegate, BlockPos origin) {
        this.delegate = delegate;
        this.origin = origin.immutable();
    }

    @Override
    public void put(float x, float y, float z, BakedQuad quad, QuadInstance instance) {
        for (var i = 0; i < BakedQuad.VERTEX_COUNT; i++) {
            var corner = quad.position(i);
            var tint = ColoredGlow.tint(
                    this.origin.getX() + corner.x(),
                    this.origin.getY() + corner.y(),
                    this.origin.getZ() + corner.z(),
                    instance.getLightCoords(i)
            );

            if (tint != ColoredGlow.WHITE)
                instance.setColor(i, ColoredGlow.multiply(instance.getColor(i), tint));
        }

        this.delegate.put(x, y, z, quad, instance);
    }
}
