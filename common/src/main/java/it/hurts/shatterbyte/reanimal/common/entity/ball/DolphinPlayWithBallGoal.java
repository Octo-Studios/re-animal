package it.hurts.shatterbyte.reanimal.common.entity.ball;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.dolphin.Dolphin;

import java.util.EnumSet;

public class DolphinPlayWithBallGoal extends Goal {
    private final Dolphin dolphin;
    private final BallPlay play;

    public DolphinPlayWithBallGoal(Dolphin dolphin, BallPlay play) {
        this.dolphin = dolphin;
        this.play = play;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.dolphin.isInWater() || this.dolphin.getTarget() != null || this.dolphin.gotFish())
            return false;

        this.play.findBall(BallEntity::isInWater);

        return this.play.isPlaying();
    }

    @Override
    public boolean canContinueToUse() {
        return this.dolphin.isInWater() && this.dolphin.getTarget() == null && !this.dolphin.gotFish() && this.play.isPlaying()
                && (this.play.isWaiting() || !this.play.getBall().onGround() || this.play.getBall().isInWater());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        this.play.stop();
        this.dolphin.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.play.tick();

        if (!this.play.isPlaying())
            return;

        this.dolphin.getLookControl().setLookAt(this.play.getLookTarget(), this.dolphin.getMaxHeadYRot() + 20F, this.dolphin.getMaxHeadXRot());

        if (this.play.isWaiting()) {
            this.dolphin.getNavigation().stop();

            return;
        }

        var ball = this.play.getBall();

        if (!ball.isInWater()) {
            this.dolphin.getNavigation().moveTo(ball.getX(), this.dolphin.getY(), ball.getZ(), 1D);

            return;
        }

        var circle = this.play.getCircleTarget();

        if (circle != null) {
            this.dolphin.getNavigation().moveTo(circle.x, circle.y, circle.z, 1D);

            return;
        }

        if (this.dolphin.distanceToSqr(ball) > 1.8D * 1.8D) {
            var target = ball.position().add(0D, -0.4D, 0D);

            this.dolphin.getNavigation().moveTo(target.x, target.y, target.z, 1D);

            return;
        }

        this.dolphin.getNavigation().stop();
        this.play.hitBall();
    }
}
