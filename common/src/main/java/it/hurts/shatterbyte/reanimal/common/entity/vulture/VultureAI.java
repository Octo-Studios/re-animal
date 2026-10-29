package it.hurts.shatterbyte.reanimal.common.entity.vulture;

import it.hurts.shatterbyte.reanimal.common.entity.BrainUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.function.Predicate;

public class VultureAI {
    private static final double MAX_PREY_DISTANCE_SQR = 64D * 64D;

    private static final ImmutableList<SensorType<? extends Sensor<? super VultureEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            ReAnimalSensorTypes.VULTURE_TEMPTATIONS,
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

    public static Brain.Provider<VultureEntity> brainProvider() {
        return BrainUtil.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<VultureEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<VultureEntity> brain) {
        BrainUtil.<VultureEntity>addActivity(brain,
                Activity.CORE,
                0,
                ImmutableList.of(
                        new net.minecraft.world.entity.ai.behavior.Swim<>(0.8F),
                        new LookAtTargetSink(45, 90),
                        new MoveToTargetSink(),
                        new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
                        new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)
                )
        );
    }

    private static void initIdleActivity(Brain<VultureEntity> brain) {
        BrainUtil.<VultureEntity>addActivity(brain,
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, SetEntityLookTargetSometimes.create(net.minecraft.world.entity.EntityTypes.PLAYER, 8F, UniformInt.of(30, 60))),
                        Pair.of(1, new AnimalMakeLove(ReAnimalEntities.VULTURE, 1F, 1)),
                        Pair.of(
                                2,
                                new RunOne<>(
                                        ImmutableList.of(
                                                Pair.of(new FollowTemptation(e -> 1.2F, e -> 1.2D), 1),
                                                Pair.of(BabyFollowAdult.create(UniformInt.of(2, 4), 1.1F), 1)
                                        )
                                )
                        ),
                        Pair.of(3, new VultureRetaliateTask()),
                        Pair.of(4, new VultureHuntWoundedTask()),
                        Pair.of(5, new VultureChaseAndAttackTask()),
                        Pair.of(6, new VultureEatGroundMeatTask()),
                        Pair.of(7, new RandomLookAround(UniformInt.of(150, 250), 30F, 0F, 0F)),
                        Pair.of(
                                8,
                                new RunOne<>(
                                        ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                                        ImmutableList.of(
                                                Pair.of(RandomStroll.stroll(1.1F), 1),
                                                Pair.of(SetWalkTargetFromLookTarget.create(1.1F, 3), 1),
                                                Pair.of(new DoNothing(80, 160), 6)
                                        )
                                )
                        )
                )
        );
    }

    public static void updateActivity(VultureEntity entity) {
        entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.IDLE));
    }

    public static Predicate<ItemStack> getTemptations() {
        return stack -> stack.is(ReAnimalTags.Items.VULTURE_FOOD);
    }

    private static boolean isValidPrey(VultureEntity vulture, LivingEntity prey) {
        return prey.isAlive()
                && !prey.isInvulnerable()
                && !prey.isInWater()
                && !(prey instanceof VultureEntity)
                && !(prey instanceof Player player && (player.isCreative() || player.isSpectator()))
                && vulture.distanceToSqr(prey) <= MAX_PREY_DISTANCE_SQR;
    }

    static class VultureRetaliateTask extends Behavior<VultureEntity> {
        VultureRetaliateTask() {
            super(ImmutableMap.of(
                    MemoryModuleType.HURT_BY_ENTITY, MemoryStatus.VALUE_PRESENT
            ));
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, VultureEntity entity) {
            return entity.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY)
                    .filter(attacker -> isValidPrey(entity, attacker))
                    .isPresent();
        }

        @Override
        protected void start(ServerLevel level, VultureEntity entity, long gameTime) {
            entity.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY)
                    .filter(attacker -> isValidPrey(entity, attacker))
                    .ifPresent(entity::setTarget);

            entity.getBrain().eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
            entity.getBrain().eraseMemory(MemoryModuleType.HURT_BY);
        }
    }

    static class VultureHuntWoundedTask extends Behavior<VultureEntity> {
        private static final double SEARCH_RADIUS_SQR = 16D * 16D;
        private static final float HEALTH_RATIO_THRESHOLD = 0.25F;

        VultureHuntWoundedTask() {
            super(ImmutableMap.of(
                    MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
            ));
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, VultureEntity entity) {
            return !entity.isBaby() && entity.getTarget() == null && level.getGameTime() % 20L == 0L;
        }

        @Override
        protected void start(ServerLevel level, VultureEntity entity, long gameTime) {
            entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                    .flatMap(visible -> visible.findClosest(candidate -> isWounded(entity, candidate)))
                    .ifPresent(entity::setTarget);
        }

        private static boolean isWounded(VultureEntity entity, LivingEntity candidate) {
            return entity.distanceToSqr(candidate) <= SEARCH_RADIUS_SQR
                    && isValidPrey(entity, candidate)
                    && candidate.getHealth() / candidate.getMaxHealth() < HEALTH_RATIO_THRESHOLD;
        }
    }

    static class VultureChaseAndAttackTask extends Behavior<VultureEntity> {
        VultureChaseAndAttackTask() {
            super(ImmutableMap.of());
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, VultureEntity entity) {
            var target = entity.getTarget();

            return target != null && isValidPrey(entity, target);
        }

        @Override
        protected boolean canStillUse(ServerLevel level, VultureEntity entity, long gameTime) {
            var target = entity.getTarget();

            return target != null && isValidPrey(entity, target);
        }

        @Override
        protected void tick(ServerLevel level, VultureEntity entity, long gameTime) {
            var target = entity.getTarget();

            if (target == null || !isValidPrey(entity, target)) {
                entity.setTarget(null);
                entity.getNavigation().stop();
                return;
            }

            entity.getNavigation().moveTo(target, 1.4D);

            if (entity.distanceToSqr(target) <= 4.0D && gameTime % 20L == 0L)
                entity.doHurtTarget(level, target);
        }

        @Override
        protected void stop(ServerLevel level, VultureEntity entity, long gameTime) {
            var target = entity.getTarget();

            if (target == null || !isValidPrey(entity, target))
                entity.setTarget(null);

            entity.getNavigation().stop();
        }
    }

    static class VultureEatGroundMeatTask extends Behavior<VultureEntity> {
        private static final double SEARCH_RADIUS = 16.0D;
        private ItemEntity targetItem;

        VultureEatGroundMeatTask() {
            super(ImmutableMap.of());
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, VultureEntity entity) {
            if (entity.getTarget() != null)
                return false;

            if (!entity.getNavigation().isDone())
                return false;

            var box = entity.getBoundingBox().inflate(SEARCH_RADIUS);
            var list = level.getEntitiesOfClass(ItemEntity.class, box, item -> {
                var stack = item.getItem();
                return !item.isRemoved()
                        && stack.is(ReAnimalTags.Items.VULTURE_FOOD);
            });

            if (list.isEmpty())
                return false;

            list.sort((a, b) -> {
                var da = entity.distanceToSqr(a);
                var db = entity.distanceToSqr(b);
                return Double.compare(da, db);
            });

            targetItem = list.getFirst();
            return true;
        }

        @Override
        protected boolean canStillUse(ServerLevel level, VultureEntity entity, long gameTime) {
            if (targetItem == null || targetItem.isRemoved())
                return false;

            if (entity.getTarget() != null)
                return false;

            var maxDistSqr = SEARCH_RADIUS * SEARCH_RADIUS;

            if (entity.distanceToSqr(targetItem) > maxDistSqr)
                return false;

            return true;
        }

        @Override
        protected void start(ServerLevel level, VultureEntity entity, long gameTime) {
            if (targetItem == null || targetItem.isRemoved())
                return;

            entity.getNavigation().moveTo(targetItem, 1.2D);
        }

        @Override
        protected void tick(ServerLevel level, VultureEntity entity, long gameTime) {
            if (targetItem == null || targetItem.isRemoved()) {
                entity.getNavigation().stop();
                return;
            }

            entity.getNavigation().moveTo(targetItem, 1.2D);

            var distanceSqr = entity.distanceToSqr(targetItem);

            if (distanceSqr <= 2.0D) {
                var stack = targetItem.getItem();

                if (!stack.isEmpty() && stack.is(ReAnimalTags.Items.VULTURE_FOOD)) {
                    stack.shrink(1);

                    if (stack.isEmpty())
                        targetItem.discard();
                }

                entity.getNavigation().stop();
                targetItem = null;
            }
        }

        @Override
        protected void stop(ServerLevel level, VultureEntity entity, long gameTime) {
            entity.getNavigation().stop();

            targetItem = null;
        }
    }
}