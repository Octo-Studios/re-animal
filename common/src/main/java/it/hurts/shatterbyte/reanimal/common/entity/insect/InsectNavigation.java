package it.hurts.shatterbyte.reanimal.common.entity.insect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jspecify.annotations.Nullable;

final class InsectNavigation extends FlyingPathNavigation {
    InsectNavigation(FlyingInsectEntity insect, Level level) {
        super(insect, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new FlyNodeEvaluator() {
            private boolean stayDry;

            @Override
            public void prepare(PathNavigationRegion region, Mob mob) {
                super.prepare(region, mob);
                this.stayDry = level.isRaining() && !level.isRainingAt(mob.blockPosition());
            }

            @Override
            protected @Nullable Node findAcceptedNode(int x, int y, int z) {
                return this.stayDry && level.isRainingAt(new BlockPos(x, y, z)) ? null : super.findAcceptedNode(x, y, z);
            }
        };
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    protected void followThePath() {
        var insect = (FlyingInsectEntity) this.mob;
        if (!this.level.isRaining() || insect.isExposedToRain()) {
            super.followThePath();
            return;
        }

        if (insect.position().distanceToSqr(this.path.getNextEntityPos(insect)) <= MoveControl.MIN_SPEED_SQR)
            this.path.advance();

        if (!this.path.isDone() && !insect.hasDryFlightPath(this.path.getNextEntityPos(insect))) {
            this.stop();
            insect.getMoveControl().setWait();
            return;
        }

        this.doStuckDetection(insect.position());
    }
}
