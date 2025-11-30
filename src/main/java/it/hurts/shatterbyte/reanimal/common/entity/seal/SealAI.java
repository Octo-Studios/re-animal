package it.hurts.shatterbyte.reanimal.common.entity.seal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Set;
import java.util.function.Predicate;

public class SealAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super SealEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            ReAnimalSensorTypes.SEAL_TEMPTATIONS.get(),
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
            MemoryModuleType.DANGER_DETECTED_RECENTLY,
            MemoryModuleType.ATTACK_TARGET
    );

    public static Brain.Provider<SealEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<SealEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initPanicActivity(brain);

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<SealEntity> brain) {
        brain.addActivity(
                Activity.CORE,
                0,
                ImmutableList.of(
                        new LayCooldownBehavior(),
                        new LookAtTargetSink(45, 90),
                        new MoveToTargetSink(),
                        new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
                        new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)
                )
        );
    }

    private static void initIdleActivity(Brain<SealEntity> brain) {
        brain.addActivity(
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, new LayBehavior()),
                        Pair.of(1, SetEntityLookTargetSometimes.create(EntityType.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(2, new AnimalMakeLove(ReAnimalEntities.SEAL.get(), 1F, 1)),
                        Pair.of(
                                3,
                                new RunOne<>(
                                        ImmutableList.of(
                                                Pair.of(new FollowTemptation(entity -> 1.25F, entity -> entity.isBaby() ? 1D : 2D), 1),
                                                Pair.of(BabyFollowAdult.create(UniformInt.of(3, 6), 1.25F), 1)
                                        )
                                )
                        ),
                        Pair.of(4, new SealHuntFish(1.4F, 16.0D)),
                        Pair.of(5, new RandomLookAround(UniformInt.of(150, 250), 30F, 0F, 0F)),
                        Pair.of(6, new SealMoveToEnvironmentTarget(1.2F)),
                        Pair.of(7, new SealSwitchEnvironment(16, 0.1F)),
                        Pair.of(
                                8,
                                new RunOne<>(
                                        ImmutableMap.of(),
                                        ImmutableList.of(
                                                Pair.of(new SealSwim(1.2F), 1),
                                                Pair.of(new SealLandStroll(1F, 50, 100, 100, 200), 1))
                                )
                        )
                )
        );
    }

    private static void initPanicActivity(Brain<SealEntity> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(
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

    public static void updateActivity(SealEntity entity) {
        entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, Activity.IDLE));
    }

    public static Predicate<ItemStack> getTemptations() {
        return stack -> stack.is(ReAnimalTags.Items.SEAL_FOOD);
    }

    public static class LayCooldownBehavior extends Behavior<SealEntity> {
        public LayCooldownBehavior() {
            super(ImmutableMap.of(), 1200);
        }

        @Override
        protected boolean canStillUse(ServerLevel level, SealEntity entity, long gameTime) {
            return true;
        }

        @Override
        protected void tick(ServerLevel level, SealEntity entity, long gameTime) {
            if (entity.layCooldown > 0)
                entity.layCooldown--;
        }
    }

    public static class LayBehavior extends Behavior<SealEntity> {
        public LayBehavior() {
            super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT), 1200);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, SealEntity entity) {
            return entity.layCooldown == 0 && entity.canStartLaying();
        }

        @Override
        protected void start(ServerLevel level, SealEntity entity, long gameTime) {
            entity.startLayingDown();
        }

        @Override
        protected boolean canStillUse(ServerLevel level, SealEntity entity, long gameTime) {
            return entity.getLayState() != SealEntity.LayState.STANDING;
        }

        @Override
        protected void tick(ServerLevel level, SealEntity entity, long gameTime) {
            var brain = entity.getBrain();
            var layState = entity.getLayState();

            var shouldAbort = brain.hasMemoryValue(MemoryModuleType.IS_PANICKING)
                    || brain.hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER)
                    || entity.isInWaterOrBubble();

            if ((layState == SealEntity.LayState.LAYING_DOWN || layState == SealEntity.LayState.LAYING) && shouldAbort) {
                entity.startGettingUp();

                layState = entity.getLayState();
            }

            switch (layState) {
                case LAYING_DOWN -> {
                    if (--entity.layTime <= 0) {
                        entity.layTime = SealEntity.LAY_DURATION.sample(entity.getRandom());
                        entity.setLayState(SealEntity.LayState.LAYING);
                    }
                }
                case LAYING -> {
                    entity.getNavigation().stop();

                    if (--entity.layTime <= 0)
                        entity.startGettingUp();
                }
                case GETTING_UP -> {
                    if (--entity.layTime <= 0)
                        entity.finishGettingUp();
                }
                default -> {
                }
            }
        }

        @Override
        protected void stop(ServerLevel level, SealEntity entity, long gameTime) {
            if (entity.getLayState() != SealEntity.LayState.STANDING)
                entity.finishGettingUp();
        }
    }

    public static class SealMoveToEnvironmentTarget extends Behavior<SealEntity> {
        private final float speed;

        public SealMoveToEnvironmentTarget(float speed) {
            super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED));

            this.speed = speed;
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, SealEntity seal) {
            return seal.hasEnvironmentTarget()
                    && !seal.isLaying()
                    && !seal.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
        }

        @Override
        protected void start(ServerLevel level, SealEntity seal, long gameTime) {
            var target = seal.getEnvironmentTarget();

            if (target == null)
                return;

            seal.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(target, this.speed, 0)
            );
        }

        @Override
        protected boolean canStillUse(ServerLevel level, SealEntity seal, long gameTime) {
            if (!seal.hasEnvironmentTarget())
                return false;

            if (seal.isAtEnvironmentTarget())
                return false;

            return !seal.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
        }

        @Override
        protected void tick(ServerLevel level, SealEntity seal, long gameTime) {
            var target = seal.getEnvironmentTarget();

            if (target == null)
                return;

            seal.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(target, this.speed, 0)
            );

            if (seal.isAtEnvironmentTarget())
                seal.clearEnvironmentTarget();
        }

        @Override
        protected void stop(ServerLevel level, SealEntity seal, long gameTime) {
            if (seal.isAtEnvironmentTarget())
                seal.clearEnvironmentTarget();

            seal.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }

    public static class SealLandStroll extends Behavior<SealEntity> {
        private final float speed;
        private final int minRunTime;
        private final int maxRunTime;
        private final int minIdleTime;
        private final int maxIdleTime;

        private long endTick;
        private long nextStartTick;

        public SealLandStroll(float speed, int minRunTime, int maxRunTime, int minIdleTime, int maxIdleTime) {
            super(ImmutableMap.<MemoryModuleType<?>, MemoryStatus>of());

            this.speed = speed;
            this.minRunTime = minRunTime;
            this.maxRunTime = maxRunTime;
            this.minIdleTime = minIdleTime;
            this.maxIdleTime = maxIdleTime;
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, SealEntity seal) {
            if (seal.hasEnvironmentTarget())
                return false;

            if (seal.isLaying())
                return false;

            if (seal.isSwimming())
                return false;

            long gameTime = level.getGameTime();

            if (gameTime < this.nextStartTick)
                return false;

            var pos = LandRandomPos.getPos(seal, 10, 7);

            if (pos == null)
                return false;

            seal.getNavigation().moveTo(pos.x, pos.y, pos.z, this.speed);

            this.endTick = gameTime + Mth.nextInt(seal.getRandom(), this.minRunTime, this.maxRunTime);

            return true;
        }

        @Override
        protected boolean canStillUse(ServerLevel level, SealEntity seal, long gameTime) {
            if (seal.hasEnvironmentTarget())
                return false;

            return !seal.isInWaterOrBubble()
                    && !seal.getNavigation().isDone()
                    && gameTime < this.endTick;
        }

        @Override
        protected void start(ServerLevel level, SealEntity seal, long gameTime) {

        }

        @Override
        protected void stop(ServerLevel level, SealEntity seal, long gameTime) {
            seal.getNavigation().stop();

            this.nextStartTick = gameTime + Mth.nextInt(seal.getRandom(), this.minIdleTime, this.maxIdleTime);
        }

        @Override
        protected void tick(ServerLevel level, SealEntity seal, long gameTime) {

        }
    }

    public static class SealSwim extends Behavior<SealEntity> {
        private final float speed;

        public SealSwim(float speed) {
            super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));

            this.speed = speed;
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, SealEntity seal) {
            if (seal.hasEnvironmentTarget())
                return false;

            return seal.isInWaterOrBubble();
        }

        @Override
        protected void start(ServerLevel level, SealEntity seal, long gameTime) {
            var random = seal.getRandom();

            for (int i = 0; i < 10; i++) {
                var dx = seal.getX() + (random.nextDouble() * 2.0D - 1.0D) * 6.0D;
                var dy = seal.getY() + (random.nextDouble() * 2.0D - 1.0D) * 3.0D;
                var dz = seal.getZ() + (random.nextDouble() * 2.0D - 1.0D) * 6.0D;

                var pos = BlockPos.containing(dx, dy, dz);

                if (level.getFluidState(pos).is(FluidTags.WATER)) {
                    seal.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, this.speed, 0));

                    return;
                }
            }
        }

        @Override
        protected boolean canStillUse(ServerLevel level, SealEntity seal, long gameTime) {
            if (seal.hasEnvironmentTarget())
                return false;

            return seal.isInWaterOrBubble();
        }
    }

    public static class SealSwitchEnvironment extends Behavior<SealEntity> {
        private final int radius;
        private final float startChance;
        private long lastSwitchGameTime;

        public SealSwitchEnvironment(int radius, float startChance) {
            super(ImmutableMap.of());

            this.radius = radius;
            this.startChance = startChance;
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, SealEntity seal) {
            if (seal.isLaying())
                return false;

            var time = level.getGameTime();

            if (time - this.lastSwitchGameTime < 400)
                return false;

            if (seal.getRandom().nextFloat() > this.startChance)
                return false;

            return true;
        }

        @Override
        protected void start(ServerLevel level, SealEntity seal, long gameTime) {
            var best = this.findTarget(level, seal);

            if (best != null) {
                seal.setEnvironmentTarget(best);

                this.lastSwitchGameTime = gameTime;
            }
        }

        private BlockPos findTarget(ServerLevel level, SealEntity seal) {
            var origin = seal.blockPosition();
            var random = seal.getRandom();

            var toLand = seal.isInWaterOrBubble();

            for (var i = 0; i < 40; i++) {
                var dx = random.nextInt(this.radius * 2 + 1) - this.radius;
                var dz = random.nextInt(this.radius * 2 + 1) - this.radius;

                var x = origin.getX() + dx;
                var z = origin.getZ() + dz;

                var topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                if (topY <= level.getMinBuildHeight())
                    continue;

                var surface = new BlockPos(x, topY - 1, z);

                var surfaceState = level.getBlockState(surface);
                var surfaceFluid = level.getFluidState(surface);

                var head = surface.above();

                var headState = level.getBlockState(head);
                var headFluid = level.getFluidState(head);

                if (toLand) {
                    if (surfaceState.isAir() || surfaceFluid.is(FluidTags.WATER))
                        continue;

                    if (!headState.isAir() || !headFluid.isEmpty())
                        continue;
                } else {
                    if (!surfaceFluid.is(FluidTags.WATER))
                        continue;

                    if (!headState.isAir() && !headFluid.is(FluidTags.WATER))
                        continue;
                }

                return head.immutable();
            }

            return null;
        }

        @Override
        protected boolean canStillUse(ServerLevel level, SealEntity seal, long gameTime) {
            return false;
        }
    }

    public static class SealHuntFish extends Behavior<SealEntity> {
        private final float speed;
        private final double maxDistance;
        private final double maxDistanceSqr;
        private LivingEntity targetFish;
        private ItemEntity targetItem;
        private int attackCooldown;

        public SealHuntFish(float speed, double maxDistance) {
            super(ImmutableMap.of(
                    MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
                    MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED,
                    MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
            ));
            this.speed = speed;
            this.maxDistance = maxDistance;
            this.maxDistanceSqr = maxDistance * maxDistance;
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, SealEntity seal) {
            if (seal.hasEnvironmentTarget())
                return false;

            if (!seal.isInWaterOrBubble())
                return false;

            if (seal.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING))
                return false;

            this.targetFish = null;
            this.targetItem = null;

            var nearestDist = this.maxDistanceSqr;

            ItemEntity bestItem = null;

            for (var item : level.getEntitiesOfClass(
                    ItemEntity.class,
                    seal.getBoundingBox().inflate(this.maxDistance),
                    it -> it.isAlive()
                            && it.isInWaterOrBubble()
                            && it.getItem().is(ReAnimalTags.Items.SEAL_FOOD)
            )) {
                var d = seal.distanceToSqr(item);

                if (d <= nearestDist) {
                    nearestDist = d;
                    bestItem = item;
                }
            }

            if (bestItem != null) {
                this.targetItem = bestItem;
                this.attackCooldown = 0;

                return true;
            }

            var opt = seal.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);

            if (opt.isEmpty())
                return false;

            var visible = opt.get();

            this.targetFish = visible.findClosest(e ->
                            e instanceof AbstractFish fish
                                    && fish.isAlive()
                                    && seal.distanceToSqr(fish) <= this.maxDistanceSqr
                    )
                    .orElse(null);

            if (this.targetFish == null)
                return false;

            seal.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, this.targetFish);

            this.attackCooldown = 0;

            return true;
        }

        @Override
        protected boolean canStillUse(ServerLevel level, SealEntity seal, long gameTime) {
            if (seal.hasEnvironmentTarget())
                return false;

            if (!seal.isInWaterOrBubble())
                return false;

            if (seal.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING))
                return false;

            if (this.targetItem != null) {
                if (!this.targetItem.isAlive())
                    return false;

                if (seal.distanceToSqr(this.targetItem) > this.maxDistanceSqr)
                    return false;

                return true;
            }

            if (this.targetFish != null) {
                if (!this.targetFish.isAlive())
                    return false;

                if (seal.distanceToSqr(this.targetFish) > this.maxDistanceSqr)
                    return false;

                return true;
            }

            return false;
        }

        @Override
        protected void stop(ServerLevel level, SealEntity seal, long gameTime) {
            var brain = seal.getBrain();

            if (this.targetFish != null)
                brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);

            this.targetFish = null;
            this.targetItem = null;
        }

        @Override
        protected void tick(ServerLevel level, SealEntity seal, long gameTime) {
            if (this.targetItem != null) {
                seal.getLookControl().setLookAt(this.targetItem, 30.0F, 30.0F);
                seal.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetItem.position(), this.speed, 0));

                if (this.attackCooldown > 0) {
                    this.attackCooldown--;

                    return;
                }

                var distSqr = seal.distanceToSqr(this.targetItem);
                var reach = 1.5D;

                if (distSqr <= reach * reach) {
                    seal.doHurtTarget(this.targetItem);
                    this.attackCooldown = 20;
                }

                return;
            }

            if (this.targetFish == null)
                return;

            seal.getLookControl().setLookAt(this.targetFish, 30.0F, 30.0F);
            seal.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetFish, this.speed, 0));

            if (this.attackCooldown > 0) {
                this.attackCooldown--;
                return;
            }

            var distSqr = seal.distanceToSqr(this.targetFish);
            var attackReach = 2.0D;

            if (distSqr <= attackReach * attackReach) {
                seal.doHurtTarget(this.targetFish);

                this.attackCooldown = 20;
            }
        }
    }
}