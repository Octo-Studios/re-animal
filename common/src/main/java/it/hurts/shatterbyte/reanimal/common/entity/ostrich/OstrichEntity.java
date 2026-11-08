package it.hurts.shatterbyte.reanimal.common.entity.ostrich;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import it.hurts.shatterbyte.reanimal.init.ReAnimalBlocks;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public class OstrichEntity extends TamableAnimal implements GeoEntity, PlayerRideableJumping {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ostrich.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.ostrich.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.ostrich.run");
    private static final RawAnimation IDLE_SIT = RawAnimation.begin().thenLoop("animation.ostrich.idle_sit");
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().then("animation.ostrich.sitting", LoopType.PLAY_ONCE).thenLoop("animation.ostrich.idle_sit");
    private static final RawAnimation GETTING_UP = RawAnimation.begin().then("animation.ostrich.getting_up", LoopType.PLAY_ONCE);
    private static final RawAnimation COURTSHIP = RawAnimation.begin().then("animation.ostrich.courtship", LoopType.PLAY_ONCE);
    private static final RawAnimation KICK_LEFT = RawAnimation.begin().then("animation.ostrich.kick_left", LoopType.PLAY_ONCE);
    private static final RawAnimation KICK_RIGHT = RawAnimation.begin().then("animation.ostrich.kick_right", LoopType.PLAY_ONCE);
    private static final int KICK_DURATION_TICKS = 12;
    private static final int KICK_HIT_TICK = 4;
    private static final int KICK_COOLDOWN_TICKS = 40;
    private static final int SIT_TRANSITION_TICKS = 10;
    private static final int STAND_TRANSITION_TICKS = 5;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(OstrichEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> KICKING = SynchedEntityData.defineId(OstrichEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> COURTING = SynchedEntityData.defineId(OstrichEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SIT_STATE = SynchedEntityData.defineId(OstrichEntity.class, EntityDataSerializers.INT);

    private float jumpPower;
    private LivingEntity kickTarget;
    private int kickTicks;
    private int nextKickTick;
    private float kickYaw;
    private int sitTransitionTicks;

    private static final int EGG_RETRY_TICKS = 100;
    private static final int EGG_DELAY_MIN = 100;
    private static final int EGG_DELAY_MAX = 200;

    private boolean hasEgg;
    private int eggTime;

    public boolean isSaddled() {
        return this.entityData.get(SADDLED);
    }

    public void setSaddled(boolean saddled) {
        this.entityData.set(SADDLED, saddled);
    }

    public boolean isKicking() {
        return this.entityData.get(KICKING);
    }

    public boolean isCourting() {
        return this.entityData.get(COURTING);
    }

    public void startCourtship() {
        this.entityData.set(COURTING, true);
    }

    public void stopCourtship() {
        this.entityData.set(COURTING, false);
        this.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
        this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    public OstrichEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);

        this.getNavigation().setCanFloat(true);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.tickEggLaying();

        if (!this.level().isClientSide())
            this.tickSitTransition();

        if (this.getSitState() != SitState.STANDING) {
            this.getNavigation().stop();
            this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            this.setJumping(false);

            this.zza = 0F;
            this.xxa = 0F;

            if (this.onGround())
                this.setDeltaMovement(0D, this.getDeltaMovement().y, 0D);
        }

        if (this.isKicking() || this.isCourting()) {
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
        }

        if (!this.level().isClientSide() && this.isVehicle() && this.isInWater())
            this.ejectPassengers();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        var level = this.level();

        if (this.isFood(stack)) {
            if (!this.isTame()) {
                if (!level.isClientSide()) {
                    if (!player.getAbilities().instabuild)
                        stack.shrink(1);

                    if (this.random.nextInt(3) == 0) {
                        this.tame(player);
                        this.setTarget(null);
                        this.getNavigation().stop();

                        level.broadcastEntityEvent(this, (byte) 7);
                    } else
                        level.broadcastEntityEvent(this, (byte) 6);
                }

                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            }

            if (this.isOwnedBy(player) && this.getHealth() < this.getMaxHealth()) {
                if (!level.isClientSide()) {
                    if (!player.getAbilities().instabuild)
                        stack.shrink(1);

                    this.heal(2F);
                }

                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            }

            return super.mobInteract(player, hand);
        }

        if (this.isKicking() || this.isCourting())
            return super.mobInteract(player, hand);

        if (this.isTame() && !this.isBaby() && stack.is(Items.SADDLE) && !this.isSaddled()) {
            if (!level.isClientSide()) {
                this.setSaddled(true);

                if (!player.getAbilities().instabuild)
                    stack.shrink(1);

                level.playSound(null, this, SoundEvents.HORSE_SADDLE, this.getSoundSource(), 1F, 1F);

                this.gameEvent(GameEvent.EQUIP);
            }
            return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
        }

        if (hand == InteractionHand.MAIN_HAND && this.isTame() && this.isOwnedBy(player) && !this.isVehicle()
                && (!this.isSaddled() || player.isSecondaryUseActive() || this.getSitState() != SitState.STANDING)) {
            if (level.isClientSide())
                this.setSitState(this.isSittingDownOrSitting() ? SitState.GETTING_UP : SitState.SITTING_DOWN);
            else
                this.setSitting(!this.isSittingDownOrSitting());

            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }

        if (this.isTame() && !this.isBaby() && this.isSaddled() && !this.isVehicle()
                && this.getSitState() == SitState.STANDING && !player.isSecondaryUseActive()) {
            if (!level.isClientSide())
                player.startRiding(this);

            return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
        }

        return super.mobInteract(player, hand);
    }

    private boolean isSittingDownOrSitting() {
        return this.getSitState() == SitState.SITTING_DOWN || this.getSitState() == SitState.SITTING;
    }

    private void setSitting(boolean sitting) {
        this.setOrderedToSit(sitting);
        this.setInSittingPose(sitting);
        this.setSitState(sitting ? SitState.SITTING_DOWN : SitState.GETTING_UP);
        this.sitTransitionTicks = SIT_TRANSITION_TICKS;

        this.getNavigation().stop();
        this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    private void tickSitTransition() {
        var state = this.getSitState();

        if (state != SitState.SITTING_DOWN && state != SitState.GETTING_UP)
            return;

        if (--this.sitTransitionTicks == 0)
            this.setSitState(state == SitState.SITTING_DOWN ? SitState.SITTING : SitState.STANDING);
    }

    private SitState getSitState() {
        return SitState.values()[this.entityData.get(SIT_STATE)];
    }

    private void setSitState(SitState state) {
        this.entityData.set(SIT_STATE, state.ordinal());
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isKicking() || this.isCourting() || this.getSitState() != SitState.STANDING) {
            if (this.isKicking())
                this.setYRot(this.kickYaw);

            if (this.hurtTime == 0)
                this.setDeltaMovement(0D, this.getDeltaMovement().y, 0D);

            super.travel(Vec3.ZERO);

            return;
        }

        var rider = this.getControllingPassenger();

        if (rider != null && this.isSaddled() && this.isVehicle()) {
            this.setYRot(rider.getYRot());

            this.yRotO = this.getYRot();

            this.setXRot(rider.getXRot() * 0.5F);

            this.setRot(this.getYRot(), this.getXRot());

            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();

            var strafe = rider.xxa * 0.5F;
            var forward = rider.zza;

            if (forward <= 0.0F)
                forward *= 0.25F;

            this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.5F);

            super.travel(new Vec3(strafe, travelVector.y, forward));
        } else
            super.travel(travelVector);
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        ((Brain<OstrichEntity>) this.getBrain()).tick(serverLevel, this);

        OstrichAI.updateActivity(this);

        this.setSprinting(this.isPanicking());
        this.tickKick(serverLevel);

        super.customServerAiStep(serverLevel);
    }

    private void tickKick(ServerLevel level) {
        if (this.kickTicks == 0) {
            if (!this.isPanicking() || this.tickCount < this.nextKickTick || this.isBaby() || this.isVehicle() || this.isPassenger()
                    || !this.onGround() || this.getSitState() != SitState.STANDING)
                return;

            var attacker = this.getLastHurtByMob();

            this.kickTarget = level.getEntitiesOfClass(LivingEntity.class, this.getAttackBoundingBox(1D), target ->
                            (target instanceof Player player ? !player.isCreative() : target == attacker)
                                    && this.canAttack(target) && !this.isAlliedTo(target)
                                    && this.isWithinMeleeAttackRange(target) && this.hasLineOfSight(target))
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);

            if (this.kickTarget == null)
                return;

            this.kickTicks = KICK_DURATION_TICKS + 1;
            this.nextKickTick = this.tickCount + KICK_COOLDOWN_TICKS;
            this.entityData.set(KICKING, true);

            this.triggerAnim("kick", this.random.nextBoolean() ? "left" : "right");
        }

        this.kickYaw = (float) Mth.atan2(this.getX() - this.kickTarget.getX(), this.kickTarget.getZ() - this.getZ()) * Mth.RAD_TO_DEG;

        this.setYRot(this.kickYaw);
        this.getLookControl().setLookAt(this.kickTarget, 180F, 45F);

        if (--this.kickTicks == KICK_DURATION_TICKS - KICK_HIT_TICK) {
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1F, 0.8F);

            if (this.canAttack(this.kickTarget) && this.isWithinMeleeAttackRange(this.kickTarget) && this.hasLineOfSight(this.kickTarget)
                    && this.doHurtTarget(level, this.kickTarget))
                this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1F, 0.7F);
        }

        if (this.kickTicks == 0) {
            this.entityData.set(KICKING, false);
            this.kickTarget = null;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result && !this.isOrderedToSit()) {
            this.getBrain().getMemory(MemoryModuleType.BREED_TARGET).ifPresent(partner -> {
                ((OstrichEntity) partner).stopCourtship();
                this.stopCourtship();
            });

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
        tag.putBoolean("hasEgg", this.hasEgg);
        tag.putInt("eggTime", this.eggTime);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.setSitState(this.isOrderedToSit() ? SitState.SITTING : SitState.STANDING);
        this.setSaddled(tag.getBooleanOr("saddled", false));
        this.hasEgg = tag.getBooleanOr("hasEgg", false);
        this.eggTime = tag.getIntOr("eggTime", 0);
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
        builder.define(KICKING, false);
        builder.define(COURTING, false);
        builder.define(SIT_STATE, SitState.STANDING.ordinal());
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
        var baby = ReAnimalEntities.OSTRICH.create(level, EntitySpawnReason.BREEDING);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal mate) {
        if (!(mate instanceof OstrichEntity ostrichMate)) {
            super.spawnChildFromBreeding(level, mate);

            return;
        }

        if (this.hasEgg || ostrichMate.hasEgg)
            return;

        this.hasEgg = true;
        this.eggTime = EGG_DELAY_MIN + this.random.nextInt(EGG_DELAY_MAX - EGG_DELAY_MIN);

        this.finalizeSpawnChildFromBreeding(level, mate, null);
    }

    @Override
    public boolean canMate(Animal other) {
        if (this.getSitState() != SitState.STANDING || this.hasEgg
                || other instanceof OstrichEntity ostrich && (ostrich.getSitState() != SitState.STANDING || ostrich.hasEgg))
            return false;

        return super.canMate(other);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::mainPredicate));
        controllers.add(new AnimationController<>("kick", 0, state -> PlayState.STOP)
                .triggerableAnim("left", KICK_LEFT)
                .triggerableAnim("right", KICK_RIGHT));
    }

    private PlayState mainPredicate(AnimationTest<OstrichEntity> state) {
        var controller = state.controller();
        var entity = state.animatable();

        controller.setTransitionTicks(this.getSitState() == SitState.STANDING ? STAND_TRANSITION_TICKS : 0);

        if (entity.isKicking())
            return PlayState.STOP;

        switch (this.getSitState()) {
            case SITTING_DOWN -> controller.setAnimation(SIT_DOWN);
            case SITTING -> controller.setAnimation(controller.getCurrentRawAnimation() == SIT_DOWN ? SIT_DOWN : IDLE_SIT);
            case GETTING_UP -> controller.setAnimation(GETTING_UP);
            case STANDING -> {
                if (entity.isCourting())
                    controller.setAnimation(COURTSHIP);
                else if (state.isMoving()) {
                    if (entity.isSprinting() || entity.isVehicle())
                        controller.setAnimation(RUN);
                    else
                        controller.setAnimation(WALK);
                } else
                    controller.setAnimation(IDLE);
            }
        }

        return PlayState.CONTINUE;
    }

    private void tickEggLaying() {
        if (this.level().isClientSide() || this.isBaby() || !this.hasEgg)
            return;

        if (--this.eggTime > 0)
            return;

        var level = (ServerLevel) this.level();
        var pos = this.blockPosition();
        var state = ReAnimalBlocks.OSTRICH_EGG.defaultBlockState();

        if (!level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty() || !state.canSurvive(level, pos)) {
            this.eggTime = EGG_RETRY_TICKS;

            return;
        }

        level.setBlock(pos, state, 3);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(this, state));

        this.playSound(SoundEvents.CHICKEN_EGG, 1F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1F);

        this.hasEgg = false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 8D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.5D)
                .add(Attributes.FOLLOW_RANGE, 16D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D)
                .add(Attributes.STEP_HEIGHT, 1.1D)
                .add(Attributes.SAFE_FALL_DISTANCE, 8D);
    }

    private enum SitState {
        STANDING,
        SITTING_DOWN,
        SITTING,
        GETTING_UP
    }
}
