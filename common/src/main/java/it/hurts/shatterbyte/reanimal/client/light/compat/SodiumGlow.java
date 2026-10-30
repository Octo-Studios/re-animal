package it.hurts.shatterbyte.reanimal.client.light.compat;

import it.hurts.shatterbyte.reanimal.client.light.ColoredGlow;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.core.BlockPos;

public final class SodiumGlow {
    private SodiumGlow() {
    }

    public static Cell cellOf(BlockPos pos, BlockPos offsetInSection) {
        return ColoredGlow.isNear(pos) ? new Cell(pos.subtract(offsetInSection), pos.immutable()) : null;
    }

    public static ChunkVertexEncoder.Vertex[] tint(ChunkVertexEncoder.Vertex[] vertices, Cell cell) {
        if (cell == null)
            return vertices;

        var section = cell.section();

        for (var vertex : vertices) {
            var tint = ColoredGlow.tint(section.getX() + vertex.x, section.getY() + vertex.y, section.getZ() + vertex.z, vertex.light, cell.block());

            if (tint != ColoredGlow.WHITE)
                vertex.color = ColoredGlow.multiplyAbgr(vertex.color, tint);
        }

        return vertices;
    }

    public record Cell(BlockPos section, BlockPos block) {
    }
}
