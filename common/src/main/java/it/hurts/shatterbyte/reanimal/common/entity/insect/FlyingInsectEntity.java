package it.hurts.shatterbyte.reanimal.common.entity.insect;

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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public abstract class FlyingInsectEntity extends Animal {
    private static final EntityDataAccessor<Integer> RESTING_FACE = SynchedEntityData.defineId(FlyingInsectEntity.class, EntityDataSerializers.INT);
    private static final int REST_TRANSITION_TICKS = 10;

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

        this.moveControl = new FlyingMoveControl<>(this, 10, false) {
            @Override
            public void tick() {
                if (!FlyingInsectEntity.this.isResting())
                    super.tick();
            }
        };
        this.navigation = new FlyingPathNavigation(this, level);
        this.navigation.setCanFloat(true);
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
        return this.tickCount >= this.nextLandingTick && this.hurtTime == 0 && !this.isInWater() && !this.isPassenger()
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

        if (this.takeoffTarget != null) {
            if (this.position().distanceToSqr(this.takeoffTarget) <= Math.max(this.getDeltaMovement().lengthSqr(), MoveControl.MIN_SPEED_SQR)
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
        if (this.isResting())
            this.setDeltaMovement(Vec3.ZERO);
        else
            super.travel(input);
    }

    @Override
    public boolean isPushable() {
        return !this.isResting() && super.isPushable();
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
