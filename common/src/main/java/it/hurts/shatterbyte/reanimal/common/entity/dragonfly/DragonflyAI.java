package it.hurts.shatterbyte.reanimal.common.entity.dragonfly;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.common.entity.BrainUtil;
import it.hurts.shatterbyte.reanimal.common.entity.PerchOnMob;
import it.hurts.shatterbyte.reanimal.common.entity.insect.RestOnSurface;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Optional;
import java.util.Set;

public class DragonflyAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super DragonflyEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
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
            MemoryModuleType.DANGER_DETECTED_RECENTLY,
            MemoryModuleType.ATTACK_TARGET,
            MemoryModuleType.ATTACK_COOLING_DOWN
    );

    public static Brain.Provider<DragonflyEntity> brainProvider() {
        return BrainUtil.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<DragonflyEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initPanicActivity(brain);
        initFightActivity(brain);
        BrainUtil.addActivity(brain, Activity.HIDE, 0, ImmutableList.of());

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<DragonflyEntity> brain) {
        BrainUtil.<DragonflyEntity>addActivity(brain,
                Activity.CORE,
                0,
                ImmutableList.of(
                        new Swim<>(0.8F),
                        new LookAtTargetSink(45, 90),
                        new MoveToTargetSink(),
                        new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
                        new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)
                )
        );
    }

    private static void initIdleActivity(Brain<DragonflyEntity> brain) {
        BrainUtil.<DragonflyEntity>addActivity(brain,
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, SetEntityLookTargetSometimes.create(net.minecraft.world.entity.EntityTypes.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(1, new AnimalMakeLove(ReAnimalEntities.DRAGONFLY, 1F, 1)),
                        Pair.of(2, StartAttacking.create((level, entity) -> canHunt(entity), DragonflyAI::findNearestPrey)),
                        Pair.of(
                                3,
                                new RunOne<>(
                                        ImmutableList.of(
                                                Pair.of(new FollowTemptation(entity -> 1.25F, entity -> entity.isBaby() ? 1D : 2D), 1),
                                                Pair.of(BabyFollowAdult.create(UniformInt.of(2, 4), 1.25F), 1)
                                        )
                                )
                        ),
                        Pair.of(4, new RandomLookAround(UniformInt.of(150, 250), 30F, 0F, 0F)),
                        Pair.of(
                                5,
                                new RunOne<>(
                                        ImmutableMap.of(
                                                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                                                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT
                                        ),
                                        ImmutableList.of(
                                                Pair.of(RandomStroll.fly(1F), 1),
                                                Pair.of(new RestOnSurface(), 1),
                                                Pair.of(new PerchOnMob(), 1),
                                                Pair.of(SetWalkTargetFromLookTarget.create(1F, 3), 1),
                                                Pair.of(new DoNothing(30, 60), 1)
                                        )
                                )
                        )
                )
        );
    }

    private static void initPanicActivity(Brain<DragonflyEntity> brain) {
        BrainUtil.<DragonflyEntity>addActivityAndRemoveMemoryWhenStopped(brain,
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

    public static void updateActivity(DragonflyEntity entity) {
        var brain = entity.getBrain();

        if (brain.getMemory(MemoryModuleType.ATTACK_TARGET)
                .filter(target -> !canHunt(entity) || !entity.closerThan(target, entity.getAttributeValue(Attributes.FOLLOW_RANGE))).isPresent()) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
            entity.getNavigation().stop();
            entity.getMoveControl().setWait();
        }

        if (entity.canFly())
            brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, Activity.FIGHT, Activity.IDLE));
        else
            brain.setActiveActivityIfPossible(Activity.HIDE);
    }

    private static void initFightActivity(Brain<DragonflyEntity> brain) {
        BrainUtil.<DragonflyEntity>addActivityAndRemoveMemoryWhenStopped(brain,
                Activity.FIGHT,
                10,
                ImmutableList.of(
                        StopAttackingIfTargetInvalid.create((level, target) -> target.isInWater()
                                || !target.getType().builtInRegistryHolder().is(ReAnimalTags.EntityTypes.DRAGONFLY_PREY)),
                        new ChasePrey(),
                        MeleeAttack.create(DragonflyAI::canHunt, 20)
                ),
                MemoryModuleType.ATTACK_TARGET
        );
    }

    private static boolean canHunt(DragonflyEntity entity) {
        return entity.canFly() && !entity.isBaby() && !entity.isPassenger() && !entity.isInWater() && !entity.isScared()
                && !entity.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
    }

    private static Optional<LivingEntity> findNearestPrey(ServerLevel level, DragonflyEntity entity) {
        return entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                .flatMap(visible -> visible.findClosest(target -> !target.isInWater()
                        && target.getType().builtInRegistryHolder().is(ReAnimalTags.EntityTypes.DRAGONFLY_PREY)
                        && Sensor.isEntityAttackable(level, entity, target)));
    }

    private static class ChasePrey extends Behavior<DragonflyEntity> {
        public ChasePrey() {
            super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), 200);
        }

        @Override
        protected void start(ServerLevel level, DragonflyEntity entity, long gameTime) {
            entity.stopResting();
            entity.getNavigation().stop();
            entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }

        @Override
        protected boolean canStillUse(ServerLevel level, DragonflyEntity entity, long gameTime) {
            return canHunt(entity) && entity.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
        }

        @Override
        protected void tick(ServerLevel level, DragonflyEntity entity, long gameTime) {
            var target = entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElseThrow();
            var position = target.getBoundingBox().getCenter().subtract(0D, entity.getBbHeight() * 0.5D, 0D);

            entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));

            if (level.noCollision(entity, entity.getBoundingBox().expandTowards(position.subtract(entity.position())))) {
                entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                entity.getNavigation().stop();
                entity.getMoveControl().setWantedPosition(position.x, position.y, position.z, 1.25D);
            } else
                entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(target, false), 1.25F, 0));
        }

        @Override
        protected void stop(ServerLevel level, DragonflyEntity entity, long gameTime) {
            entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            entity.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
            entity.getNavigation().stop();
            entity.getMoveControl().setWait();
        }
    }
}
