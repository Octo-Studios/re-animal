package it.hurts.shatterbyte.reanimal.common.entity.meerkat;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;

public class MeerkatEntity extends Animal implements GeoEntity {
    private static final RawAnimation ADULT_IDLE = RawAnimation.begin().thenLoop("animation.meerkat.idle");
    private static final RawAnimation ADULT_WALK = RawAnimation.begin().thenLoop("animation.meerkat.walk");
    private static final RawAnimation ADULT_ASCEND = RawAnimation.begin().then("animation.meerkat.ascend", LoopType.PLAY_ONCE);
    private static final RawAnimation ADULT_STANDING = RawAnimation.begin().thenLoop("animation.meerkat.standing");
    private static final RawAnimation ADULT_DESCEND = RawAnimation.begin().then("animation.meerkat.descend", LoopType.PLAY_ONCE);

    private static final RawAnimation BABY_IDLE = RawAnimation.begin().thenLoop("animation.meerkat_baby.idle");
    private static final RawAnimation BABY_WALK = RawAnimation.begin().thenLoop("animation.meerkat_baby.walk");
    private static final RawAnimation BABY_ASCEND = RawAnimation.begin().then("animation.meerkat_baby.ascend", LoopType.PLAY_ONCE);
    private static final RawAnimation BABY_STANDING = RawAnimation.begin().thenLoop("animation.meerkat_baby.standing");
    private static final RawAnimation BABY_DESCEND = RawAnimation.begin().then("animation.meerkat_baby.descend", LoopType.PLAY_ONCE);

    private static final EntityDataAccessor<Integer> STAND_STATE = SynchedEntityData.defineId(MeerkatEntity.class, EntityDataSerializers.INT);

    private static final int STAND_TRANSITION_TICKS = 10;
    private static final double STAND_PLAYER_RADIUS = 6D;
    private static final int STAND_DURATION_MIN = 60;
    private static final int STAND_DURATION_MAX = 160;
    private static final int STAND_COOLDOWN_MIN = 200;
    private static final int STAND_COOLDOWN_MAX = 600;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int standTransitionTicks;
    private int standTicks;
    private int standCooldownTicks;

    public MeerkatEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.getNavigation().setCanFloat(true);

        this.standCooldownTicks = Mth.randomBetweenInclusive(this.random, STAND_COOLDOWN_MIN, STAND_COOLDOWN_MAX);
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        var level = this.level();

        ((Brain<MeerkatEntity>) this.getBrain()).tick(serverLevel, this);

        MeerkatAI.updateActivity(this);

        super.customServerAiStep(serverLevel);
    }
    protected Brain.Provider<MeerkatEntity> brainProvider() {
        return MeerkatAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return MeerkatAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        var targetPlayer = this.findNearbyPlayer();

        if (!this.level().isClientSide())
            this.updateStandState(this.shouldStand());

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
        return stack.is(ReAnimalTags.Items.MEERKAT_FOOD);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.MEERKAT.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result) {
            if (this.getStandState() != StandState.ON_ALL_FOURS) {
                this.setStandState(StandState.DESCENDING);
                this.standTransitionTicks = STAND_TRANSITION_TICKS;
            }

            this.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);

            MeerkatAI.updateActivity(this);
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
        controllers.add(new AnimationController<>("main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationTest<MeerkatEntity> state) {
        var controller = state.controller();
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
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 8D);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("StandState", this.entityData.get(STAND_STATE));
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.setStandState(StandState.byId(tag.getIntOr("StandState", 0)));
    }

    private boolean shouldStand() {
        if (this.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING) || !this.onGround() || this.isInWater()) {
            this.standTicks = 0;

            return false;
        }

        if (this.standTicks > 0) {
            this.standTicks--;

            if (this.standTicks == 0)
                this.standCooldownTicks = Mth.randomBetweenInclusive(this.random, STAND_COOLDOWN_MIN, STAND_COOLDOWN_MAX);

            return this.standTicks > 0;
        }

        if (this.standCooldownTicks > 0) {
            this.standCooldownTicks--;

            return false;
        }

        if (this.getStandState() != StandState.ON_ALL_FOURS)
            return false;

        this.standTicks = Mth.randomBetweenInclusive(this.random, STAND_DURATION_MIN, STAND_DURATION_MAX);

        return true;
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
