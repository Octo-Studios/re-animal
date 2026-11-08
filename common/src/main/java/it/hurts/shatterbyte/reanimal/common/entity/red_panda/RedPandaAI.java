package it.hurts.shatterbyte.reanimal.common.entity.red_panda;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.common.entity.BrainUtil;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class RedPandaAI {
    private static final int TREE_SEARCH_RADIUS = 6;
    private static final int TREE_SEARCH_HEIGHT = 12;

    private static final ImmutableList<SensorType<? extends Sensor<? super RedPandaEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            ReAnimalSensorTypes.RED_PANDA_TEMPTATIONS,
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

    public static Brain.Provider<RedPandaEntity> brainProvider() {
        return BrainUtil.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<RedPandaEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initPanicActivity(brain);

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<RedPandaEntity> brain) {
        BrainUtil.<RedPandaEntity>addActivity(brain,
                Activity.CORE,
                0,
                ImmutableList.of(
                        new Swim<>(0.8F),
                        new LookAtTargetSink(45, 90),
                        new MoveToTargetSink() {
                            private RedPandaNavigation navigation;

                            @Override
                            protected void start(ServerLevel level, Mob entity, long timestamp) {
                                this.navigation = (RedPandaNavigation) entity.getNavigation();

                                super.start(level, entity, timestamp);
                            }

                            @Override
                            protected boolean timedOut(long timestamp) {
                                return !this.navigation.isOnSurfacePath() && super.timedOut(timestamp);
                            }

                            @Override
                            protected boolean canStillUse(ServerLevel level, Mob entity, long timestamp) {
                                return this.navigation.isOnSurfacePath()
                                        && entity.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
                                        || super.canStillUse(level, entity, timestamp);
                            }
                        },
                        new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
                        new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)
                )
        );
    }

    private static void initIdleActivity(Brain<RedPandaEntity> brain) {
        BrainUtil.<RedPandaEntity>addActivity(brain,
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, SetEntityLookTargetSometimes.create(net.minecraft.world.entity.EntityTypes.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(1, new AnimalMakeLove(ReAnimalEntities.RED_PANDA, 1F, 1)),
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
                        Pair.of(4, new ClimbTree()),
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

    private static void initPanicActivity(Brain<RedPandaEntity> brain) {
        BrainUtil.<RedPandaEntity>addActivityAndRemoveMemoryWhenStopped(brain,
                Activity.PANIC,
                10,
                ImmutableList.of(
                        new RedPandaPanic()
                ),
                MemoryModuleType.IS_PANICKING
        );
    }

    public static void updateActivity(RedPandaEntity entity) {
        entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, Activity.IDLE));
    }

    public static Predicate<ItemStack> getTemptations() {
        return stack -> stack.is(ReAnimalTags.Items.RED_PANDA_FOOD);
    }

    private static Path findTreePath(RedPandaEntity panda) {
        var candidates = new HashSet<BlockPos>();
        var navigation = (RedPandaNavigation) panda.getNavigation();

        for (var support : BlockPos.betweenClosed(panda.blockPosition().offset(-TREE_SEARCH_RADIUS, 0, -TREE_SEARCH_RADIUS),
                panda.blockPosition().offset(TREE_SEARCH_RADIUS, TREE_SEARCH_HEIGHT, TREE_SEARCH_RADIUS))) {
            var state = panda.level().getBlockState(support);

            if (!state.is(ReAnimalTags.Blocks.RED_PANDA_CLIMBABLE) && !state.is(BlockTags.LEAVES))
                continue;

            for (var point : navigation.getPoints(support.above())) {
                if (point.side() == Direction.DOWN && point.position().y > panda.getY() + panda.maxUpStep() && navigation.canRest(point.position()))
                    candidates.add(point.cell());
            }
        }

        return candidates.isEmpty() ? null : navigation.createSurfacePath(candidates, 0, 16F);
    }

    private static class RedPandaPanic extends AnimalPanic<RedPandaEntity> {
        private BlockPos hidingPlace;
        private long nextTreeSearchTick;

        private RedPandaPanic() {
            super(1.5F, mob -> DamageTypeTags.PANIC_CAUSES);
        }

        @Override
        protected void start(ServerLevel level, RedPandaEntity panda, long timestamp) {
            super.start(level, panda, timestamp);

            this.hidingPlace = null;
            this.nextTreeSearchTick = timestamp;
        }

        @Override
        protected void tick(ServerLevel level, RedPandaEntity panda, long timestamp) {
            var navigation = (RedPandaNavigation) panda.getNavigation();

            if (!navigation.isDone())
                return;

            if (!panda.isOnFire() && !panda.isInLiquid() && !panda.isPassenger()) {
                if (panda.blockPosition().equals(this.hidingPlace) && navigation.canRest(panda.position()))
                    return;

                if (timestamp >= this.nextTreeSearchTick) {
                    this.nextTreeSearchTick = timestamp + 20L;

                    var path = findTreePath(panda);

                    if (path != null) {
                        this.hidingPlace = path.getTarget();
                        panda.setStandState(RedPandaEntity.StandState.ON_ALL_FOURS);
                        panda.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(Vec3.atBottomCenterOf(this.hidingPlace), 1.5F, 0));
                        navigation.moveTo(path, 1.5D);

                        return;
                    }
                }
            }

            super.tick(level, panda, timestamp);
        }
    }

    private static class ClimbTree extends Behavior<RedPandaEntity> {
        private long nextSearchTick;
        private Path path;

        private ClimbTree() {
            super(Map.of(
                    MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT
            ), 1);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, RedPandaEntity panda) {
            if (panda.isInLiquid() || panda.isPassenger() || level.getGameTime() < this.nextSearchTick)
                return false;

            this.nextSearchTick = level.getGameTime() + 100 + panda.getRandom().nextInt(100);

            this.path = findTreePath(panda);

            return this.path != null;
        }

        @Override
        protected void start(ServerLevel level, RedPandaEntity panda, long timestamp) {
            panda.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(Vec3.atBottomCenterOf(this.path.getTarget()), 1F, 0));
            panda.getNavigation().moveTo(this.path, 1D);
        }
    }
}
