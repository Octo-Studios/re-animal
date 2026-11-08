package it.hurts.shatterbyte.reanimal.common.entity.ball;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.wolf.Wolf;

import java.util.EnumSet;

public class WolfPlayWithBallGoal extends Goal {
    private final Wolf wolf;
    private final BallPlay play;

    public WolfPlayWithBallGoal(Wolf wolf, BallPlay play) {
        this.wolf = wolf;
        this.play = play;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static boolean canPlay(Wolf wolf) {
        return wolf.isTame() && !wolf.isOrderedToSit() && wolf.getTarget() == null && !wolf.isAngry() && !wolf.isInLove();
    }

    @Override
    public boolean canUse() {
        if (!canPlay(this.wolf))
            return false;

        this.play.findBall(ball -> ball.getVehicle() == this.wolf || !ball.isInWater());

        return this.play.isPlaying();
    }

    @Override
    public boolean canContinueToUse() {
        return canPlay(this.wolf) && this.play.isPlaying();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        this.play.stop();
        this.wolf.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.play.tick();

        if (!this.play.isPlaying())
            return;

        var ball = this.play.getBall();
        var thrower = ball.getThrower();
        var canReturn = thrower != null && thrower.isAlive() && !thrower.isSpectator()
                && thrower.level() == this.wolf.level() && this.wolf.distanceToSqr(thrower) <= 32D * 32D;

        if (ball.getVehicle() == this.wolf) {
            if (!canReturn) {
                this.play.stop();
                this.wolf.getNavigation().stop();

                return;
            }

            this.wolf.getLookControl().setLookAt(thrower, this.wolf.getMaxHeadYRot() + 20F, this.wolf.getMaxHeadXRot());

            if (this.wolf.distanceToSqr(thrower) > 1.8D * 1.8D || !this.wolf.hasLineOfSight(thrower)) {
                this.wolf.getNavigation().moveTo(thrower, 1D);

                return;
            }

            this.wolf.getNavigation().stop();
            ball.kick(thrower.position().subtract(this.wolf.position()).multiply(1D, 0D, 1D).normalize().scale(0.12D).add(0D, 0.05D, 0D));
            ball.playHits = 0;
            this.play.waitForPlayer();

            return;
        }

        this.wolf.getLookControl().setLookAt(this.play.getLookTarget(), this.wolf.getMaxHeadYRot() + 20F, this.wolf.getMaxHeadXRot());

        if (this.play.isWaiting()) {
            this.wolf.getNavigation().stop();

            return;
        }

        var retrieving = canReturn && ball.playHits >= 2;

        if (this.wolf.distanceToSqr(ball) > (retrieving ? 1D : 1.5D * 1.5D)) {
            this.wolf.getNavigation().moveTo(ball.getX(), ball.onGround() ? ball.getY() : this.wolf.getY(), ball.getZ(), 1D);

            return;
        }

        this.wolf.getNavigation().stop();

        if (retrieving)
            ball.startRiding(this.wolf, true, true);
        else
            this.play.hitBall(false);
    }
}
