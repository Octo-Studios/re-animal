package it.hurts.shatterbyte.reanimal.common.entity.hedgehog;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.common.entity.BrainUtil;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class HedgehogAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super HedgehogEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, ReAnimalSensorTypes.HEDGEHOG_TEMPTATIONS, SensorType.NEAREST_ADULT, ReAnimalSensorTypes.HEDGEHOG_SCARE_DETECTED
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

    public static Brain.Provider<HedgehogEntity> brainProvider() {
        return BrainUtil.provider(MEMORY_TYPES, SENSOR_TYPES);
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
        initFightActivity(brain);
        BrainUtil.addActivity(brain, Activity.DIG, 0, ImmutableList.of());

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<HedgehogEntity> brain) {
        BrainUtil.<HedgehogEntity>addActivity(brain,
                Activity.CORE,
                0,
                ImmutableList.of(
                        new HedgehogDigging(),
                        new Swim<>(0.8F),
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
        BrainUtil.<HedgehogEntity>addActivity(brain,
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, SetEntityLookTargetSometimes.create(net.minecraft.world.entity.EntityTypes.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(1, new AnimalMakeLove(ReAnimalEntities.HEDGEHOG, 1F, 1)),
                        Pair.of(2, StartAttacking.create((level, entity) -> canHunt(entity), HedgehogAI::findNearestPrey)),
                        Pair.of(3, new HedgehogPickupItem()),
                        Pair.of(
                                4,
                                new RunOne<>(
                                        ImmutableList.of(
                                                Pair.of(new FollowTemptation(entity -> 1.25F, entity -> entity.isBaby() ? 1D : 2D), 1),
                                                Pair.of(BabyFollowAdult.create(UniformInt.of(2, 5), 1.25F), 1)
                                        )
                                )
                        ),
                        Pair.of(5, new RandomLookAround(UniformInt.of(150, 250), 30F, 0F, 0F)),
                        Pair.of(
                                6,
                                new RunOne<>(
                                        ImmutableMap.of(
                                                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                                                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT
                                        ),
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
        BrainUtil.<HedgehogEntity>addActivityWithConditions(brain,
                Activity.PANIC,
                ImmutableList.of(Pair.of(0, new HedgehogBallUp())),
                Set.of(
                        Pair.of(MemoryModuleType.DANGER_DETECTED_RECENTLY, MemoryStatus.VALUE_PRESENT),
                        Pair.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT)
                )
        );
    }

    private static void initFightActivity(Brain<HedgehogEntity> brain) {
        BrainUtil.<HedgehogEntity>addActivityAndRemoveMemoryWhenStopped(brain,
                Activity.FIGHT,
                10,
                ImmutableList.of(
                        StopAttackingIfTargetInvalid.create(),
                        SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.2F),
                        MeleeAttack.create(HedgehogAI::canHunt, 20)
                ),
                MemoryModuleType.ATTACK_TARGET
        );
    }

    public static void updateActivity(HedgehogEntity entity) {
        var brain = entity.getBrain();

        if (brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && !canHunt(entity)) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
            entity.getNavigation().stop();
        }

        brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, entity.isDigging() ? Activity.DIG : Activity.FIGHT, Activity.IDLE));
    }

    private static boolean canHunt(HedgehogEntity entity) {
        var brain = entity.getBrain();

        return !entity.isBaby() && !entity.isPassenger() && !entity.isScared() && !entity.isDigging() && !entity.isInLove()
                && !brain.hasMemoryValue(MemoryModuleType.IS_PANICKING)
                && !brain.hasMemoryValue(MemoryModuleType.DANGER_DETECTED_RECENTLY)
                && !brain.hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER);
    }

    private static Optional<LivingEntity> findNearestPrey(ServerLevel level, HedgehogEntity entity) {
        return entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                .flatMap(visible -> visible.findClosest(target -> target.getType().builtInRegistryHolder().is(ReAnimalTags.EntityTypes.HEDGEHOG_PREY)
                        && Sensor.isEntityAttackable(level, entity, target)));
    }

    public static Predicate<ItemStack> getTemptations() {
        return stack -> stack.is(ReAnimalTags.Items.HEDGEHOG_FOOD);
    }

    public static class HedgehogDigging extends Behavior<HedgehogEntity> {
        private static final int DIG_DURATION_TICKS = 80;
        private static final int DIG_TRANSITION_TICKS = 5;
        private static final UniformInt DIG_COOLDOWN = UniformInt.of(600, 1200);

        private BlockPos target;
        private Vec3 digPosition;
        private int digTicks;
        private long nextDigTick;

        public HedgehogDigging() {
            super(Map.of(
                    MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.DANGER_DETECTED_RECENTLY, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT
            ), DIG_DURATION_TICKS);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, HedgehogEntity entity) {
            if (!entity.getBrain().isActive(Activity.IDLE) || level.getGameTime() < this.nextDigTick
                    || !entity.onGround() || entity.isInLiquid() || entity.isPassenger() || entity.isScared() || entity.isInLove())
                return false;

            var front = entity.position().add(new Vec3(0D, 0D, entity.getBbWidth() * 0.5D).yRot(-entity.yBodyRot * Mth.DEG_TO_RAD));

            this.target = BlockPos.containing(front.x, entity.getOnPos().getY(), front.z);

            var state = level.getBlockState(this.target);

            if (!state.is(ReAnimalTags.Blocks.HEDGEHOG_DIGGABLE))
                return false;

            this.digPosition = new Vec3(front.x, this.target.getY() + state.getCollisionShape(level, this.target).max(Direction.Axis.Y), front.z);

            return true;
        }

        @Override
        protected void start(ServerLevel level, HedgehogEntity entity, long gameTime) {
            this.digTicks = 0;
            entity.getNavigation().stop();
            entity.getMoveControl().setWait();
            entity.setDeltaMovement(0D, entity.getDeltaMovement().y, 0D);
            entity.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
            entity.setYRot(entity.yBodyRot);
            entity.yHeadRot = entity.yBodyRot;
            entity.setDigging(true);
            updateActivity(entity);
        }

        @Override
        protected boolean canStillUse(ServerLevel level, HedgehogEntity entity, long gameTime) {
            var brain = entity.getBrain();

            return entity.isDigging() && entity.onGround() && !entity.isInLiquid() && !entity.isPassenger()
                    && !entity.isScared() && !entity.isInLove()
                    && !brain.hasMemoryValue(MemoryModuleType.IS_PANICKING)
                    && !brain.hasMemoryValue(MemoryModuleType.DANGER_DETECTED_RECENTLY)
                    && !brain.hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER)
                    && !brain.hasMemoryValue(MemoryModuleType.BREED_TARGET)
                    && level.getBlockState(this.target).is(ReAnimalTags.Blocks.HEDGEHOG_DIGGABLE);
        }

        @Override
        protected void tick(ServerLevel level, HedgehogEntity entity, long gameTime) {
            if (++this.digTicks >= DIG_TRANSITION_TICKS && this.digTicks <= DIG_DURATION_TICKS - DIG_TRANSITION_TICKS && this.digTicks % 4 == 0)
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(this.target)),
                        this.digPosition.x, this.digPosition.y, this.digPosition.z, 4, entity.getBbWidth() * 0.2D, 0D, entity.getBbWidth() * 0.2D, 0.04D);
        }

        @Override
        protected void stop(ServerLevel level, HedgehogEntity entity, long gameTime) {
            entity.setDigging(false);
            this.nextDigTick = gameTime + DIG_COOLDOWN.sample(entity.getRandom());
            updateActivity(entity);
        }
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

                    if (i < (long) HedgehogEntity.HedgehogState.UNROLLING.getAnimationDuration())
                        entity.setState(HedgehogEntity.HedgehogState.UNROLLING);

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

            if (entity.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET))
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
            if (entity.isBaby() || !entity.getStack().isEmpty() || entity.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET))
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
