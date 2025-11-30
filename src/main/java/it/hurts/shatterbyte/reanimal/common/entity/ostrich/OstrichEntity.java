package it.hurts.shatterbyte.reanimal.common.entity.ostrich;

import com.mojang.serialization.Dynamic;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class OstrichEntity extends Animal implements GeoEntity, PlayerRideableJumping {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ostrich.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.ostrich.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.ostrich.run");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(OstrichEntity.class, EntityDataSerializers.BOOLEAN);

    private float jumpPower;
    private boolean jumpingFromPlayer;

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
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (this.isSaddled() && !this.isVehicle()) {
            if (!level.isClientSide())
                player.startRiding(this);

            return InteractionResult.sidedSuccess(level.isClientSide());
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

            if (!this.onGround())
                this.jumpingFromPlayer = false;
        } else
            super.travel(travelVector);
    }

    @Override
    protected void customServerAiStep() {
        var level = this.level();
        var profiler = level.getProfiler();

        profiler.push("ostrichBrain");
        ((Brain<OstrichEntity>) this.getBrain()).tick((ServerLevel) this.level(), this);
        profiler.pop();

        profiler.push("ostrichActivityUpdate");
        OstrichAI.updateActivity(this);
        profiler.pop();

        this.setSprinting(this.isPanicking());

        super.customServerAiStep();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        var result = super.hurt(source, amount);

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

        this.hasImpulse = true;
        this.jumpingFromPlayer = true;
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
        this.jumpingFromPlayer = false;
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
            this.spawnAtLocation(Items.SADDLE);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putBoolean("saddled", this.isSaddled());
        tag.putInt("eggTime", this.eggTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.setSaddled(tag.getBoolean("saddled"));
        this.eggTime = tag.contains("eggTime") ? tag.getInt("eggTime") : this.random.nextInt(6000) + 6000;
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity entity) {
        return super.getPassengerRidingPosition(entity).add(0, -1, 0);
    }

    @Override
    protected Brain.Provider<OstrichEntity> brainProvider() {
        return OstrichAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return OstrichAI.makeBrain(this.brainProvider().makeBrain(dynamic));
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
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.OSTRICH.get().create(level);

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
        controllers.add(new AnimationController<>(this, "main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationState<OstrichEntity> state) {
        var controller = state.getController();
        var entity = state.getAnimatable();

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
        this.spawnAtLocation(ReAnimalItems.OSTRICH_EGG.get());
        this.gameEvent(GameEvent.ENTITY_PLACE, this);

        this.eggTime = this.random.nextInt(6000) + 6000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 16D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D)
                .add(Attributes.STEP_HEIGHT, 1.1D)
                .add(Attributes.SAFE_FALL_DISTANCE, 8D);
    }
}
