package it.hurts.shatterbyte.reanimal.common.entity.kiwi;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

import java.util.Comparator;

public class KiwiEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.kiwi.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.kiwi.walk");
    private static final RawAnimation KICK = RawAnimation.begin().then("animation.kiwi.kick", LoopType.PLAY_ONCE);
    private static final RawAnimation BABY_IDLE = RawAnimation.begin().thenLoop("animation.kiwi_baby.idle");
    private static final RawAnimation BABY_WALK = RawAnimation.begin().thenLoop("animation.kiwi_baby.walk");
    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.28F, 0.4F);
    private static final int KICK_DURATION_TICKS = 16;
    private static final int KICK_JUMP_TICK = 2;
    private static final int KICK_HIT_TICK = 7;
    private static final int KICK_COOLDOWN_TICKS = 40;
    private static final EntityDataAccessor<Boolean> KICKING = SynchedEntityData.defineId(KiwiEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private LivingEntity kickTarget;
    private int kickTicks;
    private int nextKickTick;
    private float kickYaw;
    private int eggTime = this.random.nextInt(6000) + 6000;

    public KiwiEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.getNavigation().setCanFloat(true);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.tickEggLaying();

        if (this.isKicking()) {
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        ((Brain<KiwiEntity>) this.getBrain()).tick(serverLevel, this);

        KiwiAI.updateActivity(this);

        this.tickKick(serverLevel);

        super.customServerAiStep(serverLevel);
    }

    private void tickKick(ServerLevel level) {
        if (this.kickTicks == 0) {
            if (!this.isPanicking() || this.tickCount < this.nextKickTick || this.isBaby() || this.isVehicle() || this.isPassenger() || !this.onGround())
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
            this.triggerAnim("kick", "kick");
        }

        this.kickYaw = (float) Mth.atan2(this.getX() - this.kickTarget.getX(), this.kickTarget.getZ() - this.getZ()) * Mth.RAD_TO_DEG;

        this.setYRot(this.kickYaw);
        this.getLookControl().setLookAt(this.kickTarget, 180F, 45F);

        --this.kickTicks;

        if (this.kickTicks == KICK_DURATION_TICKS - KICK_JUMP_TICK) {
            this.jumpFromGround();
            this.setDeltaMovement(0D, this.getDeltaMovement().y * 1.1D, 0D);
        }

        if (this.kickTicks == KICK_DURATION_TICKS - KICK_HIT_TICK) {
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 1.4F);

            if (this.canAttack(this.kickTarget) && this.isWithinMeleeAttackRange(this.kickTarget) && this.hasLineOfSight(this.kickTarget)
                    && this.doHurtTarget(level, this.kickTarget))
                this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 0.6F, 1.4F);
        }

        if (this.kickTicks == 0) {
            this.entityData.set(KICKING, false);
            this.kickTarget = null;
        }
    }

    public boolean isKicking() {
        return this.entityData.get(KICKING);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isKicking()) {
            this.setYRot(this.kickYaw);

            if (this.hurtTime == 0)
                this.setDeltaMovement(0D, this.getDeltaMovement().y, 0D);

            super.travel(Vec3.ZERO);

            return;
        }

        super.travel(travelVector);
    }

    protected Brain.Provider<KiwiEntity> brainProvider() {
        return KiwiAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return KiwiAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(KICKING, false);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.KIWI_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.KIWI_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.KIWI_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.KIWI_DEATH;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.KIWI.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);

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
            this.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);

            KiwiAI.updateActivity(this);
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
        controllers.add(new AnimationController<>("kick", 0, state -> PlayState.STOP)
                .triggerableAnim("kick", KICK));
    }

    private PlayState mainPredicate(AnimationTest<KiwiEntity> state) {
        var controller = state.controller();
        var entity = state.animatable();

        if (entity.isKicking())
            return PlayState.STOP;

        if (state.isMoving())
            controller.setAnimation(entity.isBaby() ? BABY_WALK : WALK);
        else
            controller.setAnimation(entity.isBaby() ? BABY_IDLE : IDLE);

        return PlayState.CONTINUE;
    }

    private void tickEggLaying() {
        if (this.level().isClientSide() || this.isBaby())
            return;

        if (--this.eggTime > 0)
            return;

        this.playSound(SoundEvents.CHICKEN_EGG, 1F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1F);
        this.spawnAtLocation((ServerLevel) this.level(), ReAnimalItems.KIWI_EGG);
        this.gameEvent(GameEvent.ENTITY_PLACE, this);

        this.eggTime = this.random.nextInt(6000) + 6000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 4D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 3D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.25D)
                .add(Attributes.FOLLOW_RANGE, 8D);
    }
}
