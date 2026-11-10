package it.hurts.shatterbyte.reanimal.common.entity.seal;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.keyframehandler.AutoPlayingSoundKeyframeHandler;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.util.GeckoLibUtil;
import it.hurts.shatterbyte.reanimal.common.entity.BodyBounds;
import it.hurts.shatterbyte.reanimal.common.entity.ball.BallEntity;
import it.hurts.shatterbyte.reanimal.common.entity.ball.BallPlay;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SealEntity extends TamableAnimal implements GeoEntity {
    public static final DataTicket<Boolean> SLAP_PARTICLE = DataTicket.create("reanimal_seal_slap_particle", Boolean.class);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.seal.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.seal.walk");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.seal.swim");
    private static final RawAnimation SWIM_IDLE = RawAnimation.begin().thenLoop("animation.seal.swim_idle");
    private static final RawAnimation LAYING = RawAnimation.begin().then("animation.seal.laying", LoopType.PLAY_ONCE);
    private static final RawAnimation LAY = RawAnimation.begin().thenLoop("animation.seal.lay");
    private static final RawAnimation LAY_SLAP = RawAnimation.begin().thenLoop("animation.seal.lay_slap");
    private static final RawAnimation GETTING_UP = RawAnimation.begin().then("animation.seal.getting_up", LoopType.PLAY_ONCE);

    private static final EntityDataAccessor<Integer> LAY_STATE = SynchedEntityData.defineId(SealEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SWIM_ROLL = SynchedEntityData.defineId(SealEntity.class, EntityDataSerializers.FLOAT);

    private static final int LAY_TRANSITION_TICKS = 14;
    private static final int SWIM_ROLL_DURATION = 40;
    private static final int SWIM_BACKSTROKE_DURATION = 100;
    private static final int SWIM_BACKSTROKE_TRANSITION = 20;

    private static final UniformInt SWIM_ROLL_COOLDOWN = UniformInt.of(160, 320);

    public static final UniformInt LAY_DURATION = UniformInt.of(400, 800);
    private static final UniformInt LAY_COOLDOWN = UniformInt.of(200, 400);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public int layTime;
    public int layCooldown;

    @Nullable
    private BlockPos environmentTarget;

    private final BallPlay ballPlay = new BallPlay(this);

    private float swimPitch;
    private float previousSwimPitch;
    private float swimRoll;
    private float previousSwimRoll;
    private int swimRollType;
    private int swimRollTime;
    private int swimRollCooldown;
    private int dismountDelay;

    public SealEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);

        this.moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, 0.4F, 1F, false) {
            private float movementPitch;

            @Override
            public void tick() {
                if (this.mob.isInWater())
                    this.mob.setXRot(this.movementPitch);

                super.tick();

                this.movementPitch = this.mob.isInWater() ? this.mob.getXRot() : 0F;
            }
        };
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.navigation = new AmphibiousPathNavigation(this, level);

        this.setPathfindingMalus(PathType.WATER, 0F);
        this.swimRollCooldown = SWIM_ROLL_COOLDOWN.sample(this.random);
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
    protected AABB makeBoundingBox(Vec3 position) {
        var bounds = super.makeBoundingBox(position);

        return BodyBounds.fitAlongYaw(this, bounds, 1.8D);
    }

    @Override
    public void setYRot(float yaw) {
        var previous = this.getYRot();

        super.setYRot(yaw);

        if (this.getYRot() != previous)
            this.setBoundingBox(this.makeBoundingBox());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);

        if (LAY_STATE.equals(accessor))
            this.refreshDimensions();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(1F, this.getLayState() == LayState.LAYING ? 1.33F : 1F);
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        this.ballPlay.tick();

        if (this.tickCount % 20 == 0) {
            this.checkForThreats();

            if (!this.isPlayingWithBall() && this.isTame() && !this.isInLove())
                this.ballPlay.findBall(ball -> true);
        }

        SealAI.updateActivity(this);

        ((Brain<SealEntity>) this.getBrain()).tick(serverLevel, this);

        super.customServerAiStep(serverLevel);
    }

    public BallPlay getBallPlay() {
        return this.ballPlay;
    }

    @Nullable
    public BallEntity getPlayBall() {
        return this.ballPlay.getBall();
    }

    public void startPlayingWithBall(BallEntity ball) {
        this.ballPlay.start(ball);
    }

    public void stopPlayingWithBall() {
        this.ballPlay.stop();
    }

    public boolean isPlayingWithBall() {
        return this.isTame() && !this.isInLove() && this.ballPlay.isPlaying();
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide()) {
            this.previousSwimPitch = this.swimPitch;

            var movement = this.getDeltaMovement();
            var maxPitch = this.isEyeInFluid(FluidTags.WATER) ? 35F : 12F;
            var targetPitch = this.isInWater()
                    ? Mth.clamp((float) Mth.atan2(movement.y, movement.horizontalDistance()) * Mth.RAD_TO_DEG, -maxPitch, maxPitch)
                    * (float) Math.min(movement.length() / this.getAttributeValue(Attributes.MOVEMENT_SPEED), 1D)
                    : 0F;

            this.swimPitch = Mth.lerp(0.15F, this.swimPitch, targetPitch);
        }

        if (this.isLaying()) {
            this.setXRot(0);
            this.xRotO = 0;

            this.yHeadRot = this.yBodyRot;
            this.yHeadRotO = this.yBodyRotO;
        }

        if (!this.level().isClientSide() && this.isLaying())
            this.getNavigation().stop();
    }

    public float getSwimPitch(float partialTick) {
        return Mth.lerp(partialTick, this.previousSwimPitch, this.swimPitch);
    }

    public float getSwimRoll(float partialTick) {
        return Mth.rotLerp(partialTick, this.previousSwimRoll, this.swimRoll);
    }

    private void updateSwimRoll() {
        if (this.swimRollCooldown > 0)
            this.swimRollCooldown--;

        if (!this.isAlive() || !this.isInWater() || this.isPassenger() || this.isVehicle() || this.hurtTime > 0
                || this.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING)) {
            if (this.swimRollType != 0)
                this.swimRollCooldown = SWIM_ROLL_COOLDOWN.sample(this.random);

            this.swimRollType = 0;
            this.entityData.set(SWIM_ROLL, 0F);

            return;
        }

        if (this.swimRollType == 0) {
            if (this.swimRollCooldown > 0 || !this.isEyeInFluid(FluidTags.WATER) || this.getNavigation().isDone()
                    || !this.level().getFluidState(this.blockPosition().below()).is(FluidTags.WATER))
                return;

            this.swimRollType = (this.random.nextInt(2) + 1) * (this.random.nextBoolean() ? 1 : -1);
            this.swimRollTime = 0;
        }

        this.swimRollTime++;

        var backstroke = Math.abs(this.swimRollType) == 2;
        var duration = backstroke ? SWIM_BACKSTROKE_DURATION : SWIM_ROLL_DURATION;
        var roll = backstroke
                ? 180F * (Mth.smoothstep(Mth.clamp((float) this.swimRollTime / SWIM_BACKSTROKE_TRANSITION, 0F, 1F))
                + Mth.smoothstep(Mth.clamp((float) (this.swimRollTime - duration + SWIM_BACKSTROKE_TRANSITION) / SWIM_BACKSTROKE_TRANSITION, 0F, 1F)))
                : 360F * Mth.smoothstep((float) this.swimRollTime / duration);

        if (this.swimRollTime >= duration) {
            this.swimRollType = 0;
            this.swimRollCooldown = SWIM_ROLL_COOLDOWN.sample(this.random);
            this.entityData.set(SWIM_ROLL, 0F);
        } else
            this.entityData.set(SWIM_ROLL, (float) roll * Math.signum(this.swimRollType));
    }

    protected Brain.Provider<SealEntity> brainProvider() {
        return SealAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return SealAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    public boolean canRideAdult(SealEntity adult) {
        return this.isBaby() && this.isInWater() && !this.isPassenger() && !this.isVehicle()
                && this.boardingCooldown == 0 && this.hurtTime == 0 && !this.isLaying() && this.swimRollType == 0
                && !this.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING)
                && adult.isInWater() && adult.canCarryBaby() && !adult.isVehicle();
    }

    private boolean canCarryBaby() {
        return !this.isBaby() && this.isAlive() && !this.isPassenger()
                && this.hurtTime == 0 && !this.isLaying() && this.swimRollType == 0
                && !this.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
    }

    @Override
    public boolean canControlVehicle() {
        return false;
    }

    @Override
    protected boolean isImmobile() {
        return this.getVehicle() instanceof SealEntity || super.isImmobile();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return passenger instanceof SealEntity ? new Vec3(0D, 13D / 16D, 0D)
                : super.getPassengerAttachmentPoint(passenger, dimensions, scale);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            this.previousSwimRoll = this.swimRoll;

            var targetRoll = this.isInWater() && !this.isPassenger()
                    ? this.entityData.get(SWIM_ROLL) - Mth.clamp(Mth.wrapDegrees(this.yBodyRot - this.yBodyRotO) * 3F, -20F, 20F)
                    : 0F;

            this.swimRoll = Mth.wrapDegrees(Mth.rotLerp(0.2F, this.swimRoll, targetRoll));
        } else
            this.updateSwimRoll();

        if (!this.level().isClientSide() && this.getVehicle() instanceof SealEntity adult) {
            if (!this.isBaby() || this.hurtTime > 0 || this.isLaying()
                    || this.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING) || !adult.canCarryBaby())
                this.stopRiding();
            else if (adult.isInWater())
                this.dismountDelay = 0;
            else if (this.dismountDelay == 0)
                this.dismountDelay = this.random.nextIntBetweenInclusive(20, 100);
            else if (--this.dismountDelay == 0)
                this.stopRiding();
        } else
            this.dismountDelay = 0;
    }

    @Override
    public void rideTick() {
        super.rideTick();

        if (this.getVehicle() instanceof SealEntity adult) {
            this.setYRot(adult.yBodyRot);
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            this.setXRot(0F);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.SEAL_FOOD);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        var level = this.level();
        var held = player.getItemInHand(hand);

        if (this.isFood(held)) {
            if (!this.isTame()) {
                if (!level.isClientSide()) {
                    if (!player.getAbilities().instabuild)
                        held.shrink(1);

                    if (this.random.nextInt(3) == 0) {
                        this.tame(player);
                        level.broadcastEntityEvent(this, (byte) 7);
                    } else
                        level.broadcastEntityEvent(this, (byte) 6);
                }

                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            }

            if (this.isOwnedBy(player) && this.getHealth() < this.getMaxHealth()) {
                if (!level.isClientSide()) {
                    if (!player.getAbilities().instabuild)
                        held.shrink(1);

                    this.heal(2F);
                }

                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            }
        }

        if (held.is(ReAnimalItems.BALL) && this.isTame()) {
            if (!level.isClientSide())
                this.ballPlay.giveBall(player, hand);

            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.SEAL_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.SEAL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.SEAL_DEATH;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.SEAL.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result) {
            this.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);

            SealAI.updateActivity(this);
        }

        return result;
    }

    private void checkForThreats() {
        if (this.level().isClientSide()) return;

        var polarBear = this.level().getEntitiesOfClass(PolarBear.class, this.getBoundingBox().inflate(12.0D, 6.0D, 12.0D)).stream().findFirst().orElse(null);

        if (polarBear != null) {
            this.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);

            if (this.isLaying() && this.getLayState() != LayState.GETTING_UP) {
                this.startGettingUp();
            }
        }
    }

    @Override
    public void updateSwimming() {
        this.setSwimming(this.isEffectiveAi() && this.isInWater());
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater() && this.isSwimming()) {
            var swimSpeed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.25F;
            var target = this.getNavigation().getTargetPos();
            var leavingWater = this.getNavigation().isInProgress() && target != null
                    && target.getY() > this.getY() && !this.level().getFluidState(target).is(FluidTags.WATER);

            this.moveRelative(swimSpeed, travelVector);

            if (this.isPlayingWithBall() && !leavingWater)
                this.setDeltaMovement(this.getDeltaMovement().x,
                        Math.min(this.getDeltaMovement().y, Math.max(0D, this.getFluidHeight(FluidTags.WATER) - this.getBbHeight() * 0.5D)),
                        this.getDeltaMovement().z);

            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));

            if (this.horizontalCollision && leavingWater)
                this.setDeltaMovement(this.getDeltaMovement().x, 0.3D, this.getDeltaMovement().z);
        } else
            super.travel(travelVector);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::mainPredicate)
                .setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>())
                .setParticleKeyframeHandler(event -> event.renderState().addGeckolibData(SLAP_PARTICLE, true)));
    }

    private PlayState mainPredicate(AnimationTest<SealEntity> state) {
        var controller = state.controller();
        var entity = state.animatable();

        if (this.getVehicle() instanceof SealEntity) {
            controller.setAnimation(IDLE);

            return PlayState.CONTINUE;
        }

        switch (this.getLayState()) {
            case LAYING_DOWN -> controller.setAnimation(LAYING);
            case LAYING -> {
                if (entity.level().getNearestPlayer(this, 3) != null)
                    controller.setAnimation(LAY_SLAP);
                else
                    controller.setAnimation(LAY);
            }
            case GETTING_UP -> controller.setAnimation(GETTING_UP);
            case STANDING -> {
                if (this.isInWater())
                    controller.setAnimation(state.isMoving() ? SWIM : SWIM_IDLE);
                else
                    controller.setAnimation(state.isMoving() ? WALK : IDLE);
            }
        }

        return PlayState.CONTINUE;
    }

    public static boolean checkSealSpawnRules(EntityType<SealEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        var below = pos.below();

        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.getFluidState(pos).isEmpty();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 16D)
                .add(Attributes.STEP_HEIGHT, 1.1D)
                .add(Attributes.ATTACK_DAMAGE, 4D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder buidler) {
        super.defineSynchedData(buidler);

        buidler.define(LAY_STATE, LayState.STANDING.ordinal());
        buidler.define(SWIM_ROLL, 0F);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("LayState", this.entityData.get(LAY_STATE));
        tag.putInt("LayTime", this.layTime);
        tag.putInt("LayCooldown", this.layCooldown);
        this.ballPlay.save(tag);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.setLayState(LayState.byId(tag.getIntOr("LayState", 0)));

        this.layTime = tag.getIntOr("LayTime", 0);

        this.layCooldown = tag.getIntOr("LayCooldown", 0);
        this.ballPlay.load(tag);
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                if (SealEntity.this.isLaying())
                    return;

                super.clientTick();
            }
        };
    }

    public boolean isLaying() {
        var state = this.getLayState();

        return state == LayState.LAYING_DOWN || state == LayState.LAYING || state == LayState.GETTING_UP;
    }

    public LayState getLayState() {
        return LayState.byId(this.entityData.get(LAY_STATE));
    }

    public void setLayState(LayState state) {
        this.entityData.set(LAY_STATE, state.ordinal());
    }

    public boolean canStartLaying() {
        return !this.isVehicle()
                && !this.isInWater()
                && !this.getNavigation().isInProgress()
                && this.getDeltaMovement().horizontalDistanceSqr() < 0.0001D
                && !this.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING)
                && !this.getBrain().hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER)
                && this.random.nextInt(600) == 0;
    }

    public void startLayingDown() {
        this.getNavigation().stop();

        this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        this.setLayState(LayState.LAYING_DOWN);

        this.layTime = LAY_TRANSITION_TICKS;
    }

    public void startGettingUp() {
        this.setLayState(LayState.GETTING_UP);

        this.layTime = LAY_TRANSITION_TICKS;
    }

    public void finishGettingUp() {
        this.setLayState(LayState.STANDING);

        this.layCooldown = LAY_COOLDOWN.sample(this.random);
    }

    public enum LayState {
        STANDING,
        LAYING_DOWN,
        LAYING,
        GETTING_UP;

        public static LayState byId(int id) {
            var values = LayState.values();

            return id >= 0 && id < values.length ? values[id] : STANDING;
        }
    }
}
