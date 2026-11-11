package it.hurts.shatterbyte.reanimal.common.entity.insect;

import it.hurts.shatterbyte.reanimal.common.entity.PerchingAnimalEntity;
import it.hurts.shatterbyte.reanimal.common.entity.SurfaceClimber;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public abstract class FlyingInsectEntity extends PerchingAnimalEntity implements SurfaceClimber {
    private static final EntityDataAccessor<Integer> RESTING_FACE = SynchedEntityData.defineId(FlyingInsectEntity.class, EntityDataSerializers.INT);
    private static final int REST_TRANSITION_TICKS = 10;
    private static final double FLIGHT_ACCELERATION = 0.02D;
    private static final double FLIGHT_DRAG = 0.91D;

    private final InsectSurfaceNavigation surfaceNavigation;
    private final Quaternionf previousRestRotation = new Quaternionf();
    private final Quaternionf restRotation = new Quaternionf();
    private boolean seekingShelter;
    private int shelterSearchTicks;
    private int nextLandingTick;
    private Vec3 takeoffTarget;
    private Direction renderRestingFace = Direction.UP;
    private float previousRestProgress;
    private float restProgress;
    private float previousRestYaw;
    private float restYaw;
    private float restTargetYaw;

    protected FlyingInsectEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);

        this.surfaceNavigation = new InsectSurfaceNavigation(this, level);
        this.moveControl = new FlyingMoveControl<>(this, 10, true) {
            @Override
            public void tick() {
                var insect = FlyingInsectEntity.this;
                if (insect.seekingShelter || insect.isResting() || insect.isPerched())
                    return;

                var moving = this.hasWanted();

                super.tick();

                if (moving) {
                    var movement = new Vec3(this.wantedX, this.wantedY, this.wantedZ).subtract(insect.position());
                    var distance = movement.length();
                    var speed = insect.getSpeed() * FLIGHT_ACCELERATION / (1D - FLIGHT_DRAG);

                    insect.setYya(0F);
                    insect.setZza(0F);
                    insect.setDeltaMovement(distance < MIN_SPEED ? Vec3.ZERO : movement.scale(Math.min(speed, distance) / distance));
                } else if (insect.level().isRaining() && !insect.isExposedToRain()
                        && !insect.hasDryPath(insect.position(), insect.position().add(insect.getDeltaMovement())))
                    insect.setDeltaMovement(Vec3.ZERO);
            }
        };
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        var navigation = new InsectNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(RESTING_FACE, -1);
    }

    public Direction getRestingFace() {
        var face = this.getEntityData().get(RESTING_FACE);

        return face < 0 ? null : Direction.from3DDataValue(face);
    }

    public boolean isResting() {
        return this.getEntityData().get(RESTING_FACE) >= 0;
    }

    @Override
    public Direction getClimbingSide() {
        var face = this.getRestingFace();
        return face == null ? Direction.DOWN : face.getOpposite();
    }

    @Override
    public void setClimbingSide(Direction side, Vec3 movement) {
        var face = side.getOpposite();
        if (face != this.getRestingFace()) {
            this.getEntityData().set(RESTING_FACE, face.get3DDataValue());
            this.setOnGround(side == Direction.DOWN);
            this.setNoGravity(true);
        }
    }

    InsectSurfaceNavigation getSurfaceNavigation() {
        return this.surfaceNavigation;
    }

    public boolean isExposedToRain() {
        return this.level().isRainingAt(this.blockPosition());
    }

    boolean isSeekingShelter() {
        return this.seekingShelter;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos) {
        return this.level().isRainingAt(pos) ? Float.NEGATIVE_INFINITY : super.getWalkTargetValue(pos);
    }

    boolean hasDryPath(Vec3 from, Vec3 target) {
        var level = this.level();
        return !level.isRaining() || !level.isRainingAt(BlockPos.containing(target))
                && BlockGetter.traverseBlocks(from, target, level,
                        (world, pos) -> world.isRainingAt(pos) ? false : null, world -> true);
    }

    public boolean canFlyDirectlyTo(Vec3 target) {
        return this.hasDryPath(this.position(), target)
                && this.level().noCollision(this, this.getBoundingBox().expandTowards(target.subtract(this.position())));
    }

    protected boolean updateRainNavigation(ServerLevel level) {
        var exposed = this.isExposedToRain();

        if (exposed == this.seekingShelter)
            return false;

        this.seekingShelter = exposed;
        var brain = (Brain<FlyingInsectEntity>) this.getBrain();

        brain.stopAll(level, this);
        this.stopResting();
        this.getNavigation().stop();
        this.getMoveControl().setWait();
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
        brain.eraseMemory(MemoryModuleType.PATH);
        brain.eraseMemory(MemoryModuleType.BREED_TARGET);
        this.setYya(0F);
        this.setZza(0F);
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(!exposed);
        this.shelterSearchTicks = 0;
        if (exposed)
            this.takeoffTarget = null;

        return true;
    }

    private void tickRainShelter() {
        if (this.shelterSearchTicks > 0)
            --this.shelterSearchTicks;

        if (!this.isResting() && !this.onGround())
            return;

        if (this.surfaceNavigation.isDone() && this.shelterSearchTicks == 0) {
            this.shelterSearchTicks = 20;
            if (!this.surfaceNavigation.seekShelter())
                this.surfaceNavigation.stroll();
        }

        this.tickSurfaceNavigation();
    }

    @Override
    public boolean canPerch() {
        return super.canPerch() && this.canLand() && !this.isResting();
    }

    @Override
    public boolean canPerchOn(LivingEntity host) {
        return super.canPerchOn(host) && (!this.level().isRaining() || this.hasDryPath(this.position(), this.getPerchPosition(host)));
    }

    @Override
    public float getPerchFootSpan() {
        return 3F / 16F;
    }

    @Override
    protected void onPerchTakeoff() {
        this.delayLanding();
    }

    public float getRestProgress(float partialTick) {
        return Mth.lerp(partialTick, this.previousRestProgress, this.restProgress);
    }

    public float getRestYaw(float partialTick) {
        return Mth.rotLerp(partialTick, this.previousRestYaw, this.restYaw);
    }

    public Quaternionf getRestRotation(float partialTick) {
        return new Quaternionf(this.previousRestRotation).slerp(this.restRotation, partialTick);
    }

    @Override
    public void calculateEntityAnimation(boolean useY) {
        super.calculateEntityAnimation(useY || this.isResting());
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.onGround() && !this.isNoGravity()) {
            var movement = this.getDeltaMovement();

            if (movement.y < 0D)
                this.setDeltaMovement(movement.multiply(1D, 0.6D, 1D));
        }

        if (this.level().isClientSide()) {
            this.previousRestProgress = this.restProgress;
            this.previousRestYaw = this.restYaw;
            this.previousRestRotation.set(this.restRotation);

            var face = this.getRestingFace();
            if (face != null) {
                var rotation = face.getRotation();
                var movement = this.position().subtract(this.xo, this.yo, this.zo).with(face.getAxis(), 0D);
                var moving = movement.lengthSqr() > 0D;

                if (moving || this.restProgress == 0F || face != this.renderRestingFace) {
                    var facing = moving ? movement : Vec3.directionFromRotation(this.getXRot(), this.yBodyRot);
                    var look = rotation.transformInverse(facing.toVector3f());
                    this.restTargetYaw = (float) Mth.atan2(-look.x, look.z) * Mth.RAD_TO_DEG;
                }

                if (this.restProgress == 0F) {
                    this.restRotation.set(rotation);
                    this.previousRestRotation.set(rotation);
                } else
                    this.restRotation.slerp(rotation, 0.25F);

                this.renderRestingFace = face;
                this.restYaw = Mth.approachDegrees(this.restYaw, this.restTargetYaw, this.getHeadRotSpeed());
                this.restProgress = Math.min(1F, this.restProgress + 1F / REST_TRANSITION_TICKS);
            } else {
                this.restProgress = Math.max(0F, this.restProgress - 1F / REST_TRANSITION_TICKS);
                if (this.restProgress == 0F)
                    this.restYaw = this.yBodyRot;
            }
        }
    }

    boolean canLand() {
        return !this.isExposedToRain() && this.tickCount >= this.nextLandingTick && this.hurtTime == 0 && !this.isInWater() && !this.isPassenger()
                && !this.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING)
                && !this.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                && !this.getBrain().hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER);
    }

    boolean canRestOn(BlockPos pos, Direction face) {
        var state = this.level().getBlockState(pos);

        return !this.getType().isBlockDangerous(state) && Block.isFaceFull(state.getCollisionShape(this.level(), pos), face);
    }

    void delayLanding() {
        this.nextLandingTick = this.tickCount + 100 + this.random.nextInt(100);
    }

    void restOn(Direction face, Vec3 position) {
        this.getEntityData().set(RESTING_FACE, face.get3DDataValue());
        this.setPos(position);
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        this.setOnGround(face == Direction.UP);
        this.setSpeed(0.0F);
        this.setYya(0.0F);
        this.setZza(0.0F);
        this.setXRot(0.0F);
        this.getNavigation().stop();
        this.getMoveControl().setWait();
    }

    public void stopResting() {
        if (this.getVehicle() instanceof LivingEntity host)
            this.leavePerch(host);

        var face = this.getRestingFace();

        if (face != null) {
            this.surfaceNavigation.stop();
            this.getEntityData().set(RESTING_FACE, -1);
            if (this.seekingShelter)
                this.setNoGravity(false);
            else
                this.takeoffTarget = this.position().add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(this.getBbWidth()));
        }

        this.delayLanding();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (this.seekingShelter) {
            this.tickRainShelter();
            return;
        }

        if (this.takeoffTarget != null) {
            if (!this.getNavigation().isDone() || this.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
                    || this.position().distanceToSqr(this.takeoffTarget) <= Math.max(this.getDeltaMovement().lengthSqr(), MoveControl.MIN_SPEED_SQR)
                    || !this.canFlyDirectlyTo(this.takeoffTarget))
                this.takeoffTarget = null;
            else
                this.getMoveControl().setWantedPosition(this.takeoffTarget.x, this.takeoffTarget.y, this.takeoffTarget.z, 1.0D);
        }

        if (!this.isResting())
            return;

        if (!this.canLand() || this.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
            this.stopResting();
            return;
        }

        this.tickSurfaceNavigation();
    }

    private void tickSurfaceNavigation() {
        this.surfaceNavigation.tick();
        if (this.isResting() && this.surfaceNavigation.isDone() && !this.surfaceNavigation.hasSupport(this.position(), this.getClimbingSide()))
            this.stopResting();
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isResting()) {
            var movement = this.isEffectiveAi() ? this.surfaceNavigation.getMotion() : Vec3.ZERO;
            this.setDeltaMovement(movement);

            if (movement.lengthSqr() > 0D) {
                this.move(MoverType.SELF, movement);
                this.setOnGround(this.getClimbingSide() == Direction.DOWN);
                this.resetFallDistance();
            }
        } else if (this.isPerched())
            this.setDeltaMovement(Vec3.ZERO);
        else
            super.travel(input);
    }

    @Override
    public boolean isPushable() {
        return !this.isResting() && !this.isPerched() && super.isPushable();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var hurt = super.hurtServer(level, source, amount);

        if (hurt)
            this.stopResting();

        return hurt;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);

        output.putInt("RestingFace", this.getEntityData().get(RESTING_FACE));
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        this.getEntityData().set(RESTING_FACE, input.getIntOr("RestingFace", -1));
    }
}
