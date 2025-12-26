package it.hurts.shatterbyte.reanimal.common.entity.crocodile;

import com.mojang.serialization.Dynamic;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class CrocodileEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.crocodile.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.crocodile.walk");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.crocodile.swim");
    private static final RawAnimation ATTACK_1 = RawAnimation.begin().then("animation.crocodile.bite", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ATTACK_2 = RawAnimation.begin().then("animation.crocodile.spinn", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation[] ATTACKS = new RawAnimation[]{ATTACK_1, ATTACK_2};

    private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(CrocodileEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int attackAnimationTicks;

    private int eggTime = this.random.nextInt(6000) + 6000;

    @Nullable
    private BlockPos environmentTarget;

    public CrocodileEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.4F, 1F, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.navigation = new AmphibiousPathNavigation(this, level);

        this.setPathfindingMalus(PathType.WATER, 0F);
    }

    public boolean isAttacking() {
        return this.entityData.get(ATTACKING);
    }

    public void setAttacking(boolean attacking) {
        this.entityData.set(ATTACKING, attacking);
    }

    public void startAttackAnimation() {
        this.attackAnimationTicks = CrocodileAI.ATTACK_ANIMATION_TICKS;
        this.setAttacking(true);
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
        var brain = (Brain<CrocodileEntity>) this.getBrain();

        var lastAttacker = this.getLastHurtByMob();

        if (lastAttacker != null && CrocodileAI.isValidTarget(this, lastAttacker))
            brain.setMemory(MemoryModuleType.ATTACK_TARGET, lastAttacker);

        brain.getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
            if (!CrocodileAI.isValidTarget(this, target) || CrocodileAI.isHoldingFavoriteFood(target)) {
                brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            }
        });

        var level = this.level();
        var profiler = level.getProfiler();

        profiler.push("crocodileBrain");
        brain.tick((ServerLevel) this.level(), this);
        profiler.pop();

        profiler.push("crocodileActivityUpdate");
        CrocodileAI.updateActivity(this);
        profiler.pop();

        super.customServerAiStep();
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide && this.attackAnimationTicks > 0) {
            this.attackAnimationTicks--;

            if (this.attackAnimationTicks == 0 && this.isAttacking())
                this.setAttacking(false);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide)
            this.tickEggLaying();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("eggTime", this.eggTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.eggTime = tag.contains("eggTime") ? tag.getInt("eggTime") : this.random.nextInt(6000) + 6000;
    }

    @Override
    protected Brain.Provider<CrocodileEntity> brainProvider() {
        return CrocodileAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return CrocodileAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.CROCODILE_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.CROCODILE_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.CROCODILE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.CROCODILE_DEATH.get();
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.CROCODILE.get().create(level);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public void updateSwimming() {
        this.setSwimming(this.isEffectiveAi() && this.isInWaterOrBubble());
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWaterOrBubble() && this.isSwimming()) {
            var swimSpeed = (float) this.getAttributeValue(NeoForgeMod.SWIM_SPEED);

            this.moveRelative(swimSpeed, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));

            if (this.horizontalCollision)
                this.setDeltaMovement(this.getDeltaMovement().x, 0.3D, this.getDeltaMovement().z);
        } else
            super.travel(travelVector);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return super.doHurtTarget(target);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainPredicate));
        controllers.add(new AnimationController<>(this, "attack", 5, this::attackPredicate));
    }

    private PlayState mainPredicate(AnimationState<CrocodileEntity> state) {
        var controller = state.getController();

        if (state.isMoving()) {
            if (this.isInWater())
                controller.setAnimation(SWIM);
            else
                controller.setAnimation(WALK);
        } else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    private PlayState attackPredicate(AnimationState<CrocodileEntity> state) {
        var controller = state.getController();

        if (this.isAttacking()) {
            this.setAttacking(false);

            controller.forceAnimationReset();
            controller.setAnimation(Util.getRandom(ATTACKS, this.getRandom()));
        }

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(NeoForgeMod.SWIM_SPEED, 0.05D)
                .add(Attributes.FOLLOW_RANGE, 16D)
                .add(Attributes.STEP_HEIGHT, 1.1D)
                .add(Attributes.ATTACK_DAMAGE, 10D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder buidler) {
        super.defineSynchedData(buidler);

        buidler.define(ATTACKING, false);
    }

    private void tickEggLaying() {
        if (this.isBaby())
            return;

        if (--this.eggTime > 0)
            return;

        this.playSound(SoundEvents.CHICKEN_EGG, 1F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1F);
        this.spawnAtLocation(ReAnimalItems.CROCODILE_EGG.get());
        this.gameEvent(GameEvent.ENTITY_PLACE, this);

        this.eggTime = this.random.nextInt(6000) + 6000;
    }
}
