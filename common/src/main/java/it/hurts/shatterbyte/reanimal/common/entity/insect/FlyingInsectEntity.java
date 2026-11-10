package it.hurts.shatterbyte.reanimal.common.entity.insect;

import it.hurts.shatterbyte.reanimal.common.entity.PerchingAnimalEntity;
import lombok.Getter;
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
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class FlyingInsectEntity extends PerchingAnimalEntity {
    private static final EntityDataAccessor<Integer> RESTING_FACE = SynchedEntityData.defineId(FlyingInsectEntity.class, EntityDataSerializers.INT);
    private static final int REST_TRANSITION_TICKS = 10;
    private static final double FLIGHT_ACCELERATION = 0.02D;
    private static final double FLIGHT_DRAG = 0.91D;

    private final MoveControl flightMoveControl;
    private final PathNavigation flightNavigation;
    private boolean shelteringFromRain;
    private int shelterSearchTicks;
    private Vec3 rainLandingTarget;
    private int nextLandingTick;
    private Vec3 takeoffTarget;
    @Getter
    private Direction renderRestingFace = Direction.UP;
    private float previousRestProgress;
    private float restProgress;
    private float previousRestYaw;
    private float restYaw;

    protected FlyingInsectEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);

        this.moveControl = new FlyingMoveControl<>(this, 10, true) {
            @Override
            public void tick() {
                if (FlyingInsectEntity.this.isResting() || FlyingInsectEntity.this.isPerched())
                    return;

                var moving = this.hasWanted();

                super.tick();

                if (moving) {
                    var movement = new Vec3(this.wantedX, this.wantedY, this.wantedZ).subtract(FlyingInsectEntity.this.position());
                    var distance = movement.length();
                    var speed = FlyingInsectEntity.this.getSpeed() * FLIGHT_ACCELERATION / (1D - FLIGHT_DRAG);

                    FlyingInsectEntity.this.setYya(0F);
                    FlyingInsectEntity.this.setZza(0F);
                    FlyingInsectEntity.this.setDeltaMovement(distance < MIN_SPEED ? Vec3.ZERO : movement.scale(Math.min(speed, distance) / distance));
                }
            }
        };
        this.navigation = new FlyingPathNavigation(this, level);
        this.navigation.setCanFloat(true);
        this.flightMoveControl = this.moveControl;
        this.flightNavigation = this.navigation;
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

    public boolean canFly() {
        return !this.level().isRaining();
    }

    protected boolean updateRainNavigation(ServerLevel level) {
        var raining = !this.canFly();

        if (raining == this.shelteringFromRain)
            return false;

        var brain = (Brain<FlyingInsectEntity>) this.getBrain();

        brain.stopAll(level, this);
        this.stopResting();
        this.getNavigation().stop();
        this.getMoveControl().setWait();
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
        brain.eraseMemory(MemoryModuleType.PATH);
        brain.eraseMemory(MemoryModuleType.BREED_TARGET);
        this.navigation = this.flightNavigation;
        this.moveControl = this.flightMoveControl;
        this.setNoGravity(true);
        this.setYya(0F);
        this.setZza(0F);
        this.setXRot(0F);
        this.setDeltaMovement(Vec3.ZERO);
        this.shelteringFromRain = raining;
        this.shelterSearchTicks = 0;
        this.rainLandingTarget = null;

        return true;
    }

    private void tickRainShelter(ServerLevel level) {
        if (this.shelterSearchTicks > 0)
            --this.shelterSearchTicks;

        if (this.navigation == this.flightNavigation) {
            if (!this.landForRain(level))
                return;

            this.getNavigation().stop();
            this.getMoveControl().setWait();
            this.navigation = new GroundPathNavigation(this, level);
            this.navigation.setCanFloat(true);
            this.moveControl = new MoveControl(this);
            this.setNoGravity(false);
            this.setYya(0F);
            this.setZza(0F);
            this.setXRot(0F);
            this.setDeltaMovement(Vec3.ZERO);
            this.rainLandingTarget = null;
            this.shelterSearchTicks = 0;
        }

        if (!level.isRainingAt(this.blockPosition())) {
            this.getNavigation().stop();
            this.getMoveControl().setWait();

            return;
        }

        if (!this.onGround() || !this.getNavigation().isDone() || this.shelterSearchTicks > 0)
            return;

        this.shelterSearchTicks = 20;

        var range = Mth.ceil(this.getAttributeValue(Attributes.FOLLOW_RANGE));
        var height = Mth.ceil(this.maxUpStep());

        for (var pos : BlockPos.withinBoxByManhattanDistance(this.blockPosition(), range, height, range)) {
            if (level.isRainingAt(pos) || !level.getFluidState(pos).isEmpty() || !this.navigation.isStableDestination(pos)
                    || !level.noCollision(this, this.getBoundingBox().move(Vec3.atBottomCenterOf(pos).subtract(this.position()))))
                continue;

            var path = this.navigation.createPath(pos, 0);

            if (path != null && path.canReach()) {
                this.navigation.moveTo(path, 1D);

                return;
            }
        }

        var pos = LandRandomPos.getPos(this, range, height);

        if (pos != null)
            this.navigation.moveTo(pos.x, pos.y, pos.z, 1D);
    }

    private boolean landForRain(ServerLevel level) {
        if (this.onGround())
            return true;

        if (this.rainLandingTarget == null && this.shelterSearchTicks == 0) {
            this.shelterSearchTicks = 20;

            var range = Mth.ceil(this.getAttributeValue(Attributes.FOLLOW_RANGE));

            for (var pos : BlockPos.withinBoxByManhattanDistance(this.blockPosition(), range, 0, range)) {
                var start = new Vec3(pos.getX() + 0.5D, this.getY(), pos.getZ() + 0.5D);
                var hit = level.clip(new ClipContext(start, new Vec3(start.x, level.getMinY(), start.z), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));

                if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP
                        || !level.getFluidState(hit.getBlockPos()).isEmpty() || !this.canRestOn(hit.getBlockPos(), Direction.UP))
                    continue;

                var target = Vec3.atBottomCenterOf(hit.getBlockPos().above());

                if (!level.noCollision(this, this.getBoundingBox().expandTowards(target.subtract(this.position()))))
                    continue;

                this.rainLandingTarget = target;

                break;
            }
        }

        if (this.rainLandingTarget == null)
            return false;

        if (this.position().distanceToSqr(this.rainLandingTarget) <= Math.max(this.getDeltaMovement().lengthSqr(), MoveControl.MIN_SPEED_SQR)) {
            this.setPos(this.rainLandingTarget);
            this.setOnGround(true);

            return true;
        }

        this.getMoveControl().setWantedPosition(this.rainLandingTarget.x, this.rainLandingTarget.y, this.rainLandingTarget.z, 0.5D);

        return false;
    }

    @Override
    public boolean canPerch() {
        return super.canPerch() && this.canLand() && !this.isResting();
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

            var face = this.getRestingFace();

            if (face != null) {
                this.renderRestingFace = face;
                this.restProgress = Math.min(1.0F, this.restProgress + 1.0F / REST_TRANSITION_TICKS);
            } else
                this.restProgress = Math.max(0.0F, this.restProgress - 1.0F / REST_TRANSITION_TICKS);

            if (this.restProgress > 0.0F && this.renderRestingFace != Direction.UP) {
                var look = Vec3.directionFromRotation(this.getXRot(), this.yBodyRot).toVector3f();

                this.renderRestingFace.getRotation().conjugate().transform(look);
                this.restYaw = Mth.approachDegrees(this.restYaw, (float) Mth.atan2(-look.x, look.z) * Mth.RAD_TO_DEG, this.getHeadRotSpeed());
            } else
                this.restYaw = this.yBodyRot;
        }
    }

    boolean canLand() {
        return this.canFly() && this.tickCount >= this.nextLandingTick && this.hurtTime == 0 && !this.isInWater() && !this.isPassenger()
                && !this.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING)
                && !this.getBrain().hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER);
    }

    boolean canRestOn(BlockPos pos, Direction face) {
        var state = this.level().getBlockState(pos);

        return !this.getType().isBlockDangerous(state) && Block.isFaceFull(state.getCollisionShape(this.level(), pos), face);
    }

    BlockPos getRestingBlock() {
        var face = this.getRestingFace();
        var halfHeight = this.getBbHeight() * 0.5D;
        var extent = face.getAxis() == Direction.Axis.Y ? halfHeight : this.getBbWidth() * 0.5D;

        return BlockPos.containing(this.position().add(0.0D, halfHeight, 0.0D).subtract(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(extent + 0.5D)));
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
    }

    public void stopResting() {
        if (this.getVehicle() instanceof LivingEntity host)
            this.leavePerch(host);

        var face = this.getRestingFace();

        if (face != null) {
            this.getEntityData().set(RESTING_FACE, -1);
            this.takeoffTarget = this.position().add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(this.getBbWidth()));
        }

        this.delayLanding();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (!this.canFly())
            this.tickRainShelter(level);

        if (this.takeoffTarget != null) {
            if (!this.canFly() || !this.getNavigation().isDone() || this.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
                    || this.position().distanceToSqr(this.takeoffTarget) <= Math.max(this.getDeltaMovement().lengthSqr(), MoveControl.MIN_SPEED_SQR)
                    || !level.noCollision(this, this.getBoundingBox().expandTowards(this.takeoffTarget.subtract(this.position()))))
                this.takeoffTarget = null;
            else
                this.getMoveControl().setWantedPosition(this.takeoffTarget.x, this.takeoffTarget.y, this.takeoffTarget.z, 1.0D);
        }

        var face = this.getRestingFace();

        if (face == null)
            return;

        if (!this.canLand() || this.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET) || !this.canRestOn(this.getRestingBlock(), face))
            this.stopResting();
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isResting() || this.isPerched())
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
