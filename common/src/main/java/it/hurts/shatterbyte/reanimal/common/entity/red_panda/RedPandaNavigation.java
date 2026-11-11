package it.hurts.shatterbyte.reanimal.common.entity.red_panda;

import it.hurts.shatterbyte.reanimal.common.entity.SurfaceNavigation;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RedPandaNavigation extends SurfaceNavigation<RedPandaEntity> {
    private static final int TREE_PATH_RANGE = 16;
    private static final UniformInt TREE_CLIMB_COOLDOWN = UniformInt.of(600, 1200);

    private long nextDescentSearchTick;
    private long nextClimbTick;
    private boolean wasInTree;

    public RedPandaNavigation(RedPandaEntity panda, Level level) {
        super(panda, level, (state, side) -> side == Direction.DOWN
                || side != Direction.UP && !state.is(BlockTags.LEAVES) && state.is(ReAnimalTags.Blocks.RED_PANDA_CLIMBABLE), 0.4D);

        this.setRequiredPathLength(TREE_PATH_RANGE);
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

    @Override
    public boolean isStableDestination(BlockPos pos) {
        return !this.isTreeBlock(pos.below()) && super.isStableDestination(pos);
    }

    private boolean isTreeBlock(BlockPos pos) {
        var state = this.level.getBlockState(pos);

        return state.is(BlockTags.LEAVES) || state.is(ReAnimalTags.Blocks.RED_PANDA_CLIMBABLE);
    }

    public boolean isInTree() {
        return this.climber.isClimbing() || this.climber.isInLeaves(this.climber.position())
                || this.climber.onGround() && this.isTreeBlock(this.climber.getOnPos());
    }

    public boolean canClimbTree() {
        return this.climber.onGround() && !this.wasInTree && this.canNavigate() && this.level.getGameTime() >= this.nextClimbTick;
    }

    public void recover() {
        var inTree = this.isInTree();
        var gameTime = this.level.getGameTime();

        if (inTree)
            this.wasInTree = true;
        else if (this.wasInTree && this.climber.onGround()) {
            this.wasInTree = false;
            this.nextClimbTick = gameTime + TREE_CLIMB_COOLDOWN.sample(this.climber.getRandom());
        }

        if (!inTree || !this.isDone() || !this.canNavigate() || gameTime < this.nextDescentSearchTick
                || this.climber.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING))
            return;

        this.nextDescentSearchTick = gameTime + 20L;

        var searchRange = TREE_PATH_RANGE * 2;
        var descentY = this.climber.getY() - TREE_PATH_RANGE;
        var path = this.createSurfacePath(searchRange, point -> point.position().y <= descentY
                || this.canFinishAt(point) && !this.isTreeBlock(point.cell().below()),
                point -> point.position().y, searchRange * searchRange);

        if (path != null) {
            this.climber.setStandState(RedPandaEntity.StandState.ON_ALL_FOURS);
            this.climber.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(Vec3.atBottomCenterOf(path.getTarget()), 1F, 0));
            this.moveTo(path, 1D);
        }
    }
}
