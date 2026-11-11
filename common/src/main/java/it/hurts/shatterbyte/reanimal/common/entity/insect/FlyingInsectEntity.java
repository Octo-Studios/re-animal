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
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;

public abstract class FlyingInsectEntity extends PerchingAnimalEntity {
    private static final EntityDataAccessor<Integer> RESTING_FACE = SynchedEntityData.defineId(FlyingInsectEntity.class, EntityDataSerializers.INT);
    private static final int REST_TRANSITION_TICKS = 10;
    private static final double FLIGHT_ACCELERATION = 0.02D;
    private static final double FLIGHT_DRAG = 0.91D;

    private boolean seekingShelter;
    private int shelterSearchTicks;
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
                var insect = FlyingInsectEntity.this;
                if (insect.isResting() || insect.isPerched())
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
                        && !insect.hasDryFlightPath(insect.position().add(insect.getDeltaMovement())))
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

    public boolean isExposedToRain() {
        return this.level().isRainingAt(this.blockPosition());
    }

    @Override
    public float getWalkTargetValue(BlockPos pos) {
        return this.level().isRainingAt(pos) ? Float.NEGATIVE_INFINITY : super.getWalkTargetValue(pos);
    }

    boolean hasDryFlightPath(Vec3 target) {
        var level = this.level();
        return !level.isRaining() || !level.isRainingAt(BlockPos.containing(target))
                && BlockGetter.traverseBlocks(this.position(), target, level,
                        (world, pos) -> world.isRainingAt(pos) ? false : null, world -> true);
    }

    public boolean canFlyDirectlyTo(Vec3 target) {
        return this.hasDryFlightPath(target)
                && this.level().noCollision(this, this.getBoundingBox().expandTowards(target.subtract(this.position())));
    }

    protected boolean updateRainNavigation(ServerLevel level) {
        var exposed = this.isExposedToRain();

        if (exposed == this.seekingShelter)
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
        this.setDeltaMovement(Vec3.ZERO);
        this.seekingShelter = exposed;
        this.shelterSearchTicks = 0;

        return true;
    }

    private void tickRainShelter(ServerLevel level) {
        if (this.shelterSearchTicks > 0)
            --this.shelterSearchTicks;

        if (!this.getNavigation().isDone() || this.shelterSearchTicks > 0)
            return;

        this.shelterSearchTicks = 20;

        var range = Mth.ceil(this.getAttributeValue(Attributes.FOLLOW_RANGE));
        var shelters = new HashSet<BlockPos>();

        for (var pos : BlockPos.withinManhattan(this.blockPosition(), range)) {
            if (level.isEmptyBlock(pos) && !level.isRainingAt(pos))
                shelters.add(pos.immutable());
        }

        var path = this.getNavigation().createPath(shelters, 0);
        if (path != null && path.canReach())
            this.getNavigation().moveTo(path, 1D);
    }

    @Override
    public boolean canPerch() {
        return super.canPerch() && this.canLand() && !this.isResting();
    }

    @Override
    public boolean canPerchOn(LivingEntity host) {
        return super.canPerchOn(host) && (!this.level().isRaining() || this.hasDryFlightPath(this.getPerchPosition(host)));
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
        return !this.isExposedToRain() && this.tickCount >= this.nextLandingTick && this.hurtTime == 0 && !this.isInWater() && !this.isPassenger()
                && !this.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING)
                && !this.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
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

        if (this.seekingShelter)
            this.tickRainShelter(level);

        if (this.takeoffTarget != null) {
            if (!this.getNavigation().isDone() || this.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
                    || this.position().distanceToSqr(this.takeoffTarget) <= Math.max(this.getDeltaMovement().lengthSqr(), MoveControl.MIN_SPEED_SQR)
                    || !this.canFlyDirectlyTo(this.takeoffTarget))
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
