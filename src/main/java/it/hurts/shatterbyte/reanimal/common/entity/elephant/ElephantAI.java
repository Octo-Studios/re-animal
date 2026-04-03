package it.hurts.shatterbyte.reanimal.common.entity.elephant;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Set;

public class ElephantAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super ElephantEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            ReAnimalSensorTypes.ELEPHANT_TEMPTATIONS.get(),
            SensorType.NEAREST_ADULT
    );

    private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
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
            MemoryModuleType.ATTACK_TARGET,
            MemoryModuleType.ATTACK_COOLING_DOWN
    );

    public static Brain.Provider<ElephantEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<ElephantEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initFightActivity(brain); // Aggressive retaliation

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<ElephantEntity> brain) {
        brain.addActivity(
                Activity.CORE,
                0,
                ImmutableList.of(
                        new Swim(0.8F),
                        new LookAtTargetSink(45, 90),
                        new MoveToTargetSink(),
                        new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
                        new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)
                )
        );
    }

    private static void initIdleActivity(Brain<ElephantEntity> brain) {
        brain.addActivity(
                Activity.IDLE,
                ImmutableList.of(
                        StartAttacking.create(e -> true, (elephant) -> elephant.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY)),
                        Pair.of(0, SetEntityLookTargetSometimes.create(EntityType.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(1, new AnimalMakeLove(ReAnimalEntities.ELEPHANT.get(), 1.0F, 1)),
                        Pair.of(2, new RunOne<>(
                                ImmutableList.of(
                                        Pair.of(new FollowTemptation(entity -> 1.1F, entity -> entity.isBaby() ? 1D : 2D), 1),
                                        Pair.of(BabyFollowAdult.create(UniformInt.of(2, 4), 1.1F), 1)
                                )
                        )),
                        Pair.of(3, new RandomLookAround(UniformInt.of(150, 250), 30F, 0F, 0F)),
                        Pair.of(4, new RunOne<>(
                                ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                                ImmutableList.of(
                                        Pair.of(RandomStroll.stroll(0.8F), 2),
                                        Pair.of(SetWalkTargetFromLookTarget.create(0.8F, 3), 2),
                                        Pair.of(new DoNothing(30, 60), 1)
                                )
                        ))
                )
        );
    }

    private static void initFightActivity(Brain<ElephantEntity> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(
                Activity.FIGHT,
                10,
                ImmutableList.of(
                        StopAttackingIfTargetInvalid.create(),
                        SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.2F),
                        MeleeAttack.create(20)
                ),
                MemoryModuleType.ATTACK_TARGET
        );
    }

    public static void updateActivity(ElephantEntity entity) {
        if (entity.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
        } else {
            entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.IDLE));
        }
    }
}
