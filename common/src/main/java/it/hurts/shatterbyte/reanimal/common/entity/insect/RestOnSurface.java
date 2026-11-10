package it.hurts.shatterbyte.reanimal.common.entity.insect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Map;

public class RestOnSurface extends Behavior<FlyingInsectEntity> {
    private static final int SEARCH_RADIUS = 6;
    private static final int APPROACH_TICKS = 200;

    private LandingSpot landing;
    private int approachTicks;
    private long nextSearchTick;

    public RestOnSurface() {
        super(Map.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT
        ), 400, 600);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, FlyingInsectEntity insect) {
        if (!insect.canLand() || level.getGameTime() < this.nextSearchTick)
            return false;

        if (insect.isResting()) {
            this.landing = new LandingSpot(insect.getRestingBlock(), insect.getRestingFace(), insect.position());

            return true;
        }

        var center = insect.getBoundingBox().getCenter();
        var candidates = new ArrayList<LandingSpot>(3);
        var found = new boolean[3];
        var directions = Direction.allShuffled(insect.getRandom());

        for (var pos : BlockPos.withinManhattan(insect.blockPosition(), SEARCH_RADIUS)) {
            if (candidates.size() == 3)
                break;

            if (!pos.closerToCenterThan(center, SEARCH_RADIUS) || level.getBlockState(pos).isAir())
                continue;

            for (var face : directions) {
                var kind = face == Direction.UP ? 0 : face == Direction.DOWN ? 2 : 1;

                if (found[kind] || !insect.canRestOn(pos, face))
                    continue;

                var adjacent = pos.relative(face);
                var space = level.getBlockState(adjacent);

                if (!space.getFluidState().isEmpty() || !space.getCollisionShape(level, adjacent).isEmpty())
                    continue;

                var halfHeight = insect.getBbHeight() * 0.5D;
                var extent = face.getAxis() == Direction.Axis.Y ? halfHeight : insect.getBbWidth() * 0.5D;
                var target = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.5D + extent)).subtract(0.0D, halfHeight, 0.0D);

                if (this.hasClearApproach(level, insect, target)) {
                    candidates.add(new LandingSpot(pos.immutable(), face, target));
                    found[kind] = true;
                }
            }
        }

        if (!insect.canFly() && candidates.size() > 1)
            candidates.removeIf(spot -> spot.face() == Direction.UP);

        if (!candidates.isEmpty()) {
            this.landing = candidates.get(insect.getRandom().nextInt(candidates.size()));

            return true;
        }

        this.nextSearchTick = level.getGameTime() + 100 + insect.getRandom().nextInt(100);

        return false;
    }

    @Override
    protected void start(ServerLevel level, FlyingInsectEntity insect, long timestamp) {
        this.approachTicks = 0;
        insect.getNavigation().stop();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, FlyingInsectEntity insect, long timestamp) {
        return insect.canLand() && !insect.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
                && (insect.isResting() || this.approachTicks < APPROACH_TICKS)
                && insect.canRestOn(this.landing.support(), this.landing.face())
                && level.getFluidState(BlockPos.containing(this.landing.target())).isEmpty()
                && this.hasClearApproach(level, insect, this.landing.target());
    }

    @Override
    protected void tick(ServerLevel level, FlyingInsectEntity insect, long timestamp) {
        if (insect.isResting() || insect.position().distanceToSqr(this.landing.target()) <= Math.max(insect.getDeltaMovement().lengthSqr(), MoveControl.MIN_SPEED_SQR))
            insect.restOn(this.landing.face(), this.landing.target());
        else {
            this.approachTicks++;
            insect.getMoveControl().setWantedPosition(this.landing.target().x, this.landing.target().y, this.landing.target().z, 1.0D);
        }
    }

    @Override
    protected void stop(ServerLevel level, FlyingInsectEntity insect, long timestamp) {
        insect.stopResting();
    }

    private boolean hasClearApproach(ServerLevel level, FlyingInsectEntity insect, Vec3 target) {
        return !level.isRainingAt(BlockPos.containing(target))
                && level.noCollision(insect, insect.getBoundingBox().expandTowards(target.subtract(insect.position())));
    }

    private record LandingSpot(BlockPos support, Direction face, Vec3 target) {
    }
}
