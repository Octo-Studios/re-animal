package it.hurts.shatterbyte.reanimal.common.entity.seal;

import com.mojang.serialization.Dynamic;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class SealEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.seal.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.seal.walk");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.seal.swim");
    private static final RawAnimation LAYING = RawAnimation.begin().then("animation.seal.laying", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation LAY = RawAnimation.begin().thenLoop("animation.seal.lay");
    private static final RawAnimation LAY_SLAP = RawAnimation.begin().thenLoop("animation.seal.lay_slap");
    private static final RawAnimation GETTING_UP = RawAnimation.begin().then("animation.seal.getting_up", Animation.LoopType.PLAY_ONCE);

    private static final EntityDataAccessor<Integer> LAY_STATE = SynchedEntityData.defineId(SealEntity.class, EntityDataSerializers.INT);

    private static final int LAY_TRANSITION_TICKS = 14;

    public static final UniformInt LAY_DURATION = UniformInt.of(400, 800);
    private static final UniformInt LAY_COOLDOWN = UniformInt.of(200, 400);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public int layTime;
    public int layCooldown;

    @Nullable
    private BlockPos environmentTarget;

    public SealEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.4F, 1F, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.navigation = new AmphibiousPathNavigation(this, level);

        this.setPathfindingMalus(PathType.WATER, 0F);
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
    protected void customServerAiStep() {
        var level = this.level();
        var profiler = level.getProfiler();

        profiler.push("sealBrain");
        ((Brain<SealEntity>) this.getBrain()).tick((ServerLevel) this.level(), this);
        profiler.pop();

        profiler.push("sealActivityUpdate");
        SealAI.updateActivity(this);
        profiler.pop();

        super.customServerAiStep();
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.isLaying()) {
            this.setXRot(0);
            this.xRotO = 0;

            this.yHeadRot = this.yBodyRot;
            this.yHeadRotO = this.yBodyRotO;
        }

        if (!this.level().isClientSide && this.isLaying())
            this.getNavigation().stop();
    }

    @Override
    protected Brain.Provider<SealEntity> brainProvider() {
        return SealAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return SealAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.SEAL_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.SEAL_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.SEAL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.SEAL_DEATH.get();
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.SEAL.get().create(level);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        var result = super.hurt(source, amount);

        if (result) {
            this.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);

            SealAI.updateActivity(this);
        }

        return result;
    }

    @Override
    public void updateSwimming() {
        this.setSwimming(this.isEffectiveAi() && this.isInWaterOrBubble());
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWaterOrBubble() && this.isSwimming()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));

            if (this.horizontalCollision)
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
        controllers.add(new AnimationController<>(this, "main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationState<SealEntity> state) {
        var controller = state.getController();
        var entity = state.getAnimatable();

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
                if (state.isMoving()) {
                    if (this.isInWater())
                        controller.setAnimation(SWIM);
                    else
                        controller.setAnimation(WALK);
                } else
                    controller.setAnimation(IDLE);
            }
        }

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(NeoForgeMod.SWIM_SPEED, 0.75D)
                .add(Attributes.FOLLOW_RANGE, 16D)
                .add(Attributes.STEP_HEIGHT, 1.1D)
                .add(Attributes.ATTACK_DAMAGE, 4D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder buidler) {
        super.defineSynchedData(buidler);

        buidler.define(LAY_STATE, LayState.STANDING.ordinal());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("LayState", this.entityData.get(LAY_STATE));
        tag.putInt("LayTime", this.layTime);
        tag.putInt("LayCooldown", this.layCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("LayState"))
            this.setLayState(LayState.byId(tag.getInt("LayState")));

        if (tag.contains("LayTime"))
            this.layTime = tag.getInt("LayTime");

        if (tag.contains("LayCooldown"))
            this.layCooldown = tag.getInt("LayCooldown");
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
                && !this.isInWaterOrBubble()
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
