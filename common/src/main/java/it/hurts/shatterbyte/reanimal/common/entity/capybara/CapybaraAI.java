package it.hurts.shatterbyte.reanimal.common.entity.capybara;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.common.entity.BrainUtil;
import it.hurts.shatterbyte.reanimal.common.entity.SeekShallowWater;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.function.Predicate;

public class CapybaraAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super CapybaraEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            ReAnimalSensorTypes.CAPYBARA_TEMPTATIONS,
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
            MemoryModuleType.BREED_TARGET,
            MemoryModuleType.NEAREST_VISIBLE_ADULT,
            MemoryModuleType.DANGER_DETECTED_RECENTLY
    );

    public static Brain.Provider<CapybaraEntity> brainProvider() {
        return BrainUtil.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    private static OneShot<CapybaraEntity> rideAdult() {
        return BehaviorBuilder.create(instance -> instance.group(
                instance.present(MemoryModuleType.NEAREST_VISIBLE_ADULT),
                instance.registered(MemoryModuleType.WALK_TARGET),
                instance.registered(MemoryModuleType.LOOK_TARGET),
                instance.absent(MemoryModuleType.TEMPTING_PLAYER)
        ).apply(instance, (nearestAdult, walkTarget, lookTarget, temptingPlayer) -> (level, entity, timestamp) -> {
            if (entity.tickCount % 20 != 0 || !(instance.get(nearestAdult) instanceof CapybaraEntity adult)
                    || !entity.canRideAdult(adult) || !entity.closerThan(adult, 4D))
                return false;

            if (entity.getBoundingBox().inflate(0.25D).intersects(adult.getBoundingBox())) {
                if (!entity.startRiding(adult))
                    return false;

                entity.getNavigation().stop();
                walkTarget.erase();
                lookTarget.erase();
                entity.getBrain().eraseMemory(MemoryModuleType.PATH);
            } else {
                lookTarget.set(new EntityTracker(adult, true));
                walkTarget.set(new WalkTarget(new EntityTracker(adult, false), 1.25F, 0));
            }

            return true;
        }));
    }

    public static Brain<?> makeBrain(Brain<CapybaraEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initPanicActivity(brain);

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<CapybaraEntity> brain) {
        BrainUtil.<CapybaraEntity>addActivity(brain,
                Activity.CORE,
                0,
                ImmutableList.of(
                        new Swim<>(0.8F),
                        new LookAtTargetSink(45, 90),
                        new MoveToTargetSink(600, 800),
                        new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
                        new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)
                )
        );
    }

    private static void initIdleActivity(Brain<CapybaraEntity> brain) {
        BrainUtil.<CapybaraEntity>addActivity(brain,
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, SetEntityLookTargetSometimes.create(net.minecraft.world.entity.EntityTypes.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(1, new AnimalMakeLove(ReAnimalEntities.CAPYBARA, 1F, 1)),
                        Pair.of(2, rideAdult()),
                        Pair.of(
                                2,
                                new RunOne<>(
                                        ImmutableList.of(
                                                Pair.of(new FollowTemptation(entity -> 1.25F, entity -> entity.isBaby() ? 1D : 2D), 1),
                                                Pair.of(BabyFollowAdult.create(UniformInt.of(3, 6), 1.25F), 1)
                                        )
                                )
                        ),
                        Pair.of(3, new RandomLookAround(UniformInt.of(150, 250), 30F, 0F, 0F)),
                        Pair.of(
                                4,
                                new RunOne<>(
                                        ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                                        ImmutableList.of(
                                                Pair.of(new SeekShallowWater<CapybaraEntity>(entity -> entity.getSitState() == CapybaraEntity.SitState.STANDING), 1),
                                                Pair.of(RandomStroll.stroll(1F), 1),
                                                Pair.of(SetWalkTargetFromLookTarget.create(1F, 3), 1),
                                                Pair.of(new DoNothing(30, 60), 1)
                                        )
                                )
                        )
                )
        );
    }

    private static void initPanicActivity(Brain<CapybaraEntity> brain) {
        BrainUtil.<CapybaraEntity>addActivityAndRemoveMemoryWhenStopped(brain,
                Activity.PANIC,
                10,
                ImmutableList.of(
                        new AnimalPanic<>(
                                1.5F,
                                mob -> DamageTypeTags.PANIC_CAUSES
                        )
                ),
                MemoryModuleType.IS_PANICKING
        );
    }

    public static void updateActivity(CapybaraEntity entity) {
        entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, Activity.IDLE));
    }

    public static Predicate<ItemStack> getTemptations() {
        return stack -> stack.is(ReAnimalTags.Items.CAPYBARA_FOOD);
    }
}