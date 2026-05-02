package it.hurts.shatterbyte.reanimal.common.entity.red_panda;

import com.mojang.serialization.Dynamic;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RedPandaEntity extends Animal implements GeoEntity {
    private static final RawAnimation ADULT_IDLE = RawAnimation.begin().thenLoop("animation.red_panda.idle");
    private static final RawAnimation ADULT_WALK = RawAnimation.begin().thenLoop("animation.red_panda.walk");
    private static final RawAnimation ADULT_ASCEND = RawAnimation.begin().then("animation.red_panda.ascend", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ADULT_STANDING = RawAnimation.begin().thenLoop("animation.red_panda.standing");
    private static final RawAnimation ADULT_DESCEND = RawAnimation.begin().then("animation.red_panda.descend", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation BABY_IDLE = RawAnimation.begin().thenLoop("animation.red_panda_baby.idle");
    private static final RawAnimation BABY_WALK = RawAnimation.begin().thenLoop("animation.red_panda_baby.walk");
    private static final RawAnimation BABY_ASCEND = RawAnimation.begin().then("animation.red_panda_baby.ascend", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BABY_STANDING = RawAnimation.begin().thenLoop("animation.red_panda_baby.standing");
    private static final RawAnimation BABY_DESCEND = RawAnimation.begin().then("animation.red_panda_baby.descend", Animation.LoopType.PLAY_ONCE);

    private static final EntityDataAccessor<Integer> STAND_STATE = SynchedEntityData.defineId(RedPandaEntity.class, EntityDataSerializers.INT);

    private static final int STAND_TRANSITION_TICKS = 10;
    private static final double STAND_PLAYER_RADIUS = 6D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int standTransitionTicks;

    public RedPandaEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.getNavigation().setCanFloat(true);
    }

    @Override
    protected void customServerAiStep() {
        var level = this.level();
        var profiler = level.getProfiler();

        profiler.push("redPandaBrain");
        ((Brain<RedPandaEntity>) this.getBrain()).tick((ServerLevel) this.level(), this);
        profiler.pop();

        profiler.push("redPandaActivityUpdate");
        RedPandaAI.updateActivity(this);
        profiler.pop();

        super.customServerAiStep();
    }

    @Override
    protected Brain.Provider<RedPandaEntity> brainProvider() {
        return RedPandaAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return RedPandaAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        var targetPlayer = this.findNearbyPlayer();
        var shouldStand = targetPlayer != null && !this.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);

        if (!this.level().isClientSide())
            this.updateStandState(shouldStand);

        if (this.getStandState() != StandState.ON_ALL_FOURS) {
            this.getNavigation().stop();
            this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            this.setJumping(false);

            this.zza = 0F;
            this.xxa = 0F;

            if (this.onGround())
                this.setDeltaMovement(0D, this.getDeltaMovement().y, 0D);

            if (targetPlayer != null) {
                this.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(targetPlayer, true));
                this.getLookControl().setLookAt(targetPlayer, 8F, 0F);
            } else
                this.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.RED_PANDA_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.RED_PANDA_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.RED_PANDA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.RED_PANDA_DEATH.get();
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.RED_PANDA.get().create(level);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        var result = super.hurt(source, amount);

        if (result) {
            if (this.getStandState() != StandState.ON_ALL_FOURS) {
                this.setStandState(StandState.DESCENDING);
                this.standTransitionTicks = STAND_TRANSITION_TICKS;
            }

            this.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);

            RedPandaAI.updateActivity(this);
        }

        return result;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(STAND_STATE, StandState.ON_ALL_FOURS.ordinal());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationState<RedPandaEntity> state) {
        var controller = state.getController();
        var idle = this.isBaby() ? BABY_IDLE : ADULT_IDLE;
        var walk = this.isBaby() ? BABY_WALK : ADULT_WALK;
        var ascend = this.isBaby() ? BABY_ASCEND : ADULT_ASCEND;
        var standing = this.isBaby() ? BABY_STANDING : ADULT_STANDING;
        var descend = this.isBaby() ? BABY_DESCEND : ADULT_DESCEND;

        switch (this.getStandState()) {
            case ASCENDING -> controller.setAnimation(ascend);
            case STANDING -> controller.setAnimation(standing);
            case DESCENDING -> controller.setAnimation(descend);
            case ON_ALL_FOURS -> {
                if (state.isMoving())
                    controller.setAnimation(walk);
                else
                    controller.setAnimation(idle);
            }
        }

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 8D);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("StandState", this.entityData.get(STAND_STATE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("StandState"))
            this.setStandState(StandState.byId(tag.getInt("StandState")));
    }

    private Player findNearbyPlayer() {
        return this.level().getNearestPlayer(this, STAND_PLAYER_RADIUS);
    }

    public StandState getStandState() {
        return StandState.byId(this.entityData.get(STAND_STATE));
    }

    public void setStandState(StandState state) {
        this.entityData.set(STAND_STATE, state.ordinal());
    }

    private void updateStandState(boolean shouldStand) {
        var standState = this.getStandState();

        if (shouldStand) {
            if (standState == StandState.ON_ALL_FOURS || standState == StandState.DESCENDING) {
                this.setStandState(StandState.ASCENDING);
                this.standTransitionTicks = STAND_TRANSITION_TICKS;
            }
        } else if (standState == StandState.STANDING || standState == StandState.ASCENDING) {
            this.setStandState(StandState.DESCENDING);
            this.standTransitionTicks = STAND_TRANSITION_TICKS;
        }

        standState = this.getStandState();

        if (standState == StandState.ASCENDING || standState == StandState.DESCENDING) {
            if (this.standTransitionTicks > 0)
                this.standTransitionTicks--;

            if (this.standTransitionTicks == 0) {
                if (standState == StandState.ASCENDING)
                    this.setStandState(StandState.STANDING);
                else
                    this.setStandState(StandState.ON_ALL_FOURS);
            }
        }
    }

    public enum StandState {
        ON_ALL_FOURS,
        ASCENDING,
        STANDING,
        DESCENDING;

        public static StandState byId(int id) {
            var values = values();

            if (id < 0 || id >= values.length)
                return ON_ALL_FOURS;

            return values[id];
        }
    }
}
