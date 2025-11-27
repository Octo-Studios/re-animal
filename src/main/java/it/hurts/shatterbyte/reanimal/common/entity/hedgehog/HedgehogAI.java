package it.hurts.shatterbyte.reanimal.common.entity.hedgehog;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class HedgehogAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super HedgehogEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, ReAnimalSensorTypes.HEDGEHOG_TEMPTATIONS.get(), SensorType.NEAREST_ADULT, ReAnimalSensorTypes.HEDGEHOG_SCARE_DETECTED.get()
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

    public static Brain.Provider<HedgehogEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    private static final OneShot<HedgehogEntity> HEDGEHOG_ROLLING_OUT = BehaviorBuilder.create(
            instance -> instance.group(instance.absent(MemoryModuleType.DANGER_DETECTED_RECENTLY))
                    .apply(instance, accessor -> (level, entity, id) -> {
                        if (entity.isScared()) {
                            entity.rollOut();

                            return true;
                        } else
                            return false;
                    })
    );

    public static Brain<?> makeBrain(Brain<HedgehogEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initScaredActivity(brain);

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<HedgehogEntity> brain) {
        brain.addActivity(
                Activity.CORE,
                0,
                ImmutableList.of(
                        new Swim(0.8F),
                        new HedgehogPanic(2F),
                        new LookAtTargetSink(45, 90),
                        new MoveToTargetSink() {
                            @Override
                            protected boolean checkExtraStartConditions(ServerLevel level, Mob mob) {
                                if (mob instanceof HedgehogEntity hedgehog && hedgehog.isScared())
                                    return false;

                                return super.checkExtraStartConditions(level, mob);
                            }
                        },
                        new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
                        new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS),
                        HEDGEHOG_ROLLING_OUT
                )
        );
    }

    private static void initIdleActivity(Brain<HedgehogEntity> brain) {
        brain.addActivity(
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, SetEntityLookTargetSometimes.create(EntityType.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(1, new AnimalMakeLove(ReAnimalEntities.HEDGEHOG.get(), 1F, 1)),
                        Pair.of(2, new HedgehogPickupItem()),
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

    private static void initScaredActivity(Brain<HedgehogEntity> brain) {
        brain.addActivityWithConditions(
                Activity.PANIC,
                ImmutableList.of(Pair.of(0, new HedgehogBallUp())),
                Set.of(
                        Pair.of(MemoryModuleType.DANGER_DETECTED_RECENTLY, MemoryStatus.VALUE_PRESENT),
                        Pair.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT)
                )
        );
    }

    public static void updateActivity(HedgehogEntity entity) {
        entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, Activity.IDLE));
    }

    public static Predicate<ItemStack> getTemptations() {
        return stack -> stack.is(Items.SPIDER_EYE);
    }

    public static class HedgehogBallUp extends Behavior<HedgehogEntity> {
        static final int BALL_UP_STAY_IN_STATE = 5 * TimeUtil.SECONDS_PER_MINUTE * 20;

        int nextPeekTimer = 0;
        boolean dangerWasAround;

        public HedgehogBallUp() {
            super(Map.of(), BALL_UP_STAY_IN_STATE);
        }

        protected void tick(ServerLevel level, HedgehogEntity entity, long id) {
            super.tick(level, entity, id);

            if (this.nextPeekTimer > 0)
                this.nextPeekTimer--;

            if (entity.getState() == HedgehogEntity.HedgehogState.ROLLING && entity.inStateTicks > (long) HedgehogEntity.HedgehogState.ROLLING.getAnimationDuration()) {
                entity.setState(HedgehogEntity.HedgehogState.SCARED);
            } else {
                HedgehogEntity.HedgehogState state = entity.getState();

                long i = entity.getBrain().getTimeUntilExpiry(MemoryModuleType.DANGER_DETECTED_RECENTLY);

                boolean flag = i > 75L;

                if (flag != this.dangerWasAround)
                    this.nextPeekTimer = this.pickNextPeekTimer(entity);

                this.dangerWasAround = flag;

                if (state == HedgehogEntity.HedgehogState.SCARED) {
                    if (this.nextPeekTimer == 0 && entity.onGround() && flag) {
                        level.broadcastEntityEvent(entity, (byte) 64);

                        this.nextPeekTimer = this.pickNextPeekTimer(entity);
                    }

                    if (i < (long) HedgehogEntity.HedgehogState.UNROLLING.getAnimationDuration()) {
                        entity.setState(HedgehogEntity.HedgehogState.UNROLLING);
                    }

                } else if (state == HedgehogEntity.HedgehogState.UNROLLING && i > (long) HedgehogEntity.HedgehogState.UNROLLING.getAnimationDuration())
                    entity.setState(HedgehogEntity.HedgehogState.SCARED);
            }
        }

        private int pickNextPeekTimer(HedgehogEntity entity) {
            return HedgehogEntity.HedgehogState.SCARED.getAnimationDuration() + entity.getRandom().nextIntBetweenInclusive(100, 400);
        }

        protected boolean checkExtraStartConditions(ServerLevel level, HedgehogEntity entity) {
            return entity.onGround();
        }

        protected boolean canStillUse(ServerLevel level, HedgehogEntity entity, long id) {
            return entity.getState().isThreatened();
        }

        protected void start(ServerLevel level, HedgehogEntity entity, long id) {
            entity.rollUp();
        }

        protected void stop(ServerLevel level, HedgehogEntity entity, long id) {
            if (!entity.canStayRolledUp())
                entity.rollOut();
        }
    }

    public static class HedgehogPanic extends AnimalPanic<HedgehogEntity> {
        public HedgehogPanic(float speedMultiplier) {
            super(speedMultiplier, mob -> DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES);
        }

        protected void start(ServerLevel level, HedgehogEntity entity, long id) {
            entity.rollOut();

            super.start(level, entity, id);
        }
    }

    public static class HedgehogPickupItem extends Behavior<HedgehogEntity> {
        private static final int SEARCH_RADIUS = 8;
        private static final int MAX_DURATION = 200;

        public HedgehogPickupItem() {
            super(Map.of(
                    MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED
            ), MAX_DURATION);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, HedgehogEntity entity) {
            if (entity.isBaby())
                return false;

            if (!entity.getStack().isEmpty())
                return false;

            ItemEntity nearest = findNearestItem(level, entity);

            if (nearest == null)
                return false;

            entity.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(new EntityTracker(nearest, false), 1.1F, 0)
            );

            return true;
        }

        @Override
        protected boolean canStillUse(ServerLevel level, HedgehogEntity entity, long gameTime) {
            if (entity.isBaby() || !entity.getStack().isEmpty())
                return false;

            var opt = entity.getBrain().getMemory(MemoryModuleType.WALK_TARGET);

            if (opt.isEmpty())
                return false;

            var tracker = opt.get().getTarget();

            if (tracker instanceof EntityTracker entityTracker) {
                var target = entityTracker.getEntity();

                if (!target.isAlive() || !(target instanceof ItemEntity)) {
                    entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

                    return false;
                }
            }

            return true;
        }

        @Override
        protected void tick(ServerLevel level, HedgehogEntity entity, long gameTime) {
            super.tick(level, entity, gameTime);

            var opt = entity.getBrain().getMemory(MemoryModuleType.WALK_TARGET);

            if (opt.isEmpty())
                return;

            PositionTracker tracker = opt.get().getTarget();

            if (!(tracker instanceof EntityTracker entityTracker))
                return;

            var target = entityTracker.getEntity();

            if (!target.isAlive() || !(target instanceof ItemEntity item)) {
                return;
                }

            if (entity.position().distanceTo(item.position()) <= 1.5F) {
                if (entity.getStack().isEmpty()) {
                    entity.setStack(item.getItem().copy());

                    item.discard();
                }

                entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            }
        }

        @Override
        protected void stop(ServerLevel level, HedgehogEntity entity, long gameTime) {
            entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }

        private ItemEntity findNearestItem(ServerLevel level, HedgehogEntity entity) {
            var list = level.getEntitiesOfClass(
                    ItemEntity.class,
                    entity.getBoundingBox().inflate(SEARCH_RADIUS),
                    item -> item.isAlive()
                            && !item.getItem().isEmpty()
                            && !item.hasPickUpDelay()
            );

            if (list.isEmpty())
                return null;

            ItemEntity nearest = list.getFirst();

            double bestDist = entity.distanceToSqr(nearest);

            for (ItemEntity other : list) {
                double d = entity.distanceToSqr(other);

                if (d < bestDist) {
                    bestDist = d;
                    nearest = other;
                }
            }

            return nearest;
        }
    }
}