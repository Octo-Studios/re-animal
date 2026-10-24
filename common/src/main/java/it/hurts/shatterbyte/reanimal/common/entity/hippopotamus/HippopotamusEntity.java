package it.hurts.shatterbyte.reanimal.common.entity.hippopotamus;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.util.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

public class HippopotamusEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.hippopotamus.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.hippopotamus.walk");
    private static final RawAnimation LAYING = RawAnimation.begin().then("animation.hippopotamus.laying", LoopType.PLAY_ONCE);
    private static final RawAnimation IDLE_LAY = RawAnimation.begin().thenLoop("animation.hippopotamus.idle_lay");
    private static final RawAnimation GETTING_UP = RawAnimation.begin().then("animation.hippopotamus.getting_up", LoopType.PLAY_ONCE);
    private static final RawAnimation ATTACK_1 = RawAnimation.begin().then("animation.hippopotamus.attack_1", LoopType.PLAY_ONCE);
    private static final RawAnimation ATTACK_2 = RawAnimation.begin().then("animation.hippopotamus.attack_2", LoopType.PLAY_ONCE);

    private static final RawAnimation[] ATTACKS = new RawAnimation[]{ATTACK_1, ATTACK_2};

    private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(HippopotamusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LAY_STATE = SynchedEntityData.defineId(HippopotamusEntity.class, EntityDataSerializers.INT);

    private static final int LAY_TRANSITION_TICKS = 15;

    public static final UniformInt LAY_DURATION = UniformInt.of(200, 400);
    private static final UniformInt LAY_COOLDOWN = UniformInt.of(200, 400);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int attackAnimationTicks;
    public int layTime;
    public int layCooldown;

    public HippopotamusEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.getNavigation().setCanFloat(true);
    }

    public boolean isAttacking() {
        return this.entityData.get(ATTACKING);
    }

    public void setAttacking(boolean attacking) {
        this.entityData.set(ATTACKING, attacking);
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

    public void startAttackAnimation() {
        this.attackAnimationTicks = HippopotamusAI.ATTACK_ANIMATION_TICKS;

        this.setAttacking(true);
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        var brain = (Brain<HippopotamusEntity>) this.getBrain();

        var lastAttacker = this.getLastHurtByMob();

        if (lastAttacker != null && HippopotamusAI.isValidTarget(this, lastAttacker))
            brain.setMemory(MemoryModuleType.ATTACK_TARGET, lastAttacker);

        brain.getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
            if (!HippopotamusAI.isValidTarget(this, target) || HippopotamusAI.isHoldingFavoriteFood(target)) {
                brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            }
        });

        var level = this.level();

        ((Brain<HippopotamusEntity>) this.getBrain()).tick(serverLevel, this);

        HippopotamusAI.updateActivity(this);

        super.customServerAiStep(serverLevel);
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

        if (!this.isBaby()) {
            if (brain.hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER))
                brain.eraseMemory(MemoryModuleType.TEMPTING_PLAYER);
        }

        if (!this.level().isClientSide() && this.attackAnimationTicks > 0) {
            this.attackAnimationTicks--;

            if (this.attackAnimationTicks == 0 && this.isAttacking())
                this.setAttacking(false);
        }

        if (!this.level().isClientSide() && this.isLaying())
            this.getNavigation().stop();
    }
    protected Brain.Provider<HippopotamusEntity> brainProvider() {
        return HippopotamusAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return HippopotamusAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.HIPPOPOTAMUS_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.HIPPOPOTAMUS_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.HIPPOPOTAMUS_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.HIPPOPOTAMUS_DEATH;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        var direct = source.getDirectEntity();

        if (direct instanceof AbstractArrow && source.is(DamageTypeTags.IS_PROJECTILE))
            return true;

        return super.isInvulnerableTo(level, source);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder buidler) {
        super.defineSynchedData(buidler);

        buidler.define(ATTACKING, false);
        buidler.define(LAY_STATE, LayState.STANDING.ordinal());
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.HIPPOPOTAMUS.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        return super.doHurtTarget(level, target);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::mainPredicate));
        controllers.add(new AnimationController<>("attack", 5, this::attackPredicate));
    }

    private PlayState mainPredicate(AnimationTest<HippopotamusEntity> state) {
        var controller = state.controller();

        switch (this.getLayState()) {
            case LAYING_DOWN -> controller.setAnimation(LAYING);
            case LAYING -> controller.setAnimation(IDLE_LAY);
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

    private PlayState attackPredicate(AnimationTest<HippopotamusEntity> state) {
        var controller = state.controller();

        if (this.isAttacking()) {
            this.setAttacking(false);

            controller.reset();
            controller.setAnimation(Util.getRandom(ATTACKS, this.getRandom()));
        }

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 60D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 16D)
                .add(Attributes.ATTACK_DAMAGE, 10D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.STEP_HEIGHT, 1.1D);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("LayState", this.entityData.get(LAY_STATE));
        tag.putInt("LayTime", this.layTime);
        tag.putInt("LayCooldown", this.layCooldown);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.setLayState(LayState.byId(tag.getIntOr("LayState", 0)));

        this.layTime = tag.getIntOr("LayTime", 0);

        this.layCooldown = tag.getIntOr("LayCooldown", 0);
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                if (HippopotamusEntity.this.isLaying())
                    return;

                super.clientTick();
            }
        };
    }

    public boolean canStartLaying() {
        return !this.isVehicle()
                && !this.isInWater()
                && !this.getNavigation().isInProgress()
                && this.getDeltaMovement().horizontalDistanceSqr() < 0.0001D
                && !this.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
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
