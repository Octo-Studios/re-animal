package it.hurts.shatterbyte.reanimal.common.entity.seal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.common.entity.BrainUtil;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.function.Predicate;

public class SealAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super SealEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            ReAnimalSensorTypes.SEAL_TEMPTATIONS,
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
            MemoryModuleType.ATTACK_TARGET
    );

    public static Brain.Provider<SealEntity> brainProvider() {
        return BrainUtil.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    private static OneShot<SealEntity> rideAdult() {
        return BehaviorBuilder.create(instance -> instance.group(
                instance.present(MemoryModuleType.NEAREST_VISIBLE_ADULT),
                instance.registered(MemoryModuleType.WALK_TARGET),
                instance.registered(MemoryModuleType.LOOK_TARGET),
                instance.absent(MemoryModuleType.TEMPTING_PLAYER)
        ).apply(instance, (nearestAdult, walkTarget, lookTarget, temptingPlayer) -> (level, entity, timestamp) -> {
            if (entity.tickCount % 20 != 0 || !(instance.get(nearestAdult) instanceof SealEntity adult)
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

    public static Brain<?> makeBrain(Brain<SealEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initPanicActivity(brain);

        BrainUtil.<SealEntity>addActivity(brain, Activity.PLAY, 0, ImmutableList.of(new SealPlayWithBall()));

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<SealEntity> brain) {
        BrainUtil.<SealEntity>addActivity(brain,
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
        BrainUtil.<SealEntity>addActivity(brain,
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, new LayBehavior()),
                        Pair.of(1, SetEntityLookTargetSometimes.create(net.minecraft.world.entity.EntityTypes.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(2, new AnimalMakeLove(ReAnimalEntities.SEAL, 1F, 1)),
                        Pair.of(3, rideAdult()),
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
        BrainUtil.<SealEntity>addActivityAndRemoveMemoryWhenStopped(brain,
                Activity.PANIC,
                10,
                ImmutableList.of(new SealPanic()),
                MemoryModuleType.IS_PANICKING
        );
    }

    public static void updateActivity(SealEntity entity) {
        var brain = entity.getBrain();
        var previousActivity = brain.getActiveNonCoreActivity();

        brain.setActiveActivityToFirstValid(entity.isPlayingWithBall()
                ? ImmutableList.of(Activity.PANIC, Activity.PLAY, Activity.IDLE)
                : ImmutableList.of(Activity.PANIC, Activity.IDLE));

        if (!previousActivity.equals(brain.getActiveNonCoreActivity()))
            ((Brain<SealEntity>) brain).stopAll((ServerLevel) entity.level(), entity);
    }

    public static Predicate<ItemStack> getTemptations() {
        return stack -> stack.is(ReAnimalTags.Items.SEAL_FOOD);
    }

    private static BlockPos findWater(ServerLevel level, SealEntity seal, BlockPos origin) {
        for (var pos : BlockPos.withinBoxByManhattanDistance(origin, 16, 4, 16)) {
            if (!level.getFluidState(pos).is(FluidTags.WATER) || !level.getFluidState(pos.above()).isEmpty()
                    || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty())
                continue;

            var path = seal.getNavigation().createPath(pos, 0);

            if (path != null && path.canReach())
                return pos.immutable();
        }

        return null;
    }

    public static class SealPanic extends AnimalPanic<SealEntity> {
        public SealPanic() {
            super(1.5F, mob -> DamageTypeTags.PANIC_CAUSES, SealPanic::getPanicPos);
        }

        private static Vec3 getPanicPos(SealEntity seal) {
            var level = (ServerLevel) seal.level();
            var threat = seal.getLastHurtByMob();

            if (!seal.isInWater()) {
                var water = findWater(level, seal, seal.blockPosition());

                if (water != null)
                    return Vec3.atBottomCenterOf(water);

                return threat != null ? LandRandomPos.getPosAway(seal, 16, 4, threat.position()) : LandRandomPos.getPos(seal, 5, 4);
            }

            for (var i = 0; i < 10; i++) {
                var target = threat != null ? DefaultRandomPos.getPosAway(seal, 16, 4, threat.position()) : DefaultRandomPos.getPos(seal, 16, 4);

                if (target == null || !level.getFluidState(BlockPos.containing(target)).is(FluidTags.WATER))
                    continue;

                var path = seal.getNavigation().createPath(BlockPos.containing(target), 0);

                if (path != null && path.canReach())
                    return target;
            }

            return null;
        }

        @Override
        protected void start(ServerLevel level, SealEntity seal, long gameTime) {
            seal.clearEnvironmentTarget();

            if (seal.isLaying())
                seal.finishGettingUp();

            super.start(level, seal, gameTime);
        }
    }

    public static class SealPlayWithBall extends Behavior<SealEntity> {
        private BlockPos waterTarget;
        private long nextWaterSearch;
        private long nextHit;

        public SealPlayWithBall() {
            super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED), 1200);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, SealEntity seal) {
            return seal.isPlayingWithBall();
        }

        @Override
        protected boolean canStillUse(ServerLevel level, SealEntity seal, long gameTime) {
            return seal.isPlayingWithBall() && !seal.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
        }

        @Override
        protected void start(ServerLevel level, SealEntity seal, long gameTime) {
            seal.clearEnvironmentTarget();
            seal.getNavigation().stop();
            seal.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            seal.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);

            if (seal.isLaying())
                seal.finishGettingUp();

            this.waterTarget = null;
            this.nextWaterSearch = gameTime;
            this.nextHit = gameTime;
        }

        @Override
        protected void tick(ServerLevel level, SealEntity seal, long gameTime) {
            var ball = seal.getPlayBall();
            var brain = seal.getBrain();
            var play = seal.getBallPlay();

            brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(play.getLookTarget(), true));

            if (play.isWaiting()) {
                brain.eraseMemory(MemoryModuleType.WALK_TARGET);
                seal.getNavigation().stop();

                return;
            }

            if (!ball.isInWater() && !ball.onGround() && seal.isInWater()) {
                brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new Vec3(ball.getX(), seal.getY(), ball.getZ()), 1.4F, 1));

                return;
            }

            if (!ball.isInWater()) {
                if (gameTime >= this.nextWaterSearch) {
                    this.waterTarget = findWater(level, seal, ball.blockPosition());
                    this.nextWaterSearch = gameTime + 40;
                }

                if (this.waterTarget != null) {
                    brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(ball.position(), 1.3F, 0));

                    if (seal.distanceToSqr(ball) <= 1.5D * 1.5D && gameTime >= this.nextHit) {
                        var direction = Vec3.atCenterOf(this.waterTarget).subtract(ball.position()).multiply(1D, 0D, 1D).normalize();

                        ball.kick(direction.scale(0.85D).add(0D, 0.3D, 0D));
                        this.nextHit = gameTime + 25;
                    }

                    return;
                }
            }

            var circle = play.getCircleTarget();

            if (circle != null) {
                brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(circle, 1.4F, 0));

                return;
            }

            if ((ball.isInWater() && !seal.isInWater()) || seal.distanceToSqr(ball) > 1.8D * 1.8D) {
                var target = ball.position();

                if (ball.isInWater())
                    target = target.add(0D, -0.4D, 0D);
                else if (!ball.onGround())
                    target = new Vec3(ball.getX(), seal.getY(), ball.getZ());

                brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, ball.isInWater() ? 1.4F : 1.3F, 0));

                return;
            }

            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            seal.getNavigation().stop();

            play.hitBall();
        }

        @Override
        protected void stop(ServerLevel level, SealEntity seal, long gameTime) {
            seal.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            seal.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
            seal.getNavigation().stop();
        }
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
                    || entity.isInWater();

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

            return !seal.isInWater()
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

            return seal.isInWater();
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

            return seal.isInWater();
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

            var toLand = seal.isInWater();

            for (var i = 0; i < 40; i++) {
                var dx = random.nextInt(this.radius * 2 + 1) - this.radius;
                var dz = random.nextInt(this.radius * 2 + 1) - this.radius;

                var x = origin.getX() + dx;
                var z = origin.getZ() + dz;

                var topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                if (topY <= level.getMinY())
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

            if (!seal.isInWater())
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
                            && it.isInWater()
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

            if (!seal.isInWater())
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
                    seal.doHurtTarget((net.minecraft.server.level.ServerLevel) seal.level(), this.targetItem);
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
                seal.doHurtTarget((net.minecraft.server.level.ServerLevel) seal.level(), this.targetFish);

                this.attackCooldown = 20;
            }
        }
    }
}
