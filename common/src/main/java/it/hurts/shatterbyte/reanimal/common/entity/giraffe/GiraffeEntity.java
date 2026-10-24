package it.hurts.shatterbyte.reanimal.common.entity.giraffe;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.google.common.collect.ImmutableMap;
import it.hurts.shatterbyte.reanimal.common.entity.ostrich.OstrichAI;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
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

public class GiraffeEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.giraffe.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.giraffe.walk");

    private static final RawAnimation GRAZING = RawAnimation.begin().then("animation.giraffe.grazing", LoopType.PLAY_ONCE);
    private static final RawAnimation IDLE_GRAZE = RawAnimation.begin().thenLoop("animation.giraffe.idle_graze");
    private static final RawAnimation GETTING_UP = RawAnimation.begin().then("animation.giraffe.getting_up", LoopType.PLAY_ONCE);

    private static final EntityDataAccessor<Integer> GRAZE_STATE = SynchedEntityData.defineId(GiraffeEntity.class, EntityDataSerializers.INT);

    private static final int GRAZE_TRANSITION_TICKS = 15;

    public static final UniformInt GRAZE_DURATION = UniformInt.of(200, 400);
    public static final UniformInt GRAZE_COOLDOWN = UniformInt.of(200, 400);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public int grazeTime;
    public int grazeCooldown;

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
        var level = this.level();

        ((Brain<GiraffeEntity>) this.getBrain()).tick(serverLevel, this);

        GiraffeAI.updateActivity(this);

        super.customServerAiStep(serverLevel);
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
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.isGrazing()) {
            this.setXRot(0);
            this.xRotO = 0;

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
                if (GiraffeEntity.this.isGrazing())
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
        controllers.add(new AnimationController<>("main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationTest<GiraffeEntity> state) {
        var controller = state.controller();

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

        this.grazeTime = GRAZE_TRANSITION_TICKS;
    }

    public void startGettingUp() {
        this.setGrazeState(GrazeState.GETTING_UP);

        this.grazeTime = GRAZE_TRANSITION_TICKS;
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
