package it.hurts.shatterbyte.reanimal.common.entity.giraffe;

import com.mojang.serialization.Dynamic;
import it.hurts.shatterbyte.reanimal.common.entity.hippopotamus.HippopotamusEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GiraffeEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.giraffe.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.giraffe.walk");

    private static final RawAnimation GRAZING = RawAnimation.begin().then("animation.giraffe.grazing", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation IDLE_GRAZE = RawAnimation.begin().thenLoop("animation.giraffe.idle_graze");
    private static final RawAnimation GETTING_UP = RawAnimation.begin().then("animation.giraffe.getting_up", Animation.LoopType.PLAY_ONCE);

    private static final EntityDataAccessor<Integer> GRAZE_STATE = SynchedEntityData.defineId(GiraffeEntity.class, EntityDataSerializers.INT);

    private static final int GRAZE_TRANSITION_TICKS = 15;

    private static final UniformInt GRAZE_DURATION = UniformInt.of(100, 200);
    private static final UniformInt GRAZE_COOLDOWN = UniformInt.of(200, 400);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int grazeTime;
    private int grazeCooldown;

    public GiraffeEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    public boolean isGrazing() {
        var state = this.getGrazeState();

        return state == GrazeState.GRAZING_DOWN || state == GrazeState.GRAZING || state == GrazeState.GETTING_UP;
    }

    private GrazeState getGrazeState() {
        return GrazeState.byId(this.entityData.get(GRAZE_STATE));
    }

    private void setGrazeState(GrazeState state) {
        this.entityData.set(GRAZE_STATE, state.ordinal());
    }

    @Override
    protected void customServerAiStep() {
        var level = this.level();
        var profiler = level.getProfiler();

        profiler.push("giraffeBrain");
        ((Brain<GiraffeEntity>) this.getBrain()).tick((ServerLevel) this.level(), this);
        profiler.pop();

        profiler.push("giraffeActivityUpdate");
        GiraffeAI.updateActivity(this);
        profiler.pop();

        profiler.push("giraffeGrazeBehavior");
        this.tickGrazeBehavior();
        profiler.pop();

        super.customServerAiStep();
    }

    @Override
    protected Brain.Provider<GiraffeEntity> brainProvider() {
        return GiraffeAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return GiraffeAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.GIRAFFE_FOOD);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.GIRAFFE.get().create(level);

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

        if (!this.level().isClientSide && this.isGrazing())
            this.getNavigation().stop();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("GrazeState", this.entityData.get(GRAZE_STATE));
        tag.putInt("GrazeTime", this.grazeTime);
        tag.putInt("GrazeCooldown", this.grazeCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("GrazeState"))
            this.setGrazeState(GrazeState.byId(tag.getInt("GrazeState")));

        if (tag.contains("GrazeTime"))
            this.grazeTime = tag.getInt("GrazeTime");

        if (tag.contains("GrazeCooldown"))
            this.grazeCooldown = tag.getInt("GrazeCooldown");
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
        controllers.add(new AnimationController<>(this, "main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationState<GiraffeEntity> state) {
        var controller = state.getController();

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
                .add(Attributes.MAX_HEALTH, 100D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 8D)
                .add(Attributes.STEP_HEIGHT, 1.1D);
    }

    private void tickGrazeBehavior() {
        if (this.level().isClientSide)
            return;

        if (this.grazeCooldown > 0)
            this.grazeCooldown--;

        var brain = this.getBrain();
        var grazeState = this.getGrazeState();

        var shouldAbort = brain.hasMemoryValue(MemoryModuleType.IS_PANICKING)
                || brain.hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER)
                || brain.hasMemoryValue(MemoryModuleType.IS_TEMPTED)
                || this.isInWaterOrBubble();

        if ((grazeState == GrazeState.GRAZING_DOWN || grazeState == GrazeState.GRAZING) && shouldAbort) {
            this.startGettingUp();
            return;
        }

        switch (grazeState) {
            case STANDING -> {
                if (this.grazeCooldown == 0 && this.canStartGrazing())
                    this.startGrazingDown();
            }
            case GRAZING_DOWN -> {
                if (--this.grazeTime <= 0) {
                    this.grazeTime = GRAZE_DURATION.sample(this.random);
                    this.setGrazeState(GrazeState.GRAZING);
                }
            }
            case GRAZING -> {
                this.getNavigation().stop();

                if (--this.grazeTime <= 0)
                    this.startGettingUp();
            }
            case GETTING_UP -> {
                if (--this.grazeTime <= 0)
                    this.finishGettingUp();
            }
        }
    }

    private boolean canStartGrazing() {
        return !this.isVehicle()
                && !this.isInWaterOrBubble()
                && !this.getNavigation().isInProgress()
                && this.getDeltaMovement().horizontalDistanceSqr() < 0.0001D
                && !this.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING)
                && !this.getBrain().hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER)
                && !this.getBrain().hasMemoryValue(MemoryModuleType.IS_TEMPTED)
                && this.random.nextInt(1) == 0;
    }

    private void startGrazingDown() {
        this.getNavigation().stop();

        this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        this.setGrazeState(GrazeState.GRAZING_DOWN);

        this.grazeTime = GRAZE_TRANSITION_TICKS;
    }

    private void startGettingUp() {
        this.setGrazeState(GrazeState.GETTING_UP);

        this.grazeTime = GRAZE_TRANSITION_TICKS;
    }

    private void finishGettingUp() {
        this.setGrazeState(GrazeState.STANDING);

        this.grazeCooldown = GRAZE_COOLDOWN.sample(this.random);
    }

    private enum GrazeState {
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
