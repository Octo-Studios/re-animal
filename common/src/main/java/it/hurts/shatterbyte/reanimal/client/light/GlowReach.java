package it.hurts.shatterbyte.reanimal.client.light;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class GlowReach {
    public static final int RANGE = 17;

    private GlowReach() {
    }

    public static Long2ByteOpenHashMap flood(ClientLevel level, BlockPos origin) {
        var levels = new Long2ByteOpenHashMap();
        var queue = new LongArrayFIFOQueue();
        var cursor = new BlockPos.MutableBlockPos();

        levels.put(origin.asLong(), (byte) RANGE);
        queue.enqueue(origin.asLong());

        while (!queue.isEmpty()) {
            var current = queue.dequeueLong();
            var currentLevel = levels.get(current);

            if (currentLevel <= 1)
                continue;

            for (var direction : Direction.values()) {
                var next = BlockPos.offset(current, direction);
                var nextLevel = currentLevel - Math.max(1, level.getBlockState(cursor.set(next)).getLightDampening());

                if (nextLevel <= levels.get(next))
                    continue;

                levels.put(next, (byte) nextLevel);
                queue.enqueue(next);
            }
        }

        return levels;
    }
}
