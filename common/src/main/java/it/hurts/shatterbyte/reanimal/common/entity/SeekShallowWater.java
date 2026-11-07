package it.hurts.shatterbyte.reanimal.common.entity;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.pathfinder.PathType;

import java.util.function.Predicate;

public class SeekShallowWater<E extends PathfinderMob> extends Behavior<E> {
    private final Predicate<E> canStart;
    private long nextSearchTick;

    public SeekShallowWater(Predicate<E> canStart) {
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT
        ));

        this.canStart = canStart;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        if (level.getGameTime() < this.nextSearchTick || entity.isInWater() || !this.canStart.test(entity))
            return false;

        this.nextSearchTick = level.getGameTime() + 400 + entity.getRandom().nextInt(400);

        return true;
    }

    @Override
    protected void start(ServerLevel level, E entity, long gameTime) {
        walkToWater(level, entity, 8);
    }

    public static boolean walkToWater(ServerLevel level, PathfinderMob entity, int radius) {
        var random = entity.getRandom();
        var origin = entity.blockPosition();

        for (var i = 0; i < radius * radius / 2; i++) {
            var pos = origin.offset(random.nextInt(radius * 2 + 1) - radius, random.nextInt(5) - 2, random.nextInt(radius * 2 + 1) - radius);

            if (!level.hasChunkAt(pos) || !isShallowWater(level, entity, pos))
                continue;

            var navigation = entity.getNavigation();
            var waterMalus = entity.getPathfindingMalus(PathType.WATER);

            entity.setPathfindingMalus(PathType.WATER, 0F);

            var path = navigation.createPath(pos, 0, radius * 2);

            entity.setPathfindingMalus(PathType.WATER, waterMalus);

            if (path == null || !path.canReach())
                return false;

            var width = (int) (entity.getBbWidth() + 1F);

            for (var node = 0; node < path.getNodeCount(); node++) {
                var nodePos = path.getNodePos(node);

                for (var block : BlockPos.betweenClosed(nodePos, nodePos.offset(width - 1, 0, width - 1))) {
                    if (!level.hasChunkAt(block) || level.getFluidState(block).is(FluidTags.WATER) && !isShallowWater(level, entity, block))
                        return false;
                }
            }

            navigation.moveTo(path, 1D);
            entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, 1F, 0));

            return true;
        }

        return false;
    }

    public static boolean isShallowWater(ServerLevel level, PathfinderMob entity, BlockPos pos) {
        var fluid = level.getFluidState(pos);

        return fluid.is(FluidTags.WATER) && fluid.getHeight(level, pos) < entity.getBbHeight()
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
}
