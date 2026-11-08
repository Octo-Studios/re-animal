package it.hurts.shatterbyte.reanimal.common.entity.ball;

import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Predicate;

public class BallPlay {
    private static final UniformInt PLAY_DURATION = UniformInt.of(400, 800);
    private static final UniformInt PLAY_COOLDOWN = UniformInt.of(600, 1200);
    private static final double PLAY_RANGE = 32D;

    private final Mob mob;
    private final SoundEvent sound;
    @Nullable
    private BallEntity ball;
    private long playUntil;
    private long playAgainAt;
    private long playStoppedAt;
    private long lastThrowAt;
    private long nextSearch;
    private long nextHit;
    private long circleUntil;
    private long returnAt;
    private long waitUntil;

    public BallPlay(Mob mob, SoundEvent sound) {
        this.mob = mob;
        this.sound = sound;
    }

    @Nullable
    public BallEntity getBall() {
        return this.ball;
    }

    public boolean isPlaying() {
        return this.ball != null && this.ball.isAlive() && this.mob.distanceToSqr(this.ball) <= PLAY_RANGE * PLAY_RANGE;
    }

    public void start(BallEntity ball) {
        if (this.ball != null)
            this.ball.setPlayingAnimal(null);

        this.ball = ball;
        ball.setPlayingAnimal(this.mob);
        this.lastThrowAt = ball.getLastThrownAt();
        this.playUntil = this.mob.level().getGameTime() + PLAY_DURATION.sample(this.mob.getRandom());
        this.nextHit = this.mob.level().getGameTime();
        this.circleUntil = 0;
        this.returnAt = 0;
        this.waitUntil = 0;
    }

    public void stop() {
        if (this.ball == null)
            return;

        if (this.ball.getVehicle() == this.mob)
            this.ball.stopRiding();

        this.ball.setPlayingAnimal(null);
        this.ball = null;
        this.playStoppedAt = this.mob.level().getGameTime();
        this.playAgainAt = this.playStoppedAt + PLAY_COOLDOWN.sample(this.mob.getRandom());
    }

    public void tick() {
        if (this.ball == null)
            return;

        var gameTime = this.mob.level().getGameTime();

        if (!this.isPlaying())
            this.stop();
        else if (this.ball.getLastThrownAt() != this.lastThrowAt)
            this.start(this.ball);
        else if (gameTime >= this.playUntil || this.waitUntil > 0 && gameTime >= this.waitUntil)
            this.stop();
    }

    public void findBall(Predicate<BallEntity> available) {
        var gameTime = this.mob.level().getGameTime();

        if (this.isPlaying() || gameTime < this.nextSearch)
            return;

        this.nextSearch = gameTime + 20;

        var ball = this.mob.level().getEntitiesOfClass(BallEntity.class, this.mob.getBoundingBox().inflate(PLAY_RANGE), candidate -> candidate.isAlive() && candidate.isAvailableFor(this.mob)
                        && available.test(candidate) && this.mob.distanceToSqr(candidate) <= PLAY_RANGE * PLAY_RANGE
                        && (gameTime >= this.playAgainAt || candidate.getLastThrownAt() > this.playStoppedAt) && this.mob.hasLineOfSight(candidate))
                .stream().min(Comparator.comparingDouble(this.mob::distanceToSqr)).orElse(null);

        if (ball != null)
            this.start(ball);
    }

    public void giveBall(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        var ball = ReAnimalEntities.BALL.create(this.mob.level(), EntitySpawnReason.TRIGGERED);

        ball.setItem(stack);
        ball.setThrower(player);
        ball.setPos(this.mob.position().add(this.mob.getLookAngle().multiply(1D, 0D, 1D).scale(this.mob.getBbWidth())).add(0D, 0.25D, 0D));
        this.mob.level().addFreshEntity(ball);
        this.start(ball);
        stack.consume(1, player);
    }

