package it.hurts.shatterbyte.reanimal.common.entity.red_panda;

import it.hurts.shatterbyte.reanimal.common.entity.SurfaceNavigation;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class RedPandaNavigation extends SurfaceNavigation<RedPandaEntity> {
    private long nextRestSearchTick;

    public RedPandaNavigation(RedPandaEntity panda, Level level) {
        super(panda, level, (state, side) -> side == Direction.DOWN
                || side != Direction.UP && !state.is(BlockTags.LEAVES) && state.is(ReAnimalTags.Blocks.RED_PANDA_CLIMBABLE), 0.4D);

        this.setRequiredPathLength(16F);
    }

    @Override
    protected boolean isPassable(BlockPos pos) {
        return this.level.getBlockState(pos).is(BlockTags.LEAVES);
    }

    @Override
    protected boolean canClimbWithoutSupport(AABB bounds) {
        return this.climber.isInLeaves(bounds);
    }

    @Override
    protected boolean canFinishAt(Point point) {
        return point.side() == Direction.DOWN && this.canRest(point.position());
    }

    public void recover() {
        if (!this.isDone() || this.climber.isInLiquid() || this.climber.isPassenger()
                || (!this.climber.isClimbing() && (!this.climber.onGround() || !this.climber.isInLeaves(this.climber.position())))
                || this.level.getGameTime() < this.nextRestSearchTick)
            return;

        this.nextRestSearchTick = this.level.getGameTime() + 20L;

        var path = this.createSurfacePath(Set.of(), 0, 16F);

        if (path != null) {
            this.climber.setStandState(RedPandaEntity.StandState.ON_ALL_FOURS);
            this.climber.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(Vec3.atBottomCenterOf(path.getTarget()), 1F, 0));
            this.moveTo(path, 1D);
        }
    }
}
