package it.hurts.shatterbyte.reanimal.common.entity.crocodile;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import it.hurts.shatterbyte.reanimal.common.block.CrocodileEggBlock;
import it.hurts.shatterbyte.reanimal.init.ReAnimalBlocks;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

import org.jspecify.annotations.Nullable;

public class CrocodileEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.crocodile.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.crocodile.walk");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.crocodile.swim");
    private static final RawAnimation SWIM_IDLE = RawAnimation.begin().thenLoop("animation.crocodile.swim_idle");
    private static final RawAnimation LAND_POSE = RawAnimation.begin().thenLoop("animation.crocodile.land_pose");
    private static final RawAnimation SWIM_POSE = RawAnimation.begin().thenLoop("animation.crocodile.swim_pose");
    private static final RawAnimation AMBUSH = RawAnimation.begin().thenLoop("animation.crocodile.ambush");
    private static final RawAnimation BASK = RawAnimation.begin().thenLoop("animation.crocodile.bask");
    private static final RawAnimation HOLD = RawAnimation.begin().thenPlayAndHold("animation.crocodile.hold");
    private static final RawAnimation DASH_LAND = RawAnimation.begin().thenLoop("animation.crocodile.dash_land");
    private static final RawAnimation DASH_SWIM = RawAnimation.begin().thenLoop("animation.crocodile.dash_swim");
    private static final RawAnimation BITE = RawAnimation.begin().then("animation.crocodile.bite", LoopType.PLAY_ONCE);
    private static final RawAnimation SPIN = RawAnimation.begin().then("animation.crocodile.spinn", LoopType.PLAY_ONCE);

    private static final String BITE_TRIGGER = "bite";
    private static final String SPIN_TRIGGER = "spin";
    private static final int TURN_SPEED = 10;
    private static final int DASH_DURATION = 10;
    private static final double BITE_REACH = 24.5D / 16D;
    private static final double BITE_RADIUS = 4D / 16D;
    private static final Vec3 BITE_START = new Vec3(0D, 4D / 16D, 10D / 16D);
    private static final Vec3 BITE_END = new Vec3(0D, 4D / 16D, BITE_REACH);

    private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(CrocodileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HAS_EGG = SynchedEntityData.defineId(CrocodileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> AMBUSHING = SynchedEntityData.defineId(CrocodileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BASKING = SynchedEntityData.defineId(CrocodileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DASHING = SynchedEntityData.defineId(CrocodileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SPINNING = SynchedEntityData.defineId(CrocodileEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int attackAnimationTicks;
    private int dashTicks;
    private int dashCooldown;
    private Vec3 dashDirection = Vec3.ZERO;
    private float swimPitch;
    private float previousSwimPitch;

    @Nullable
    private BlockPos dragTarget;

    @Nullable
    private BlockPos environmentTarget;

    public CrocodileEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.moveControl = new SmoothSwimmingMoveControl<>(this, 85, TURN_SPEED, 0.4F, 1F, false) {
            private float movementPitch;

            @Override
            public void tick() {
                if (this.mob.isInWater())
                    this.mob.setXRot(this.movementPitch);

                super.tick();

                this.movementPitch = this.mob.isInWater() && !this.mob.getNavigation().isDone() ? this.mob.getXRot() : 0F;
            }
        };
        this.lookControl = new SmoothSwimmingLookControl(this, 10) {
            @Override
            public void tick() {
                if (!CrocodileEntity.this.isAmbushing() && !CrocodileEntity.this.isBasking()
                        && !CrocodileEntity.this.isAttacking() && !CrocodileEntity.this.isDashing())
                    super.tick();
            }
        };
        this.navigation = new AmphibiousPathNavigation(this, level);

        this.setPathfindingMalus(PathType.WATER, 0F);
    }

    @Override
    public int getHeadRotSpeed() {
        return 5;
    }

    @Override
    public int getMaxHeadXRot() {
        return 5;
    }

    public boolean isAttacking() {
        return this.entityData.get(ATTACKING);
    }

    public void setAttacking(boolean attacking) {
        this.entityData.set(ATTACKING, attacking);
    }

    public boolean hasEgg() {
        return this.entityData.get(HAS_EGG);
    }

    public void setHasEgg(boolean hasEgg) {
        this.entityData.set(HAS_EGG, hasEgg);
    }

    public boolean isAmbushing() {
        return this.entityData.get(AMBUSHING);
    }

    public void setAmbushing(boolean ambushing) {
        this.entityData.set(AMBUSHING, ambushing);
    }

    public boolean isBasking() {
        return this.entityData.get(BASKING);
    }

    public void setBasking(boolean basking) {
        this.entityData.set(BASKING, basking);
    }

    public boolean isDashing() {
        return this.entityData.get(DASHING);
    }

    public boolean isSpinning() {
        return this.isAttacking() && this.entityData.get(SPINNING);
    }

    public boolean canBite(LivingEntity target) {
        return this.canBite(target, this.yBodyRot);
    }

    boolean isWithinBiteRange(LivingEntity target) {
        return this.canBite(target, this.yawTo(target));
    }

    private boolean canBite(LivingEntity target, float yaw) {
        var scale = this.getScale() * this.getAgeScale();
        var pitch = this.isInWater() ? this.swimPitch * Mth.DEG_TO_RAD : 0F;
        var rotation = -yaw * Mth.DEG_TO_RAD;
        var start = this.position().add(BITE_START.scale(scale).xRot(pitch).yRot(rotation));
        var end = this.position().add(BITE_END.scale(scale).xRot(pitch).yRot(rotation));
        var bounds = target.getBoundingBox().inflate(BITE_RADIUS * scale);

        return (bounds.contains(start) || bounds.contains(end) || bounds.clip(start, end).isPresent()) && this.hasLineOfSight(target);
    }

    public void turnToward(LivingEntity target) {
        this.setFacing(Mth.approachDegrees(this.yBodyRot, this.yawTo(target), TURN_SPEED));
    }

    private float yawTo(LivingEntity target) {
        var offset = target.position().subtract(this.position());

        return (float) Mth.atan2(-offset.x, offset.z) * Mth.RAD_TO_DEG;
    }

    private void setFacing(float yaw) {
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.setYHeadRot(yaw);
    }

    @Override
    protected void tickHeadTurn(float yBodyRot) {
        if (this.isAttacking() || this.isDashing())
            this.yBodyRot = this.getYRot();
        else
            super.tickHeadTurn(yBodyRot);
    }

    public void startAttackAnimation(boolean spinning) {
        this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.getNavigation().stop();
        this.setDeltaMovement(0D, this.isInWater() ? 0D : this.getDeltaMovement().y, 0D);

        this.attackAnimationTicks = spinning ? CrocodileAI.SPIN_ANIMATION_TICKS : CrocodileAI.BITE_ANIMATION_TICKS;
        this.setFacing(this.yBodyRot);
        this.entityData.set(SPINNING, spinning);
        this.setAttacking(true);
        this.stopDash();

        if (spinning)
            this.triggerAnim("spin", SPIN_TRIGGER);

        if (this.getGrabbedPrey() == null)
            this.triggerAnim("mouth", BITE_TRIGGER);
    }

    public void tryDash(LivingEntity target) {
        var offset = target.position().subtract(this.position());
        var reach = this.getDashSpeed() * DASH_DURATION + (BITE_REACH + BITE_RADIUS) * this.getScale();

        if (this.isBaby() || this.dashCooldown > 0 || this.isDashing() || this.isAttacking() || this.isVehicle()
                || !this.isInWater() && !this.onGround() || offset.lengthSqr() < 4D || offset.horizontalDistanceSqr() == 0D
                || target.getBoundingBox().distanceToSqr(this.position()) > reach * reach)
            return;

        var yaw = (float) Mth.atan2(-offset.x, offset.z) * Mth.RAD_TO_DEG;

        if (Math.abs(Mth.wrapDegrees(yaw - this.yBodyRot)) > TURN_SPEED || !this.hasLineOfSight(target))
            return;

        this.setFacing(yaw);
        this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.getNavigation().stop();
        this.dashDirection = offset.multiply(1D, this.isInWater() ? 1D : 0D, 1D).normalize();
        this.dashTicks = DASH_DURATION;
        this.dashCooldown = 80 + this.random.nextInt(80);
        this.entityData.set(DASHING, true);
    }

    private double getDashSpeed() {
        return this.isInWater() ? 0.45D : 0.28D;
    }

    public void stopDash() {
        this.dashTicks = 0;
        this.entityData.set(DASHING, false);
    }

    @Nullable
    public LivingEntity getGrabbedPrey() {
        return this.getFirstPassenger() instanceof LivingEntity prey ? prey : null;
    }

    public boolean canGrab(LivingEntity target) {
        return !this.isBaby() && !this.isVehicle() && !this.isPassenger() && !target.isPassenger() && !target.isVehicle()
                && target.getBbWidth() <= this.getBbWidth();
    }

    public boolean grabPrey(LivingEntity target, BlockPos water) {
        if (!this.canGrab(target) || !target.startRiding(this, true, true))
            return false;

        this.dragTarget = water;
        this.stopDash();
        this.clearEnvironmentTarget();
        this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        this.getNavigation().stop();

        return true;
    }

    public void releasePrey() {
        this.ejectPassengers();
        this.dragTarget = null;
    }

    @Nullable
    public BlockPos getDragTarget() {
        return this.dragTarget;
    }

    public void setDragTarget(@Nullable BlockPos target) {
        this.dragTarget = target;
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    public boolean dismountsUnderwater() {
        return false;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0D, 4D / 16D, 23D / 16D).scale(this.getScale()).xRot(this.swimPitch * Mth.DEG_TO_RAD).yRot(-this.yBodyRot * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        var mouth = this.getPassengerRidingPosition(passenger);
        var pos = BlockPos.containing(mouth);
        var shape = this.level().getBlockState(pos).getCollisionShape(this.level(), pos);
        var height = shape.isEmpty() ? mouth.y : Math.max(mouth.y, pos.getY() + shape.max(Direction.Axis.Y));

        moveFunction.accept(passenger, mouth.x, height, mouth.z);
    }

    public void setEnvironmentTarget(@Nullable BlockPos pos) {
        this.environmentTarget = pos;
    }

    @Nullable
    public BlockPos getEnvironmentTarget() {
        return this.environmentTarget;
    }

    public boolean hasEnvironmentTarget() {
        return this.environmentTarget != null;
    }

    public void clearEnvironmentTarget() {
        this.environmentTarget = null;
    }

    public boolean isAtEnvironmentTarget() {
        if (this.environmentTarget == null)
            return false;

        return this.blockPosition().closerThan(this.environmentTarget, 2F);
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        var brain = (Brain<CrocodileEntity>) this.getBrain();

        var lastAttacker = this.getLastHurtByMob();

        var prey = this.getGrabbedPrey();

        if (prey != null && CrocodileAI.isValidTarget(this, prey))
            brain.setMemory(MemoryModuleType.ATTACK_TARGET, prey);
        else if (lastAttacker != null && CrocodileAI.isValidTarget(this, lastAttacker))
            brain.setMemory(MemoryModuleType.ATTACK_TARGET, lastAttacker);

        brain.getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
            if (!CrocodileAI.isValidTarget(this, target) || CrocodileAI.isHoldingFavoriteFood(target)) {
                brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                this.releasePrey();
            }
        });

        brain.tick(serverLevel, this);

        CrocodileAI.updateActivity(this);

        super.customServerAiStep(serverLevel);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.previousSwimPitch = this.swimPitch;

        if (!this.isAttacking()) {
            var movement = this.getDeltaMovement();
            var targetPitch = this.isInWater() && !this.isAmbushing()
                    ? Mth.clamp((float) Mth.atan2(movement.y, movement.horizontalDistance()) * Mth.RAD_TO_DEG, -35F, 35F)
                    * (float) Math.min(movement.length() / this.getAttributeValue(Attributes.MOVEMENT_SPEED), 1D)
                    : 0F;

            this.swimPitch = Mth.lerp(0.15F, this.swimPitch, targetPitch);
        }

        if (!this.level().isClientSide() && this.attackAnimationTicks > 0) {
            this.attackAnimationTicks--;

            if (this.attackAnimationTicks == 0 && this.isAttacking())
                this.setAttacking(false);
        }

        if (!this.level().isClientSide()) {
            if (this.dashCooldown > 0)
                --this.dashCooldown;

            if (this.dashTicks > 0 && (--this.dashTicks == 0 || this.horizontalCollision))
                this.stopDash();

            var prey = this.getGrabbedPrey();

            if (prey != null && (!this.isAlive() || !prey.isAlive()))
                this.releasePrey();
        }
    }

    public float getSwimPitch(float partialTick) {
        return Mth.lerp(partialTick, this.previousSwimPitch, this.swimPitch);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide())
            this.tickEggLaying();
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putBoolean("hasEgg", this.hasEgg());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.setHasEgg(tag.getBooleanOr("hasEgg", false));
    }
    protected Brain.Provider<CrocodileEntity> brainProvider() {
        return CrocodileAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return CrocodileAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.CROCODILE_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.CROCODILE_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.CROCODILE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.CROCODILE_DEATH;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ReAnimalEntities.CROCODILE.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal mate) {
        if (!(mate instanceof CrocodileEntity crocodileMate)) {
            super.spawnChildFromBreeding(level, mate);

            return;
        }

        if (this.hasEgg() || crocodileMate.hasEgg())
            return;

        this.setHasEgg(true);

        this.finalizeSpawnChildFromBreeding(level, mate, null);
    }

    @Override
    public boolean canMate(Animal other) {
        if (this.hasEgg())
            return false;

        if (other instanceof CrocodileEntity crocodile && crocodile.hasEgg())
            return false;

        return super.canMate(other);
    }

    @Override
    public void updateSwimming() {
        this.setSwimming(this.isEffectiveAi() && this.isInWater());
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isBasking()) {
            this.setDeltaMovement(0D, this.getDeltaMovement().y, 0D);
            super.travel(Vec3.ZERO);

            return;
        }

        if (this.isAmbushing() && this.isInWater()) {
            this.move(MoverType.SELF, this.getDeltaMovement());

            return;
        }

        if (this.isDashing()) {
            var movement = this.dashDirection.scale(this.getDashSpeed());

            this.setDeltaMovement(movement.x, this.isInWater() ? movement.y : this.getDeltaMovement().y, movement.z);
            travelVector = Vec3.ZERO;
        }

        if (this.isEffectiveAi() && this.isInWater() && this.isSwimming()) {
            var swimSpeed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.25F;

            this.moveRelative(swimSpeed, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));

            if (this.horizontalCollision)
                this.setDeltaMovement(this.getDeltaMovement().x, 0.3D, this.getDeltaMovement().z);
        } else
            super.travel(travelVector);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (this.isSpinning()) {
            var hurt = target.hurtServer(level, this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2F);

            if (hurt)
                this.setLastHurtMob(target);

            return hurt;
        }

        return super.doHurtTarget(level, target);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::mainPredicate));
        controllers.add(new AnimationController<>("pose", 5,
                state -> state.setAndContinue(this.isInWater() ? SWIM_POSE : LAND_POSE)).additiveAnimations());
        controllers.add(new AnimationController<>("mouth", 0, state -> {
            var controller = state.controller();

            if (this.getGrabbedPrey() != null) {
                controller.setTransitionTicks(3);

                return state.setAndContinue(HOLD);
            }

            controller.setTransitionTicks(0);

            if (controller.getCurrentRawAnimation() == BITE)
                return PlayState.CONTINUE;

            controller.reset();

            return PlayState.STOP;
        }).additiveAnimations().receiveTriggeredAnimations().triggerableAnim(BITE_TRIGGER, BITE));
        controllers.add(new AnimationController<>("spin", 0, state -> PlayState.STOP)
                .additiveAnimations()
                .triggerableAnim(SPIN_TRIGGER, SPIN));
    }

    private PlayState mainPredicate(AnimationTest<CrocodileEntity> state) {
        var controller = state.controller();
        var moving = state.isMoving() && !this.isSpinning();

        if (this.isBasking())
            controller.setAnimation(BASK);
        else if (this.isAmbushing())
            controller.setAnimation(AMBUSH);
        else if (this.isDashing())
            controller.setAnimation(this.isInWater() ? DASH_SWIM : DASH_LAND);
        else if (this.isInWater())
            controller.setAnimation(moving ? SWIM : SWIM_IDLE);
        else if (moving)
            controller.setAnimation(WALK);
        else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 16D)
                .add(Attributes.STEP_HEIGHT, 1.1D)
                .add(Attributes.ATTACK_DAMAGE, 10D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder buidler) {
        super.defineSynchedData(buidler);

        buidler.define(ATTACKING, false);
        buidler.define(HAS_EGG, false);
        buidler.define(AMBUSHING, false);
        buidler.define(BASKING, false);
        buidler.define(DASHING, false);
        buidler.define(SPINNING, false);
    }

    private void tickEggLaying() {
        if (this.isBaby())
            return;

        if (!this.hasEgg())
            return;

        var level = (ServerLevel) this.level();
        var pos = this.blockPosition();

        if (this.canLayEggAt(level, pos)) {
            this.layEgg(level, pos);
            this.setHasEgg(false);
        } else if (this.tickCount % 200 == 0 && !this.hasEnvironmentTarget()) {
            var target = this.findEggLayTarget(level);
            if (target != null)
                this.setEnvironmentTarget(target);
        }
    }

    private boolean canLayEggAt(ServerLevel level, BlockPos pos) {
        BlockPos groundPos = pos.below();
        BlockState groundState = level.getBlockState(groundPos);
        
        boolean isValidGround = groundState.is(BlockTags.SAND) || groundState.is(Blocks.MUD);
        boolean isSpaceClear = level.getBlockState(pos).isAir() && level.getFluidState(pos).isEmpty();
        
        return isValidGround && isSpaceClear;
    }

    @Nullable
    private BlockPos findEggLayTarget(ServerLevel level) {
        var origin = this.blockPosition();
        var random = this.getRandom();

        for (int i = 0; i < 40; i++) {
            var dx = random.nextInt(17) - 8;
            var dz = random.nextInt(17) - 8;

            var x = origin.getX() + dx;
            var z = origin.getZ() + dz;
            
            var topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            if (topY <= level.getMinY())
                continue;

            var eggPos = new BlockPos(x, topY, z);
            var ground = eggPos.below();
            BlockState stateBelow = level.getBlockState(ground);

            if (!stateBelow.is(BlockTags.SAND) && !stateBelow.is(Blocks.MUD))
                continue;

            if (!level.getBlockState(eggPos).isAir() || !level.getFluidState(eggPos).isEmpty())
                continue;

            return eggPos;
        }

        return null;
    }

    private void layEgg(ServerLevel level, BlockPos pos) {
        var eggs = Mth.nextInt(this.random, CrocodileEggBlock.MIN_EGGS, CrocodileEggBlock.MAX_EGGS);
        var state = ReAnimalBlocks.CROCODILE_EGG.defaultBlockState().setValue(CrocodileEggBlock.EGGS, eggs);

        level.setBlock(pos, state, 3);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(this, state));
        this.playSound(SoundEvents.TURTLE_LAY_EGG, 1F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1F);
    }
}
