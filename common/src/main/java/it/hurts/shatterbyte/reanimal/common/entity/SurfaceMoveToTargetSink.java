package it.hurts.shatterbyte.reanimal.common.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class SurfaceMoveToTargetSink extends MoveToTargetSink {
    private SurfaceNavigation<?> navigation;

    @Override
    protected void start(ServerLevel level, Mob entity, long timestamp) {
        this.navigation = (SurfaceNavigation<?>) entity.getNavigation();

        super.start(level, entity, timestamp);
    }

    @Override
    protected boolean timedOut(long timestamp) {
        return !this.navigation.isOnSurfacePath() && super.timedOut(timestamp);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Mob entity, long timestamp) {
        return this.navigation.isOnSurfacePath()
                && entity.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
                || super.canStillUse(level, entity, timestamp);
    }
}
