package it.hurts.shatterbyte.reanimal.common.entity.giraffe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.function.Predicate;

public class GiraffeAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super GiraffeEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            ReAnimalSensorTypes.GIRAFFE_TEMPTATIONS.get(),
            SensorType.NEAREST_ADULT
    );

    private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
            MemoryModuleType.IS_PANICKING,
            MemoryModuleType.HURT_BY,
            MemoryModuleType.HURT_BY_ENTITY,
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryModuleType.PATH,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryModuleType.TEMPTING_PLAYER,
            MemoryModuleType.TEMPTATION_COOLDOWN_TICKS,
            MemoryModuleType.GAZE_COOLDOWN_TICKS,
            MemoryModuleType.IS_TEMPTED,
            MemoryModuleType.BREED_TARGET,
            MemoryModuleType.NEAREST_VISIBLE_ADULT,
            MemoryModuleType.DANGER_DETECTED_RECENTLY
    );

    public static Brain.Provider<GiraffeEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<GiraffeEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initPanicActivity(brain);

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<GiraffeEntity> brain) {
        brain.addActivity(
                Activity.CORE,
                0,
                ImmutableList.of(
                        new GrazeCooldownBehavior(),
                        new Swim(0.8F),
                        new LookAtTargetSink(45, 90),
                        new MoveToTargetSink(),
                        new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
                        new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)
                )
        );
    }

    private static void initIdleActivity(Brain<GiraffeEntity> brain) {
        brain.addActivity(
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, new GrazeBehavior()),
                        Pair.of(1, SetEntityLookTargetSometimes.create(EntityType.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(2, new AnimalMakeLove(ReAnimalEntities.GIRAFFE.get(), 1F, 1)),
                        Pair.of(
                                3,
                                new RunOne<>(
                                        ImmutableList.of(
                                                Pair.of(new FollowTemptation(entity -> 1.25F, entity -> entity.isBaby() ? 1D : 2D), 1),
                                                Pair.of(BabyFollowAdult.create(UniformInt.of(2, 5), 1.25F), 1)
                                        )
                                )
                        ),
                        Pair.of(4, new RandomLookAround(UniformInt.of(150, 250), 30F, 0F, 0F)),
                        Pair.of(
                                5,
                                new RunOne<>(
                                        ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                                        ImmutableList.of(
                                                Pair.of(RandomStroll.stroll(1F), 1),
                                                Pair.of(SetWalkTargetFromLookTarget.create(1F, 3), 1),
                                                Pair.of(new DoNothing(30, 60), 1)
                                        )
                                )
                        )
                )
        );
    }

    private static void initPanicActivity(Brain<GiraffeEntity> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(
                Activity.PANIC,
                10,
                ImmutableList.of(
                        new AnimalPanic<>(
                                2.5F,
                                mob -> DamageTypeTags.PANIC_CAUSES
                        )
                ),
                MemoryModuleType.IS_PANICKING
        );
    }

    public static void updateActivity(GiraffeEntity entity) {
        entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, Activity.IDLE));
    }

    public static Predicate<ItemStack> getTemptations() {
        return stack -> stack.is(ReAnimalTags.Items.GIRAFFE_FOOD);
    }

    public static class GrazeBehavior extends Behavior<GiraffeEntity> {
        public GrazeBehavior() {
            super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT), 1200);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, GiraffeEntity entity) {
            return entity.grazeCooldown == 0 && entity.canStartGrazing();
        }

        @Override
        protected void start(ServerLevel level, GiraffeEntity entity, long gameTime) {
            entity.startGrazingDown();
        }

        @Override
        protected boolean canStillUse(ServerLevel level, GiraffeEntity entity, long gameTime) {
            return entity.getGrazeState() != GiraffeEntity.GrazeState.STANDING;
        }

        @Override
        protected void tick(ServerLevel level, GiraffeEntity entity, long gameTime) {
            var brain = entity.getBrain();
            var grazeState = entity.getGrazeState();

            var shouldAbort = brain.hasMemoryValue(MemoryModuleType.IS_PANICKING)
                    || brain.hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER)
                    || brain.hasMemoryValue(MemoryModuleType.IS_TEMPTED)
                    || entity.isInWaterOrBubble();

            if ((grazeState == GiraffeEntity.GrazeState.GRAZING_DOWN || grazeState == GiraffeEntity.GrazeState.GRAZING) && shouldAbort) {
                entity.startGettingUp();
                grazeState = entity.getGrazeState();
            }

            switch (grazeState) {
                case GRAZING_DOWN -> {
                    if (--entity.grazeTime <= 0) {
                        entity.grazeTime = GiraffeEntity.GRAZE_DURATION.sample(entity.getRandom());
                        entity.setGrazeState(GiraffeEntity.GrazeState.GRAZING);
                    }
                }
                case GRAZING -> {
                    entity.getNavigation().stop();

                    if (--entity.grazeTime <= 0)
                        entity.startGettingUp();
                }
                case GETTING_UP -> {
                    if (--entity.grazeTime <= 0)
                        entity.finishGettingUp();
                }
                default -> {
                }
            }
        }

        @Override
        protected void stop(ServerLevel level, GiraffeEntity entity, long gameTime) {
            if (entity.getGrazeState() != GiraffeEntity.GrazeState.STANDING)
                entity.finishGettingUp();
        }
    }

    public static class GrazeCooldownBehavior extends Behavior<GiraffeEntity> {
        public GrazeCooldownBehavior() {
            super(ImmutableMap.of(), 1200);
        }

        @Override
        protected boolean canStillUse(ServerLevel level, GiraffeEntity entity, long gameTime) {
            return true;
        }

        @Override
        protected void tick(ServerLevel level, GiraffeEntity entity, long gameTime) {
            if (entity.grazeCooldown > 0)
                entity.grazeCooldown--;
        }
    }
}
