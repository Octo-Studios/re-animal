package it.hurts.shatterbyte.reanimal.client.light.compat;

import it.hurts.shatterbyte.reanimal.client.light.ColoredGlow;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.core.BlockPos;

public final class SodiumGlow {
    private SodiumGlow() {
    }

    public static BlockPos sectionOf(BlockPos pos, BlockPos offsetInSection) {
        return ColoredGlow.isNear(pos) ? pos.subtract(offsetInSection) : null;
    }

    public static ChunkVertexEncoder.Vertex[] tint(ChunkVertexEncoder.Vertex[] vertices, BlockPos section) {
        if (section == null)
            return vertices;

        for (var vertex : vertices) {
            var tint = ColoredGlow.tint(section.getX() + vertex.x, section.getY() + vertex.y, section.getZ() + vertex.z, vertex.light);

            if (tint != ColoredGlow.WHITE)
                vertex.color = ColoredGlow.multiplyAbgr(vertex.color, tint);
        }

        return vertices;
    }
}
