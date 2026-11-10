package it.hurts.shatterbyte.reanimal.common.entity.giraffe;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

public class GiraffeEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.giraffe.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.giraffe.walk");

    private static final RawAnimation GRAZING = RawAnimation.begin().then("animation.giraffe.grazing", LoopType.PLAY_ONCE);
    private static final RawAnimation IDLE_GRAZE = RawAnimation.begin().thenLoop("animation.giraffe.idle_graze");
    private static final RawAnimation GETTING_UP = RawAnimation.begin().then("animation.giraffe.getting_up", LoopType.PLAY_ONCE);
    private static final RawAnimation KICK_FRONT_LEFT = RawAnimation.begin().then("animation.giraffe.kick_front_left", LoopType.PLAY_ONCE);
    private static final RawAnimation KICK_FRONT_RIGHT = RawAnimation.begin().then("animation.giraffe.kick_front_right", LoopType.PLAY_ONCE);
    private static final RawAnimation KICK_REAR_LEFT = RawAnimation.begin().then("animation.giraffe.kick_rear_left", LoopType.PLAY_ONCE);
    private static final RawAnimation KICK_REAR_RIGHT = RawAnimation.begin().then("animation.giraffe.kick_rear_right", LoopType.PLAY_ONCE);

    private static final EntityDataAccessor<Integer> GRAZE_STATE = SynchedEntityData.defineId(GiraffeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> KICKING = SynchedEntityData.defineId(GiraffeEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int GRAZING_DOWN_TICKS = 48;
    private static final int GETTING_UP_TICKS = 40;
    private static final int KICK_DURATION_TICKS = 22;
    private static final int KICK_HIT_TICK = 8;
    private static final int KICK_COOLDOWN_TICKS = 40;

    public static final UniformInt GRAZE_DURATION = UniformInt.of(200, 400);
    public static final UniformInt GRAZE_COOLDOWN = UniformInt.of(200, 400);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private LivingEntity kickTarget;
    private int kickTicks;
    private int nextKickTick;
    private float kickYaw;
    private boolean rearKick;

    public int grazeTime;
    public int grazeCooldown;
    public boolean eatingLeaves;

    public GiraffeEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.getNavigation().setCanFloat(true);
    }

    public boolean isGrazing() {
        var state = this.getGrazeState();

        return state == GrazeState.GRAZING_DOWN || state == GrazeState.GRAZING || state == GrazeState.GETTING_UP;
    }

    public GrazeState getGrazeState() {
        return GrazeState.byId(this.entityData.get(GRAZE_STATE));
    }

    public void setGrazeState(GrazeState state) {
        this.entityData.set(GRAZE_STATE, state.ordinal());
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        ((Brain<GiraffeEntity>) this.getBrain()).tick(serverLevel, this);

        GiraffeAI.updateActivity(this);
        this.tickKick(serverLevel);

        super.customServerAiStep(serverLevel);
    }

    private void tickKick(ServerLevel level) {
        if (this.kickTicks == 0) {
            if (!this.isPanicking() || this.tickCount < this.nextKickTick || this.isBaby() || this.isGrazing()
                    || this.isVehicle() || this.isPassenger() || !this.onGround() || this.isInWater())
                return;

            var attacker = this.getLastHurtByMob();

            if (attacker == null || attacker instanceof GiraffeEntity || !this.canAttack(attacker) || this.isAlliedTo(attacker)
                    || !this.isWithinMeleeAttackRange(attacker) || !this.hasLineOfSight(attacker))
                return;

            this.kickTarget = attacker;
            var offset = attacker.position().subtract(this.position());
            this.rearKick = Vec3.directionFromRotation(0F, this.yBodyRot).dot(offset) < 0D;
            var left = Vec3.directionFromRotation(0F, this.yBodyRot - 90F).dot(offset) > 0D;
            this.kickYaw = (float) Mth.atan2(-offset.x, offset.z) * Mth.RAD_TO_DEG + (this.rearKick ? 180F : 0F);
            this.kickTicks = KICK_DURATION_TICKS + 1;
            this.nextKickTick = this.tickCount + KICK_COOLDOWN_TICKS;
            this.entityData.set(KICKING, true);
            this.triggerAnim("kick", (this.rearKick ? "rear_" : "front_") + (left ? "left" : "right"));
        }

        this.getNavigation().stop();
        this.getMoveControl().setWait();
        this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        this.setJumping(false);
        this.setYRot(Mth.approachDegrees(this.getYRot(), this.kickYaw, 20F));

        if (--this.kickTicks == KICK_DURATION_TICKS - KICK_HIT_TICK) {
            var direction = Vec3.directionFromRotation(0F, this.getYRot()).dot(this.kickTarget.position().subtract(this.position()));

            if ((direction < 0D) == this.rearKick && this.canAttack(this.kickTarget)
                    && this.isWithinMeleeAttackRange(this.kickTarget) && this.hasLineOfSight(this.kickTarget)
                    && this.doHurtTarget(level, this.kickTarget))
                this.kickTarget.knockback(1.5D, this.getX() - this.kickTarget.getX(), this.getZ() - this.kickTarget.getZ(),
                        this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE), true);
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
            if (this.hurtTime == 0)
                this.setDeltaMovement(0D, this.getDeltaMovement().y, 0D);

            super.travel(Vec3.ZERO);
        } else
            super.travel(travelVector);
    }

    protected Brain.Provider<GiraffeEntity> brainProvider() {
        return GiraffeAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return GiraffeAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.GIRAFFE_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.GIRAFFE_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.GIRAFFE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.GIRAFFE_DEATH;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.GIRAFFE.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder buidler) {
        super.defineSynchedData(buidler);

        buidler.define(GRAZE_STATE, GrazeState.STANDING.ordinal());
        buidler.define(KICKING, false);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.isGrazing() || this.isKicking()) {
            this.setXRot(0);
            this.xRotO = 0;

            if (this.isKicking())
                this.yBodyRot = this.getYRot();

            this.yHeadRot = this.yBodyRot;
            this.yHeadRotO = this.yBodyRotO;
        }

        if (!this.level().isClientSide() && this.isGrazing())
            this.getNavigation().stop();
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("GrazeState", this.entityData.get(GRAZE_STATE));
        tag.putInt("GrazeTime", this.grazeTime);
        tag.putInt("GrazeCooldown", this.grazeCooldown);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.setGrazeState(GrazeState.byId(tag.getIntOr("GrazeState", 0)));

        this.grazeTime = tag.getIntOr("GrazeTime", 0);

        this.grazeCooldown = tag.getIntOr("GrazeCooldown", 0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result) {
            this.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);

            GiraffeAI.updateActivity(this);
        }

        return result;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                if (GiraffeEntity.this.isGrazing() || GiraffeEntity.this.isKicking())
                    return;

                super.clientTick();
            }
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 8, this::mainPredicate));
        controllers.add(new AnimationController<>("kick", 0, state -> PlayState.STOP)
                .triggerableAnim("front_left", KICK_FRONT_LEFT)
                .triggerableAnim("front_right", KICK_FRONT_RIGHT)
                .triggerableAnim("rear_left", KICK_REAR_LEFT)
                .triggerableAnim("rear_right", KICK_REAR_RIGHT));
    }

    private PlayState mainPredicate(AnimationTest<GiraffeEntity> state) {
        var controller = state.controller();

        if (this.isKicking())
            return state.setAndContinue(IDLE);

        switch (this.getGrazeState()) {
            case GRAZING_DOWN -> controller.setAnimation(GRAZING);
            case GRAZING -> controller.setAnimation(IDLE_GRAZE);
            case GETTING_UP -> controller.setAnimation(GETTING_UP);
            case STANDING -> {
                if (state.isMoving())
                    controller.setAnimation(WALK);
                else
                    controller.setAnimation(IDLE);
            }
        }

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 100D)
                .add(Attributes.ATTACK_DAMAGE, 12D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 8D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.STEP_HEIGHT, 1.1D);
    }

    public boolean canStartGrazing() {
        return !this.isVehicle()
                && !this.isInWater()
                && !this.getNavigation().isInProgress()
                && this.getDeltaMovement().horizontalDistanceSqr() < 0.0001D
                && !this.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING)
                && !this.getBrain().hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER)
                && this.random.nextInt(400) == 0;
    }

    public void startGrazingDown() {
        this.getNavigation().stop();

        this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        this.setGrazeState(GrazeState.GRAZING_DOWN);

        this.grazeTime = GRAZING_DOWN_TICKS;
    }

    public void startGettingUp() {
        this.setGrazeState(GrazeState.GETTING_UP);

        this.grazeTime = GETTING_UP_TICKS;
    }

    public void finishGettingUp() {
        this.setGrazeState(GrazeState.STANDING);

        this.grazeCooldown = GRAZE_COOLDOWN.sample(this.random);
    }

    public enum GrazeState {
        STANDING,
        GRAZING_DOWN,
        GRAZING,
        GETTING_UP;

        public static GrazeState byId(int id) {
            var values = GrazeState.values();

            return id >= 0 && id < values.length ? values[id] : STANDING;
        }
    }
}
