package it.hurts.shatterbyte.reanimal.client.light;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public record GlowSource(double x, double y, double z, float red, float green, float blue, Long2ByteMap reach) {
    private static final double CORNER_MARGIN = 0.01D;

    public static GlowSource at(Vec3 pos, int rgb) {
        return new GlowSource(
                pos.x, pos.y, pos.z,
                (rgb >> 16 & 0xFF) / 255.0F,
                (rgb >> 8 & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F,
                Long2ByteMaps.EMPTY_MAP
        );
    }

    public GlowSource reaching(Long2ByteMap reach) {
        return new GlowSource(this.x, this.y, this.z, this.red, this.green, this.blue, reach);
    }

    public int rgb() {
        return Math.round(this.red * 255.0F) << 16 | Math.round(this.green * 255.0F) << 8 | Math.round(this.blue * 255.0F);
    }

    public double distanceSqr(double px, double py, double pz) {
        var dx = this.x - px;
        var dy = this.y - py;
        var dz = this.z - pz;

        return dx * dx + dy * dy + dz * dz;
    }

    public int reachAt(BlockPos pos) {
        return this.reach.get(pos.asLong());
    }

    public int reachAt(double px, double py, double pz) {
        var best = 0;

        for (var dx = -1; dx <= 1; dx += 2)
            for (var dy = -1; dy <= 1; dy += 2)
                for (var dz = -1; dz <= 1; dz += 2)
                    best = Math.max(best, this.reach.get(BlockPos.asLong(
                            Mth.floor(px + dx * CORNER_MARGIN),
                            Mth.floor(py + dy * CORNER_MARGIN),
                            Mth.floor(pz + dz * CORNER_MARGIN)
                    )));

        return best;
    }
}
