package it.hurts.shatterbyte.reanimal.common.entity.toucan;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Comparator;

public class ToucanEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.toucan_adult.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.toucan_adult.walk");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.toucan_adult.fly");
    private static final RawAnimation EAT = RawAnimation.begin().thenPlay("animation.toucan_adult.eat");
    private static final RawAnimation IDLE_BABY = RawAnimation.begin().thenLoop("animation.toucan_baby.idle");
    private static final RawAnimation WALK_BABY = RawAnimation.begin().thenLoop("animation.toucan_baby.walk");

    private static final int EATING_DURATION = 25;
    private static final int FOOD_PICKUP_TICK = 6;
    private static final int FOOD_SWALLOW_TICK = 20;

    private static final EntityDataAccessor<Integer> EATING_TICKS = SynchedEntityData.defineId(ToucanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(ToucanEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private ItemEntity targetFood;
    private int foodSearchTicks;
    private int flightTicks;
    private int flightCooldown = 200 + this.random.nextInt(400);
    private int eggTime = this.random.nextInt(6000) + 6000;

    public ToucanEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.getNavigation().setCanFloat(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(EATING_TICKS, 0);
        builder.define(FLYING, false);
    }

    public int getEatingTicks() {
        return this.getEntityData().get(EATING_TICKS);
    }

    public boolean isEating() {
        return this.getEatingTicks() > 0;
    }

    public boolean isFlying() {
        return this.getEntityData().get(FLYING);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (this.flightTicks == 0 && (this.onGround() || this.isInWater()))
            this.getEntityData().set(FLYING, false);

        if (this.isEating()) {
            var ticks = this.getEatingTicks() - 1;

            this.getEntityData().set(EATING_TICKS, ticks);
            this.getNavigation().stop();

            if (ticks == EATING_DURATION - FOOD_PICKUP_TICK) {
                if (this.targetFood != null && this.targetFood.isAlive() && this.isFood(this.targetFood.getItem()) && this.isFoodInReach()) {
                    var stack = this.targetFood.getItem();

                    this.setItemSlot(EquipmentSlot.MAINHAND, stack.split(1));
                    this.setDropChance(EquipmentSlot.MAINHAND, 1F);
                    this.take(this.targetFood, 1);

                    if (stack.isEmpty())
                        this.targetFood.discard();
                } else
                    this.getEntityData().set(EATING_TICKS, 0);

                this.targetFood = null;
            } else if (ticks == EATING_DURATION - FOOD_SWALLOW_TICK) {
                this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                this.playSound(SoundEvents.GENERIC_EAT.value(), 0.5F, 1F);
                this.gameEvent(GameEvent.EAT);
            }
        } else if (this.flightTicks > 0) {
            if (--this.flightTicks == 0 || this.getNavigation().isDone() || this.isInWater())
                this.stopFlying();
        } else if (!this.tickFood(level)) {
            if (this.onGround() && !this.isInWater() && !this.isBaby() && !this.isPanicking() && --this.flightCooldown <= 0)
                this.startFlying();

            if (this.flightTicks == 0) {
                ((Brain<ToucanEntity>) this.getBrain()).tick(level, this);

                ToucanAI.updateActivity(this);
            }
        }

        super.customServerAiStep(level);
    }

    private boolean tickFood(ServerLevel level) {
        if (this.isBaby() || this.isFlying() || this.isInWater() || this.isPanicking())
            return false;

        if (this.targetFood != null && (!this.targetFood.isAlive() || !this.isFood(this.targetFood.getItem()) || this.distanceToSqr(this.targetFood) > 256D)) {
            this.targetFood = null;
            this.getNavigation().stop();
        }

        if (this.targetFood == null && this.onGround() && --this.foodSearchTicks <= 0) {
            this.foodSearchTicks = 20;
            this.targetFood = level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(16D, 4D, 16D),
                            item -> item.onGround() && this.isFood(item.getItem()) && this.hasLineOfSight(item))
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);

            if (this.targetFood != null) {
                this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                this.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
                this.getNavigation().stop();
            }
        }

        if (this.targetFood == null)
            return false;

        this.getLookControl().setLookAt(this.targetFood);

        if (this.isFoodInReach()) {
            this.getNavigation().stop();
            this.getMoveControl().setWait();
            this.getEntityData().set(EATING_TICKS, EATING_DURATION);
        } else if (this.onGround() && !this.getNavigation().moveTo(this.targetFood, 0, 1.2D)) {
            this.targetFood = null;
            this.getNavigation().stop();

            return false;
        }

        return true;
    }

    private boolean isFoodInReach() {
        var pickupReach = this.getPickupReach();

        return this.getBoundingBox().inflate(pickupReach.getX(), pickupReach.getY(), pickupReach.getZ()).intersects(this.targetFood.getBoundingBox()) && this.hasLineOfSight(this.targetFood);
    }

    private void startFlying() {
        this.flightCooldown = 200 + this.random.nextInt(400);

        var target = this.position().add(this.random.nextInt(17) - 8, 3 + this.random.nextInt(4), this.random.nextInt(17) - 8);
        var navigation = new FlyingPathNavigation(this, this.level());

        navigation.setCanFloat(true);

        var path = navigation.createPath(BlockPos.containing(target), 0);

        if (path == null || !path.canReach())
            return;

        this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.getNavigation().stop();
        this.navigation = navigation;
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.flightTicks = 100;
        this.getEntityData().set(FLYING, true);
        this.setNoGravity(true);
        this.getNavigation().moveTo(path, 1D);
    }

    private void stopFlying() {
        this.getNavigation().stop();
        this.flightTicks = 0;
        this.setNoGravity(false);
        this.setYya(0F);
        this.setZza(0F);
        this.setXRot(0F);
        this.moveControl = new MoveControl(this);
        this.navigation = new GroundPathNavigation(this, this.level());
        this.getNavigation().setCanFloat(true);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.tickEggLaying();

        if (!this.onGround() && this.getDeltaMovement().y < -0.1D) {
            var motion = this.getDeltaMovement();

            this.setDeltaMovement(motion.x, -0.1D, motion.z);
        }

        if (this.isEating()) {
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            this.setXRot(0F);
            this.setDeltaMovement(0D, this.getDeltaMovement().y, 0D);
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("eatingTicks", this.getEatingTicks());
        tag.putInt("eggTime", this.eggTime);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.getEntityData().set(EATING_TICKS, tag.getIntOr("eatingTicks", 0));
        this.eggTime = tag.getIntOr("eggTime", this.random.nextInt(6000) + 6000);
        this.stopFlying();
    }

    private void tickEggLaying() {
        if (this.level().isClientSide() || this.isBaby())
            return;

        if (--this.eggTime > 0)
            return;

        this.playSound(SoundEvents.CHICKEN_EGG, 1F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1F);
        this.spawnAtLocation((ServerLevel) this.level(), ReAnimalItems.TOUCAN_EGG);
        this.gameEvent(GameEvent.ENTITY_PLACE, this);

        this.eggTime = this.random.nextInt(6000) + 6000;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result && this.isAlive()) {
            if (this.isFlying())
                this.stopFlying();

            this.targetFood = null;
            this.getEntityData().set(EATING_TICKS, 0);
            this.spawnAtLocation(level, this.getMainHandItem());
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);

            ToucanAI.updateActivity(this);
        }

        return result;
    }

    @Override
    public boolean causeFallDamage(double distance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        this.resetFallDistance();
    }

    protected Brain.Provider<ToucanEntity> brainProvider() {
        return ToucanAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return ToucanAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.TOUCAN_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isBaby() ? ReAnimalSoundEvents.TOUCAN_IDLE_BABY : ReAnimalSoundEvents.TOUCAN_IDLE_ADULT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.TOUCAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isBaby() ? ReAnimalSoundEvents.TOUCAN_DEATH_BABY : ReAnimalSoundEvents.TOUCAN_DEATH_ADULT;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ReAnimalEntities.TOUCAN.create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 0, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationTest<ToucanEntity> state) {
        var entity = state.animatable();

        if (entity.isBaby())
            state.controller().setAnimation(state.isMoving() ? WALK_BABY : IDLE_BABY);
        else if (entity.isEating())
            state.controller().setAnimation(EAT);
        else if (entity.isFlying())
            state.controller().setAnimation(FLY);
        else
            state.controller().setAnimation(state.isMoving() ? WALK : IDLE);

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 6D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FLYING_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 16D);
    }
}
