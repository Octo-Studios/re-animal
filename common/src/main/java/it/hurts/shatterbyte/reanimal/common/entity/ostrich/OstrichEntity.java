package it.hurts.shatterbyte.reanimal.common.entity.ostrich;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;

public class OstrichEntity extends Animal implements GeoEntity, PlayerRideableJumping {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ostrich.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.ostrich.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.ostrich.run");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(OstrichEntity.class, EntityDataSerializers.BOOLEAN);

    private float jumpPower;

    private int eggTime = this.random.nextInt(6000) + 6000;

    public boolean isSaddled() {
        return this.entityData.get(SADDLED);
    }

    public void setSaddled(boolean saddled) {
        this.entityData.set(SADDLED, saddled);
    }

    public OstrichEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.getNavigation().setCanFloat(true);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.tickEggLaying();

        if (!this.level().isClientSide() && this.isVehicle() && this.isInWater())
            this.ejectPassengers();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isBaby())
            return super.mobInteract(player, hand);

        var stack = player.getItemInHand(hand);
        var level = this.level();

        if (stack.is(Items.SADDLE) && !this.isSaddled()) {
            if (!level.isClientSide()) {
                this.setSaddled(true);

                if (!player.getAbilities().instabuild)
                    stack.shrink(1);

                level.playSound(null, this, SoundEvents.HORSE_SADDLE, this.getSoundSource(), 1F, 1F);

                this.gameEvent(GameEvent.EQUIP);
            }
            return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
        }

        if (this.isSaddled() && !this.isVehicle()) {
            if (!level.isClientSide())
                player.startRiding(this);

            return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void travel(Vec3 travelVector) {
        var rider = this.getControllingPassenger();

        if (rider != null && this.isSaddled() && this.isVehicle()) {
            this.setYRot(rider.getYRot());

            this.yRotO = this.getYRot();

            this.setXRot(rider.getXRot() * 0.5F);

            this.setRot(this.getYRot(), this.getXRot());

            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();

            float strafe = rider.xxa * 0.5F;
            float forward = rider.zza;

            if (forward <= 0.0F)
                forward *= 0.25F;

            this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.5F);

            super.travel(new Vec3(strafe, travelVector.y, forward));
        } else
            super.travel(travelVector);
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        var level = this.level();

        ((Brain<OstrichEntity>) this.getBrain()).tick(serverLevel, this);

        OstrichAI.updateActivity(this);

        this.setSprinting(this.isPanicking());

        super.customServerAiStep(serverLevel);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result) {
            this.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);

            OstrichAI.updateActivity(this);
        }

        return result;
    }

    @Override
    public void onPlayerJump(int power) {
        this.jumpPower = Math.min(1F, power / 100F);

        if (!this.isSaddled() || !this.isVehicle() || !this.onGround())
            return;

        if (this.jumpPower <= 0F)
            return;

        var jumpY = 0.5D * this.jumpPower;

        var motion = this.getDeltaMovement();
        var look = this.getLookAngle();

        var forwardBoost = 2D * this.jumpPower;

        this.setDeltaMovement(motion.x + look.x * forwardBoost, jumpY, motion.z + look.z * forwardBoost);

        this.needsSync = true;
    }

    @Override
    public boolean canJump() {
        return this.isSaddled() && this.isVehicle();
    }

    @Override
    public void handleStartJump(int power) {
        this.jumpPower = 0.0F;
    }

    @Override
    public void handleStopJump() {

    }

    @Override
    public LivingEntity getControllingPassenger() {
        if (this.getFirstPassenger() instanceof LivingEntity living)
            return living;

        return null;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);

        this.ejectPassengers();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);

        if (this.isSaddled())
            this.spawnAtLocation(level, Items.SADDLE);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putBoolean("saddled", this.isSaddled());
        tag.putInt("eggTime", this.eggTime);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.setSaddled(tag.getBooleanOr("saddled", false));
        this.eggTime = tag.getIntOr("eggTime", this.random.nextInt(6000) + 6000);
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity entity) {
        return super.getPassengerRidingPosition(entity).add(0, -1, 0);
    }
    protected Brain.Provider<OstrichEntity> brainProvider() {
        return OstrichAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return OstrichAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(SADDLED, false);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.OSTRICH_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.OSTRICH_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.OSTRICH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.OSTRICH_DEATH;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.OSTRICH.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationTest<OstrichEntity> state) {
        var controller = state.controller();
        var entity = state.animatable();

        if (state.isMoving()) {
            if (entity.isSprinting() || entity.isVehicle())
                controller.setAnimation(RUN);
            else
                controller.setAnimation(WALK);
        } else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    private void tickEggLaying() {
        if (this.level().isClientSide() || this.isBaby())
            return;

        if (--this.eggTime > 0)
            return;

        this.playSound(SoundEvents.CHICKEN_EGG, 1F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1F);
        this.spawnAtLocation((ServerLevel) this.level(), ReAnimalItems.OSTRICH_EGG);
        this.gameEvent(GameEvent.ENTITY_PLACE, this);

        this.eggTime = this.random.nextInt(6000) + 6000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 16D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D)
                .add(Attributes.STEP_HEIGHT, 1.1D)
                .add(Attributes.SAFE_FALL_DISTANCE, 8D);
    }
}