    public boolean isWaiting() {
        return this.waitUntil > 0 || this.ball.isReturning();
    }

    public void waitForPlayer() {
        this.waitUntil = this.mob.level().getGameTime() + 160;
    }

    public Entity getLookTarget() {
        var thrower = this.ball.getThrower();

        return (this.isWaiting() || this.returnAt > 0) && thrower != null && thrower.level() == this.mob.level() ? thrower : this.ball;
    }

    @Nullable
    public Vec3 getCircleTarget() {
        var gameTime = this.mob.level().getGameTime();

        if (gameTime >= this.circleUntil || !this.mob.isInWater())
            return null;

        var angle = gameTime * 0.12D + this.mob.getId();
        var target = this.ball.position().add(Math.cos(angle) * 1.8D, -0.5D, Math.sin(angle) * 1.8D);

        return this.mob.level().getFluidState(BlockPos.containing(target)).is(FluidTags.WATER) ? target : null;
    }

    public void hitBall() {
        this.hitBall(true);
    }

    public void hitBall(boolean returnToPlayer) {
        var gameTime = this.mob.level().getGameTime();

        if (gameTime < this.nextHit)
            return;

        var thrower = this.ball.getThrower();

        if (returnToPlayer && this.ball.playHits >= 2 && thrower != null && thrower.isAlive() && !thrower.isSpectator()
                && thrower.level() == this.mob.level() && this.ball.distanceToSqr(thrower) <= PLAY_RANGE * PLAY_RANGE && this.mob.hasLineOfSight(thrower)) {
            this.mob.getLookControl().setLookAt(thrower, this.mob.getMaxHeadYRot() + 20F, this.mob.getMaxHeadXRot());
            this.mob.setYRot(Mth.approachDegrees(this.mob.getYRot(),
                    (float) Mth.atan2(thrower.getZ() - this.mob.getZ(), thrower.getX() - this.mob.getX()) * Mth.RAD_TO_DEG - 90F, 10F));
            this.mob.yBodyRot = this.mob.getYRot();

            if (this.returnAt == 0) {
                this.returnAt = gameTime + 20;
                this.mob.playSound(this.sound, 0.5F, 1F);
            }

            if (gameTime >= this.returnAt) {
                this.ball.returnTo(thrower);
                this.returnAt = 0;
                this.waitForPlayer();
            }

            return;
        }

        this.returnAt = 0;

        var direction = this.ball.position().subtract(this.mob.position()).multiply(1D, 0D, 1D).normalize();
        var waterAhead = this.mob.level().getFluidState(BlockPos.containing(this.ball.position().add(direction.scale(3D)))).is(FluidTags.WATER);
        var action = this.mob.getRandom().nextInt(6);

        switch (action) {
            case 0 -> {
                if (waterAhead)
                    this.ball.kick(direction.scale(0.45D).add(0D, 0.15D, 0D));
                else
                    this.ball.kick(new Vec3(0D, 0.4D, 0D));
            }
            case 1 -> this.circleUntil = gameTime + 20;
            default -> {
                var horizontal = this.ball.isInWater() ? 0.12D : 0.06D;

                if (this.ball.isInWater() && !waterAhead)
                    horizontal = 0D;

                this.ball.kick(direction.scale(horizontal).add(0D, this.ball.isInWater() ? 0.5D : 0.35D, 0D));
            }
        }

        if (action != 1 && thrower != null)
            this.ball.playHits++;

        this.nextHit = gameTime + 20;
    }

    public void save(ValueOutput output) {
        output.putLong("BallPlayAgainAt", this.playAgainAt);
        output.putLong("BallPlayStoppedAt", this.playStoppedAt);
    }

    public void load(ValueInput input) {
        this.playAgainAt = input.getLongOr("BallPlayAgainAt", 0L);
        this.playStoppedAt = input.getLongOr("BallPlayStoppedAt", 0L);
    }
}
