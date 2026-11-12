package it.hurts.shatterbyte.reanimal.common.entity.penguin;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

public class PenguinEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.penguin.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.penguin.walk");
    private static final RawAnimation SLIDE = RawAnimation.begin().thenLoop("animation.penguin.slide");
    private static final RawAnimation DIVE = RawAnimation.begin().thenLoop("animation.penguin.dive");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.penguin.swim");
    private static final RawAnimation SWIM_IDLE = RawAnimation.begin().thenLoop("animation.penguin.swim_idle");
    private static final RawAnimation BABY_IDLE = RawAnimation.begin().thenLoop("animation.penguin_baby.idle");
    private static final RawAnimation BABY_WALK = RawAnimation.begin().thenLoop("animation.penguin_baby.walk");
    private static final RawAnimation BABY_SLIDE = RawAnimation.begin().thenLoop("animation.penguin_baby.slide");
    private static final EntityDataAccessor<Boolean> SLIDING = SynchedEntityData.defineId(PenguinEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DIVING = SynchedEntityData.defineId(PenguinEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GLIDING = SynchedEntityData.defineId(PenguinEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.25F, 0.5F);
    private static final EntityDimensions BABY_SLIDING_DIMENSIONS = EntityDimensions.scalable(0.25F, 0.35F);
    private static final double DIVE_SPEED = 0.35D;
    private static final int SLIDE_COOLDOWN_TICKS = 100;
    private static final double SLIDE_SPEED = 0.28D;
    private static final double SLIDE_DECELERATION = 0.02D;
    private static final double MIN_SLIDE_DISTANCE = 3D;
    private static final UniformInt LAND_REST_DURATION = UniformInt.of(1200, 2400);
    private static final UniformInt SWIM_DURATION = UniformInt.of(600, 1200);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final MoveControl<?> landMoveControl;
    private final SmoothSwimmingMoveControl<PenguinEntity> swimMoveControl;

    private int eggTime = this.random.nextInt(6000) + 6000;
    private int landRestTicks = LAND_REST_DURATION.sample(this.random);
    private int swimTicks = SWIM_DURATION.sample(this.random);
    private boolean wasInWater;
    private Vec3 slideTarget;
    private Vec3 slideDirection;
    private double slideStopDistance;
    private double slideSpeed;
    private int nextSlideTick;
    private boolean clientDiving;
    private boolean swimmingDimensions;
    private float bodyPitch;
    private float previousBodyPitch;

    public PenguinEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.landMoveControl = this.moveControl;
        this.swimMoveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, 0.4F, 1F, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.navigation = new AmphibiousPathNavigation(this, level) {
            @Override
            public boolean canFloat() {
                return PenguinEntity.this.isBaby() || super.canFloat();
            }
        };

        this.setPathfindingMalus(PathType.WATER, 0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(SLIDING, false);
        builder.define(DIVING, false);
        builder.define(GLIDING, false);
    }

    public boolean isSliding() {
        return this.getEntityData().get(SLIDING);
    }

    public boolean isDiving() {
        return this.getEntityData().get(DIVING) || this.clientDiving;
    }

    public boolean isGliding() {
        return this.getEntityData().get(GLIDING);
    }

    public boolean wantsToSwim() {
        return !this.isBaby() && this.landRestTicks == 0 && this.swimTicks > 0;
    }

    public float getBodyPitch(float partialTick) {
        return Mth.lerp(partialTick, this.previousBodyPitch, this.bodyPitch);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);

        if (SLIDING.equals(accessor) || DIVING.equals(accessor))
            this.refreshDimensions();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        if (this.isSliding() || this.isDiving())
            return this.getSlidingDimensions(pose);

        if (this.isBaby())
            return BABY_DIMENSIONS;

        var dimensions = super.getDefaultDimensions(pose);
        return this.swimmingDimensions ? dimensions.scale(1F, 0.625F) : dimensions;
    }

    private EntityDimensions getSlidingDimensions(Pose pose) {
        return this.isBaby() ? BABY_SLIDING_DIMENSIONS : super.getDefaultDimensions(pose).scale(1F, 0.5F);
    }

    @Override
    public void aiStep() {
        var swimming = !this.isBaby() && this.isInWater();
        if (this.swimmingDimensions != swimming) {
            this.swimmingDimensions = swimming;
            this.moveControl = this.swimmingDimensions ? this.swimMoveControl : this.landMoveControl;
            this.moveControl.setWait();
            this.setYya(0F);
            this.refreshDimensions();
        }

        super.aiStep();

        this.tickEggLaying();

        if (this.level().isClientSide()) {
            var diving = this.getEntityData().get(DIVING) || this.clientDiving && !this.isInWater() && !this.onGround();

            if (this.clientDiving != diving) {
                this.clientDiving = diving;
                this.refreshDimensions();
            }

            this.previousBodyPitch = this.bodyPitch;

            var movement = this.position().subtract(this.xo, this.yo, this.zo);
            var targetPitch = this.isDiving() || this.swimmingDimensions
                    ? (float) -Mth.atan2(movement.y, movement.horizontalDistance()) * Mth.RAD_TO_DEG
                    : 0F;

            if (this.swimmingDimensions && this.isGliding() && !this.isDiving())
                targetPitch *= (float) Math.min(movement.length() / this.getAttributeValue(Attributes.MOVEMENT_SPEED), 1D);

            this.bodyPitch = this.isDiving() ? targetPitch : Mth.lerp(0.35F, this.bodyPitch, targetPitch);
        } else if (this.isDiving()) {
            this.syncPosition = true;

            if (this.isInWater() || this.onGround() || this.isPassenger())
                this.getEntityData().set(DIVING, false);
            else {
                var movement = this.getDeltaMovement();

                this.setYRot((float) Mth.atan2(-movement.x, movement.z) * Mth.RAD_TO_DEG);
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();
                this.setXRot((float) -Mth.atan2(movement.y, movement.horizontalDistance()) * Mth.RAD_TO_DEG);
            }
        }

        if (this.isSliding()) {
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            this.setXRot(0F);

            if (this.level().isClientSide())
                this.spawnSlideParticles();
        }
    }

    private void spawnSlideParticles() {
        var movement = new Vec3(this.getX() - this.xo, 0D, this.getZ() - this.zo);
        var speed = movement.length();

        if (speed == 0D)
            return;

        var direction = movement.scale(1D / speed);
        var width = this.getBbWidth();
        var front = this.position().add(direction.scale(width * 0.8D));
        var pos = BlockPos.containing(front);
        var state = this.level().getBlockState(pos);

        if (!state.is(BlockTags.SNOW))
            state = this.level().getBlockState(pos.below());

        var particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
        var strength = Math.min(speed / SLIDE_SPEED, 1D);

        for (var side = -1; side <= 1; side += 2) {
            for (var i = 0; i < 2; i++) {
                if (this.random.nextDouble() >= strength)
                    continue;

                var spread = side * width * (0.2D + this.random.nextDouble() * 0.25D);
                var outward = side * (1D + this.random.nextDouble() * 0.5D);

                this.level().addParticle(particle, front.x - direction.z * spread, front.y + width * 0.1D, front.z + direction.x * spread,
                        -direction.x * 0.35D - direction.z * outward, 0.5D, -direction.z * 0.35D + direction.x * outward);
            }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        if (this.isInWater()) {
            if (this.swimTicks > 0)
                --this.swimTicks;
        } else {
            if (this.wasInWater) {
                this.landRestTicks = LAND_REST_DURATION.sample(this.random);
                this.swimTicks = SWIM_DURATION.sample(this.random);
            }

            if (this.landRestTicks > 0)
                --this.landRestTicks;
        }

        this.wasInWater = this.isInWater();
        this.setPathfindingMalus(PathType.WATER, this.isInWater() || this.wantsToSwim() ? 0F : PathType.WATER.getMalus());

        if (this.isSliding())
            this.tickSliding();
        else if (!this.isDiving()) {
            ((Brain<PenguinEntity>) this.getBrain()).tick(serverLevel, this);

            PenguinAI.updateActivity(this);
            this.tryJumpIntoWater();

            if (!this.isDiving())
                this.tryStartSliding();
        }

        super.customServerAiStep(serverLevel);
    }

    private void tryJumpIntoWater() {
        if (this.isBaby() || !this.onGround() || this.isInWater() || !this.getNavigation().isInProgress())
            return;

        var nextNode = this.getNavigation().getPath().getNextNodePos();

        if (!this.level().getFluidState(nextNode).is(FluidTags.WATER))
            return;

        var direction = Vec3.atBottomCenterOf(nextNode).subtract(this.position()).multiply(1D, 0D, 1D).normalize();

        this.getEntityData().set(DIVING, true);
        this.getNavigation().stop();
        this.moveControl.setWait();
        this.getBrain().eraseMemory(MemoryModuleType.PATH);
        this.setJumping(false);
        this.jumpFromGround();
        this.setDeltaMovement(direction.x * DIVE_SPEED, this.getDeltaMovement().y, direction.z * DIVE_SPEED);
    }

    private void tryStartSliding() {
        if (this.tickCount < this.nextSlideTick || !this.onGround() || this.isInWater() || this.isPassenger() || this.hurtTime > 0)
            return;

        var target = this.getBrain().getMemory(MemoryModuleType.WALK_TARGET).orElse(null);

        if (target == null || target.getTarget().currentBlockPosition().getY() != this.getBlockY())
            return;

        var offset = target.getTarget().currentPosition().subtract(this.position()).multiply(1D, 0D, 1D);
        var distance = offset.length();
        var stopDistance = Math.max(target.getCloseEnoughDist(), this.getBbWidth() * 0.5D);

        if (distance - stopDistance < MIN_SLIDE_DISTANCE)
            return;

        var direction = offset.scale(1D / distance);
        var yaw = (float) Mth.atan2(-direction.x, direction.z) * Mth.RAD_TO_DEG;
        var slideBounds = this.getSlidingDimensions(this.getPose()).scale(this.getScale()).makeBoundingBox(this.position());
        var route = direction.scale(distance - stopDistance);

        if (!this.level().noCollision(this, this.getBoundingBox().move(route)))
            return;
        var steps = Mth.ceil(route.length() / (this.getBbWidth() * 0.5D));

        for (var step = 0; step <= steps; step++) {
            var movement = route.scale((double) step / steps);

            if (!this.isSlideSurface(this.position().add(movement)) || !this.level().noCollision(this, slideBounds.move(movement)))
                return;
        }

        this.slideTarget = this.position().add(offset);
        this.slideDirection = direction;
        this.slideStopDistance = stopDistance;
        this.slideSpeed = SLIDE_SPEED;
        this.getNavigation().stop();
        this.moveControl.setWait();
        this.getBrain().eraseMemory(MemoryModuleType.PATH);
        this.setJumping(false);
        this.setDeltaMovement(0D, this.getDeltaMovement().y, 0D);
        this.setYRot(yaw);
        this.getEntityData().set(SLIDING, true);
    }

    private void tickSliding() {
        var remaining = this.slideTarget.subtract(this.position()).dot(this.slideDirection) - this.slideStopDistance;
        var movement = this.slideDirection.scale(Math.min(SLIDE_SPEED, remaining));

        if (remaining <= 0D || !this.onGround() || this.isInWater() || this.isPassenger() || this.hurtTime > 0 || this.horizontalCollision
                || !this.isSlideSurface(this.position()) || !this.isSlideSurface(this.position().add(movement))
                || !this.level().noCollision(this, this.getBoundingBox().expandTowards(movement)))
            this.stopSliding();
    }

    private boolean isSlideSurface(Vec3 position) {
        var pos = BlockPos.containing(position);
        var state = this.level().getBlockState(pos);

        if (state.is(BlockTags.SNOW))
            return !state.is(Blocks.POWDER_SNOW);

        state = this.level().getBlockState(pos.below());

        return (state.is(BlockTags.ICE) || state.is(BlockTags.SNOW)) && !state.is(Blocks.POWDER_SNOW);
    }

    private void stopSliding() {
        this.getEntityData().set(SLIDING, false);
        this.nextSlideTick = this.tickCount + SLIDE_COOLDOWN_TICKS;

        if (this.hurtTime == 0)
            this.setDeltaMovement(0D, this.getDeltaMovement().y, 0D);
    }

    @Override
    public boolean shouldDiscardFriction() {
        return this.isDiving() || super.shouldDiscardFriction();
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isDiving() && !this.isInWater())
            super.travel(Vec3.ZERO);
        else if (this.isEffectiveAi() && !this.isBaby() && this.isInWater()) {
            var target = this.getNavigation().getTargetPos();
            var leavingWater = this.getNavigation().isInProgress() && target != null
                    && target.getY() > this.getY() && !this.level().getFluidState(target).is(FluidTags.WATER);
            var gliding = input.lengthSqr() == 0D;

            this.getEntityData().set(GLIDING, gliding);

            this.moveRelative((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.375F, input);

            if (gliding) {
                var buoyancy = (Math.min(this.getFluidHeight(FluidTags.WATER), this.getBbWidth()) - this.getBbWidth() * 0.5D) * 0.02D;

                this.setDeltaMovement(this.getDeltaMovement().add(0D, buoyancy, 0D));
            }

            this.move(MoverType.SELF, this.getDeltaMovement());

            var drag = gliding && this.getFluidHeight(FluidTags.WATER) > this.getBbWidth() ? 0.98D : 0.9D;

            this.setDeltaMovement(this.getDeltaMovement().multiply(drag, 0.9D, drag));

            if (this.horizontalCollision && leavingWater)
                this.setDeltaMovement(this.getDeltaMovement().x, 0.3D, this.getDeltaMovement().z);
        } else if (this.isSliding()) {
            var remaining = this.slideTarget.subtract(this.position()).dot(this.slideDirection) - this.slideStopDistance;

            var targetSpeed = Math.min(SLIDE_SPEED, Math.sqrt(2D * SLIDE_DECELERATION * remaining));
            this.slideSpeed = Math.max(targetSpeed, this.slideSpeed - SLIDE_DECELERATION);
            var speed = Math.min(this.slideSpeed, remaining);

            this.setYRot((float) Mth.atan2(-this.slideDirection.x, this.slideDirection.z) * Mth.RAD_TO_DEG);
            this.setDeltaMovement(this.slideDirection.x * speed, this.getDeltaMovement().y, this.slideDirection.z * speed);
            super.travel(Vec3.ZERO);

            if (remaining <= speed)
                this.stopSliding();
        } else
            super.travel(input);
    }

    @Override
    public void updateSwimming() {
        this.setSwimming(this.isEffectiveAi() && !this.isBaby() && this.isInWater());
    }

    @Override
    public void calculateEntityAnimation(boolean useY) {
        super.calculateEntityAnimation(useY || this.swimmingDimensions);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    protected Brain.Provider<PenguinEntity> brainProvider() {
        return PenguinAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return PenguinAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.PENGUIN_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isBaby() ? ReAnimalSoundEvents.PENGUIN_IDLE_BABY : ReAnimalSoundEvents.PENGUIN_IDLE_ADULT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isBaby() ? ReAnimalSoundEvents.PENGUIN_HURT_BABY : ReAnimalSoundEvents.PENGUIN_HURT_ADULT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isBaby() ? ReAnimalSoundEvents.PENGUIN_DEATH_BABY : ReAnimalSoundEvents.PENGUIN_DEATH_ADULT;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.PENGUIN.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("eggTime", this.eggTime);
        tag.putInt("landRestTicks", this.landRestTicks);
        tag.putInt("swimTicks", this.swimTicks);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.eggTime = tag.getIntOr("eggTime", this.random.nextInt(6000) + 6000);
        this.landRestTicks = tag.getIntOr("landRestTicks", this.landRestTicks);
        this.swimTicks = tag.getIntOr("swimTicks", this.swimTicks);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result) {
            if (this.isSliding())
                this.stopSliding();

            this.getEntityData().set(DIVING, false);
            this.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);

            PenguinAI.updateActivity(this);
        }

        return result;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationTest<PenguinEntity> state) {
        var controller = state.controller();
        var baby = this.isBaby();

        if (!baby && this.isDiving())
            controller.setAnimation(DIVE);
        else if (!baby && this.isInWater())
            controller.setAnimation(this.isGliding() ? SWIM_IDLE : SWIM);
        else if (this.isSliding())
            controller.setAnimation(baby ? BABY_SLIDE : SLIDE);
        else if (state.isMoving())
            controller.setAnimation(baby ? BABY_WALK : WALK);
        else
            controller.setAnimation(baby ? BABY_IDLE : IDLE);

        return PlayState.CONTINUE;
    }

    private void tickEggLaying() {
        if (this.level().isClientSide() || this.isBaby() || this.isInWater() || !this.onGround())
            return;

        if (--this.eggTime > 0)
            return;

        this.playSound(SoundEvents.CHICKEN_EGG, 1F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1F);
        this.spawnAtLocation((ServerLevel) this.level(), ReAnimalItems.PENGUIN_EGG);
        this.gameEvent(GameEvent.ENTITY_PLACE, this);

        this.eggTime = this.random.nextInt(6000) + 6000;
    }

    public static boolean checkPenguinSpawnRules(EntityType<PenguinEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        var ground = level.getBlockState(pos.below());
        var brightEnoughToSpawn = EntitySpawnReason.ignoresLightRequirements(reason) || isBrightEnoughToSpawn(level, pos);

        return brightEnoughToSpawn && (ground.is(BlockTags.ANIMALS_SPAWNABLE_ON) || ground.is(Blocks.SAND) || ground.is(Blocks.SNOW_BLOCK) || ground.is(Blocks.PACKED_ICE));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 8D)
                .add(Attributes.MOVEMENT_SPEED, 0.175D)
                .add(Attributes.FOLLOW_RANGE, 16D)
                .add(Attributes.ATTACK_DAMAGE, 4D);
    }
}
