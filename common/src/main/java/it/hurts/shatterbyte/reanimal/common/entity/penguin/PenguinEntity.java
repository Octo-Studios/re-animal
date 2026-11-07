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
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
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
    private static final EntityDataAccessor<Boolean> SLIDING = SynchedEntityData.defineId(PenguinEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int SLIDE_COOLDOWN_TICKS = 100;
    private static final double SLIDE_SPEED = 0.28D;
    private static final double SLIDE_DECELERATION = 0.02D;
    private static final double MIN_SLIDE_DISTANCE = 3D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int eggTime = this.random.nextInt(6000) + 6000;
    private Vec3 slideTarget;
    private Vec3 slideDirection;
    private double slideStopDistance;
    private double slideSpeed;
    private int nextSlideTick;

    public PenguinEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.getNavigation().setCanFloat(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(SLIDING, false);
    }

    public boolean isSliding() {
        return this.getEntityData().get(SLIDING);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);

        if (SLIDING.equals(accessor))
            this.refreshDimensions();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        var dimensions = super.getDefaultDimensions(pose);

        return this.isSliding() ? dimensions.scale(1F, 0.5F) : dimensions;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.tickEggLaying();

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
        if (this.isSliding())
            this.tickSliding();
        else {
            ((Brain<PenguinEntity>) this.getBrain()).tick(serverLevel, this);

            PenguinAI.updateActivity(this);
            this.tryStartSliding();
        }

        super.customServerAiStep(serverLevel);
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

        var route = offset.scale((distance - stopDistance) / distance);
        var steps = Mth.ceil(route.length() / (this.getBbWidth() * 0.5D));

        for (var step = 0; step <= steps; step++) {
            var movement = route.scale((double) step / steps);

            if (!this.isSlideSurface(this.position().add(movement)) || !this.level().noCollision(this, this.getBoundingBox().move(movement)))
                return;
        }

        this.slideTarget = this.position().add(offset);
        this.slideDirection = offset.scale(1D / distance);
        this.slideStopDistance = stopDistance;
        this.slideSpeed = SLIDE_SPEED;
        this.getNavigation().stop();
        this.getBrain().eraseMemory(MemoryModuleType.PATH);
        this.setJumping(false);
        this.setDeltaMovement(0D, this.getDeltaMovement().y, 0D);
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
    public void travel(Vec3 input) {
        if (this.isSliding()) {
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
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.eggTime = tag.getIntOr("eggTime", this.random.nextInt(6000) + 6000);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result) {
            if (this.isSliding())
                this.stopSliding();

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

        if (state.animatable().isSliding())
            controller.setAnimation(SLIDE);
        else if (state.isMoving())
            controller.setAnimation(WALK);
        else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    private void tickEggLaying() {
        if (this.level().isClientSide() || this.isBaby())
            return;

        if (--this.eggTime > 0)
            return;

        this.playSound(SoundEvents.CHICKEN_EGG, 1F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1F);
        this.spawnAtLocation((ServerLevel) this.level(), ReAnimalItems.PENGUIN_EGG);
        this.gameEvent(GameEvent.ENTITY_PLACE, this);

        this.eggTime = this.random.nextInt(6000) + 6000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 8D)
                .add(Attributes.MOVEMENT_SPEED, 0.175D)
                .add(Attributes.FOLLOW_RANGE, 8D);
    }
}
