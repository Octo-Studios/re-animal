package it.hurts.shatterbyte.reanimal.common.entity.hippopotamus;

import com.mojang.serialization.Dynamic;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.Util;
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
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HippopotamusEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.hippopotamus.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.hippopotamus.walk");
    private static final RawAnimation LAYING = RawAnimation.begin().then("animation.hippopotamus.laying", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation IDLE_LAY = RawAnimation.begin().thenLoop("animation.hippopotamus.idle_lay");
    private static final RawAnimation GETTING_UP = RawAnimation.begin().then("animation.hippopotamus.getting_up", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ATTACK_1 = RawAnimation.begin().then("animation.hippopotamus.attack_1", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ATTACK_2 = RawAnimation.begin().then("animation.hippopotamus.attack_2", Animation.LoopType.PLAY_ONCE);

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
    protected void customServerAiStep() {
        var brain = (Brain<HippopotamusEntity>) this.getBrain();
        var lastAttacker = this.getLastHurtByMob();

        if (!this.isBaby()) {
            if (lastAttacker != null && HippopotamusAI.isValidTarget(this, lastAttacker))
                brain.setMemory(MemoryModuleType.ATTACK_TARGET, lastAttacker);

            brain.getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
                if (!HippopotamusAI.isValidTarget(this, target) || HippopotamusAI.isHoldingFavoriteFood(target)) {
                    brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                    brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                }
            });
        } else {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }

        var level = this.level();
        var profiler = level.getProfiler();

        profiler.push("hippopotamusBrain");
        brain.tick((ServerLevel) this.level(), this);
        profiler.pop();

        profiler.push("hippopotamusActivityUpdate");
        HippopotamusAI.updateActivity(this);
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

        if (!this.isBaby()) {
            if (brain.hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER) || brain.hasMemoryValue(MemoryModuleType.IS_TEMPTED)) {
                brain.eraseMemory(MemoryModuleType.TEMPTING_PLAYER);
                brain.eraseMemory(MemoryModuleType.IS_TEMPTED);
            }
        }

        if (!this.level().isClientSide && this.attackAnimationTicks > 0) {
            this.attackAnimationTicks--;

            if (this.attackAnimationTicks == 0 && this.isAttacking())
                this.setAttacking(false);
        }

        if (!this.level().isClientSide && this.isLaying())
            this.getNavigation().stop();
    }

    @Override
    protected Brain.Provider<HippopotamusEntity> brainProvider() {
        return HippopotamusAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return HippopotamusAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.HIPPOPOTAMUS_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.HIPPOPOTAMUS_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.HIPPOPOTAMUS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.HIPPOPOTAMUS_DEATH.get();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        var direct = source.getDirectEntity();

        if (direct instanceof AbstractArrow && source.is(DamageTypeTags.IS_PROJECTILE))
            return true;

        return super.isInvulnerableTo(source);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder buidler) {
        super.defineSynchedData(buidler);

        buidler.define(ATTACKING, false);
        buidler.define(LAY_STATE, LayState.STANDING.ordinal());
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.HIPPOPOTAMUS.get().create(level);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }
    
   @Override
   public void spawnChildFromBreeding(ServerLevel level, Animal partner) {
    if (this.isInWater()) {
        AgeableMob baby = this.getBreedOffspring(level, partner);
        if (baby != null) {
            baby.setBaby(true);
            baby.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
            
            level.addFreshEntityWithNoPersistence(baby);
            level.broadcastEntityEvent(this, (byte)18);
            
            if (level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT)) {
                level.addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(level, this.getX(), this.getY(), this.getZ(), this.getRandom().nextInt(7) + 1));
            }
        }
    } else {
        this.setInLoveTime(600); 
        partner.setInLoveTime(600);
       }
   }
    
    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.isBaby()) {
            return false;
        }
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

    private PlayState mainPredicate(AnimationState<HippopotamusEntity> state) {
        var controller = state.getController();

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

    private PlayState attackPredicate(AnimationState<HippopotamusEntity> state) {
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
                .add(Attributes.MAX_HEALTH, 60D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 16D)
                .add(Attributes.ATTACK_DAMAGE, 10D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.STEP_HEIGHT, 1.1D);
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
                if (HippopotamusEntity.this.isLaying())
                    return;

                super.clientTick();
            }
        };
    }

    public boolean canStartLaying() {
        return !this.isVehicle()
                && !this.isInWaterOrBubble()
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
