package it.hurts.shatterbyte.reanimal.common.entity.crocodile;

import it.hurts.shatterbyte.reanimal.common.entity.BrainUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class CrocodileAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super CrocodileEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            ReAnimalSensorTypes.CROCODILE_TEMPTATIONS,
            SensorType.NEAREST_ADULT,
            SensorType.NEAREST_PLAYERS
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
            MemoryModuleType.ATTACK_COOLING_DOWN,
            MemoryModuleType.NEAREST_VISIBLE_PLAYER
    );

    public static final int BITE_ANIMATION_TICKS = 20;
    public static final int SPIN_ANIMATION_TICKS = 32;
    private static final int ATTACK_COOLDOWN_TICKS = 20;

    public static Brain.Provider<CrocodileEntity> brainProvider() {
        return BrainUtil.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<CrocodileEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initPanicActivity(brain);
        initFightActivity(brain);

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<CrocodileEntity> brain) {
        BrainUtil.<CrocodileEntity>addActivity(brain,
                Activity.CORE,
                0,
                ImmutableList.of(
                        new LookAtTargetSink(45, 90),
                        new MoveToTargetSink(),
                        new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
                        new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)
                )
        );
    }

    private static void initIdleActivity(Brain<CrocodileEntity> brain) {
        BrainUtil.<CrocodileEntity>addActivity(brain,
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, StartAttacking.create(CrocodileAI::findNearestAttackableEntity)),
                        Pair.of(1, SetEntityLookTargetSometimes.create(net.minecraft.world.entity.EntityTypes.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(2, new AnimalMakeLove(ReAnimalEntities.CROCODILE, 1F, 1)),
                        Pair.of(
                                3,
                                new RunOne<>(
                                        ImmutableList.of(
                                                Pair.of(new FollowTemptation(entity -> 1.25F, entity -> entity.isBaby() ? 1D : 2D), 1),
                                                Pair.of(BabyFollowAdult.create(UniformInt.of(3, 6), 1.25F), 1)
                                        )
                                )
                        ),
                        Pair.of(4, new RandomLookAround(UniformInt.of(150, 250), 30F, 0F, 0F)),
                        Pair.of(5, new CrocodileMoveToEnvironmentTarget(1.2F)),
                        Pair.of(
                                7,
                                new RunOne<>(
                                        ImmutableMap.of(
                                                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                                                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
                                                MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT,
                                                MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT
                                        ),
                                        ImmutableList.of(
                                                Pair.of(new CrocodileAmbush(), 2),
                                                Pair.of(new CrocodileBask(), 1),
                                                Pair.of(BehaviorBuilder.triggerIf(crocodile -> !crocodile.hasEnvironmentTarget(),
                                                        (OneShot<? super CrocodileEntity>) RandomStroll.swim(1.2F)), 1),
                                                Pair.of(new CrocodileLandStroll(1F, 50, 100, 100, 200), 1))
                                )
                        )
                )
        );
    }

    private static void initFightActivity(Brain<CrocodileEntity> brain) {
        BrainUtil.<CrocodileEntity>addActivityAndRemoveMemoryWhenStopped(brain,
                Activity.FIGHT,
                10,
                ImmutableList.of(
                        new CrocodileHunt(),
                        new CrocodileAttack(),
                        StopAttackingIfTargetInvalid.create()
                ),
                MemoryModuleType.ATTACK_TARGET
        );
    }

    private static void initPanicActivity(Brain<CrocodileEntity> brain) {
        BrainUtil.<CrocodileEntity>addActivityAndRemoveMemoryWhenStopped(brain,
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

    public static void updateActivity(CrocodileEntity entity) {
        entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, Activity.FIGHT, Activity.IDLE));
    }

    public static Predicate<ItemStack> getTemptations() {
        return stack -> stack.is(ReAnimalTags.Items.CROCODILE_FOOD);
    }

    private static Optional<LivingEntity> getAttackTarget(CrocodileEntity entity) {
        return entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
    }

    private static Optional<? extends LivingEntity> findNearestAttackableEntity(net.minecraft.server.level.ServerLevel level, CrocodileEntity entity) {
        var brain = entity.getBrain();

        if (brain.hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER)
                || !brain.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES))
            return Optional.empty();

        var followRange = entity.getAttributeValue(Attributes.FOLLOW_RANGE);
        var maxDistanceSq = followRange * followRange;

        var playerTarget = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER)
                .filter(target -> isValidTarget(entity, target) && target.distanceToSqr(entity) <= maxDistanceSq && canHuntFromHere(level, entity, target));

        if (playerTarget.isPresent())
            return playerTarget;

        return brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                .orElse(NearestVisibleLivingEntities.empty())
                .findClosest(target -> isValidTarget(entity, target) && target.distanceToSqr(entity) <= maxDistanceSq && canHuntFromHere(level, entity, target));
    }

    public static boolean isValidTarget(CrocodileEntity self, LivingEntity target) {
        return target.getType() != ReAnimalEntities.CROCODILE && !(target instanceof Monster)
                && self.level() instanceof net.minecraft.server.level.ServerLevel serverLevel && Sensor.isEntityAttackable(serverLevel, self, target) && !CrocodileAI.isHoldingFavoriteFood(target);
    }

    public static boolean isHoldingFavoriteFood(LivingEntity target) {
        return target.getMainHandItem().is(ReAnimalTags.Items.CROCODILE_FOOD) || target.getOffhandItem().is(ReAnimalTags.Items.CROCODILE_FOOD);
    }

    private static boolean canHuntFromHere(ServerLevel level, CrocodileEntity crocodile, LivingEntity target) {
        if (crocodile.isBasking())
            return crocodile.distanceToSqr(target) <= 9D;

        if (crocodile.isInWater() && !target.isInWater()) {
            if (crocodile.distanceToSqr(target) > 36D)
                return false;

            for (var pos : BlockPos.withinBoxByManhattanDistance(target.blockPosition(), 2, 2, 2))
                if (level.getFluidState(pos).is(FluidTags.WATER))
                    return true;

            return false;
        }

        return true;
    }

    @Nullable
    private static BlockPos findDeepWater(ServerLevel level, CrocodileEntity crocodile) {
        for (var pos : BlockPos.withinBoxByManhattanDistance(crocodile.blockPosition(), 8, 4, 8)) {
            if (!level.getFluidState(pos).is(FluidTags.WATER) || !level.getFluidState(pos.above()).is(FluidTags.WATER)
                    || !level.noCollision(crocodile, crocodile.getBoundingBox().move(Vec3.atBottomCenterOf(pos).subtract(crocodile.position()))))
                continue;

            var path = crocodile.getNavigation().createPath(pos, 0);

            if (path != null && path.canReach())
                return pos.immutable();
        }

        return null;
    }

    @Nullable
    private static ShoreSpot findShore(ServerLevel level, CrocodileEntity crocodile, boolean toLand) {
        var range = Mth.ceil(crocodile.getAttributeValue(Attributes.FOLLOW_RANGE));
        var origin = crocodile.blockPosition();

        while (level.getFluidState(origin.above()).is(FluidTags.WATER))
            origin = origin.above();

        for (var pos : BlockPos.withinBoxByManhattanDistance(origin, range, 4, range)) {
            if (!level.getFluidState(pos).is(FluidTags.WATER) || !level.getFluidState(pos.above()).isEmpty())
                continue;

            var surface = pos.getY() + level.getFluidState(pos).getHeight(level, pos) - crocodile.getBbHeight() * 0.75D;
            var waterPosition = new Vec3(pos.getX() + 0.5D, surface, pos.getZ() + 0.5D);

            if (!level.noCollision(crocodile, crocodile.getBoundingBox().move(waterPosition.subtract(crocodile.position()))))
                continue;

            for (var direction : Direction.Plane.HORIZONTAL) {
                for (var distance = 1; distance <= 3; distance++) {
                    var land = pos.relative(direction, distance).above();

                    if (!level.getBlockState(land).getCollisionShape(level, land).isEmpty())
                        land = land.above();

                    if (!level.getFluidState(land).isEmpty() || !level.getBlockState(land.below()).entityCanStandOn(level, land.below(), crocodile)
                            || !level.noCollision(crocodile, crocodile.getBoundingBox().move(Vec3.atBottomCenterOf(land).subtract(crocodile.position())))
                            || toLand && !level.canSeeSky(land))
                        continue;

                    var path = crocodile.getNavigation().createPath(toLand ? land : pos, 0);

                    if (path != null && path.canReach())
                        return new ShoreSpot(pos.immutable(), land);
                }
            }
        }

        return null;
    }

    private record ShoreSpot(BlockPos water, BlockPos land) {
    }

    private static class CrocodileAmbush extends Behavior<CrocodileEntity> {
        private ShoreSpot shore;
        private long nextStartTick;

        public CrocodileAmbush() {
            super(ImmutableMap.of(
                    MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT
            ), 600, 1200);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, CrocodileEntity crocodile) {
            if (crocodile.isBaby() || !crocodile.isInWater() || crocodile.hasEgg() || crocodile.hasEnvironmentTarget()
                    || level.getGameTime() < this.nextStartTick)
                return false;

            this.nextStartTick = level.getGameTime() + 200;
            this.shore = findShore(level, crocodile, false);

            return this.shore != null;
        }

        @Override
        protected void start(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            crocodile.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.shore.water(), 1.2F, 0));
        }

        @Override
        protected boolean canStillUse(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            return crocodile.isInWater() && !crocodile.hasEnvironmentTarget() && this.hasRequiredMemories(crocodile)
                    && level.getFluidState(this.shore.water()).is(FluidTags.WATER)
                    && level.getFluidState(this.shore.land()).isEmpty();
        }

        @Override
        protected void tick(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            if (!crocodile.isAmbushing() && crocodile.position().subtract(Vec3.atCenterOf(this.shore.water())).horizontalDistanceSqr() < 1D) {
                crocodile.setAmbushing(true);
                crocodile.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                crocodile.getBrain().eraseMemory(MemoryModuleType.PATH);
                crocodile.getNavigation().stop();
                crocodile.stopDash();
            }

            if (crocodile.isAmbushing()) {
                var direction = Vec3.atCenterOf(this.shore.land()).subtract(crocodile.position());
                var surface = this.shore.water().getY() + level.getFluidState(this.shore.water()).getHeight(level, this.shore.water());
                var speed = crocodile.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.4D;

                crocodile.setDeltaMovement(0D, Mth.clamp((surface - crocodile.getBbHeight() * 0.75D - crocodile.getY()) * 0.2D, -speed, speed), 0D);
                crocodile.setYRot((float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90F);
                crocodile.yBodyRot = crocodile.getYRot();
                crocodile.yHeadRot = crocodile.getYRot();
                crocodile.setXRot(0F);
            }
        }

        @Override
        protected void stop(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            crocodile.setAmbushing(false);
            crocodile.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            crocodile.getNavigation().stop();
            this.nextStartTick = gameTime + 200 + crocodile.getRandom().nextInt(200);
        }
    }

    private static class CrocodileBask extends Behavior<CrocodileEntity> {
        private ShoreSpot shore;
        private long nextStartTick;

        public CrocodileBask() {
            super(ImmutableMap.of(
                    MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT
            ), 1200, 1800);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, CrocodileEntity crocodile) {
            if (crocodile.isBaby() || crocodile.hasEgg() || crocodile.hasEnvironmentTarget() || !level.isBrightOutside()
                    || level.isRaining() || level.getGameTime() < this.nextStartTick)
                return false;

            this.nextStartTick = level.getGameTime() + 200;
            this.shore = findShore(level, crocodile, true);

            return this.shore != null;
        }

        @Override
        protected void start(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            crocodile.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.shore.land(), 1F, 0));
        }

        @Override
        protected boolean canStillUse(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            return this.hasRequiredMemories(crocodile) && !crocodile.hasEnvironmentTarget() && level.isBrightOutside()
                    && !level.isRaining() && level.getFluidState(this.shore.land()).isEmpty()
                    && level.getBlockState(this.shore.land().below()).entityCanStandOn(level, this.shore.land().below(), crocodile);
        }

        @Override
        protected void tick(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            if (!crocodile.isBasking() && crocodile.onGround() && !crocodile.isInWater()
                    && crocodile.position().distanceToSqr(Vec3.atBottomCenterOf(this.shore.land())) < 1D) {
                crocodile.setBasking(true);
                crocodile.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                crocodile.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
                crocodile.getBrain().eraseMemory(MemoryModuleType.PATH);
                crocodile.getNavigation().stop();
                crocodile.stopDash();
                crocodile.setXRot(0F);
            }
        }

        @Override
        protected void stop(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            var wasBasking = crocodile.isBasking();

            crocodile.setBasking(false);
            crocodile.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            crocodile.getNavigation().stop();

            if (wasBasking && this.hasRequiredMemories(crocodile) && !crocodile.hasEnvironmentTarget())
                crocodile.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.shore.water(), 1.2F, 0));

            this.nextStartTick = gameTime + 1200 + crocodile.getRandom().nextInt(1200);
        }
    }

    private static class CrocodileHunt extends Behavior<CrocodileEntity> {
        public CrocodileHunt() {
            super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), 1200);
        }

        @Override
        protected boolean canStillUse(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            return getAttackTarget(crocodile).filter(LivingEntity::isAlive).isPresent();
        }

        @Override
        protected void tick(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            var target = getAttackTarget(crocodile).orElseThrow();
            var brain = crocodile.getBrain();

            if (crocodile.getGrabbedPrey() == target) {
                if (crocodile.isInWater() && target.isInWater()) {
                    brain.eraseMemory(MemoryModuleType.WALK_TARGET);
                    crocodile.getNavigation().stop();
                } else {
                    var water = crocodile.getDragTarget();

                    if (water == null || !level.getFluidState(water).is(FluidTags.WATER)) {
                        water = findDeepWater(level, crocodile);
                        crocodile.setDragTarget(water);
                    }

                    if (water != null)
                        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(water, 1.4F, 0));
                    else
                        crocodile.releasePrey();
                }

                return;
            }

            if (crocodile.isDashing() || crocodile.isAttacking())
                return;

            if (crocodile.isWithinBiteRange(target)) {
                brain.eraseMemory(MemoryModuleType.WALK_TARGET);
                crocodile.getNavigation().stop();
                crocodile.turnToward(target);
            } else {
                brain.setMemory(MemoryModuleType.WALK_TARGET, crocodile.isInWater() && target.isInWater()
                        ? new WalkTarget(target.position().add(0D, 0.5D, 0D), 1.2F, 0)
                        : new WalkTarget(target, 1.2F, 0));
                if (!brain.hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN))
                    crocodile.tryDash(target);
            }
        }

        @Override
        protected void stop(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            crocodile.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            crocodile.getNavigation().stop();
            crocodile.stopDash();
            crocodile.releasePrey();
        }
    }

    public static class CrocodileMoveToEnvironmentTarget extends Behavior<CrocodileEntity> {
        private final float speed;

        public CrocodileMoveToEnvironmentTarget(float speed) {
            super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED));

            this.speed = speed;
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, CrocodileEntity crocodile) {
            return crocodile.hasEnvironmentTarget()
                    && !crocodile.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
        }

        @Override
        protected void start(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            var target = crocodile.getEnvironmentTarget();

            if (target == null)
                return;

            crocodile.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(target, this.speed, 0)
            );
        }

        @Override
        protected boolean canStillUse(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            if (!crocodile.hasEnvironmentTarget())
                return false;

            if (crocodile.isAtEnvironmentTarget())
                return false;

            return !crocodile.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
        }

        @Override
        protected void tick(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            var target = crocodile.getEnvironmentTarget();

            if (target == null)
                return;

            crocodile.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(target, this.speed, 0)
            );

            if (crocodile.isAtEnvironmentTarget())
                crocodile.clearEnvironmentTarget();
        }

        @Override
        protected void stop(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            if (crocodile.isAtEnvironmentTarget())
                crocodile.clearEnvironmentTarget();

            crocodile.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }

    public static class CrocodileLandStroll extends Behavior<CrocodileEntity> {
        private final float speed;
        private final int minRunTime;
        private final int maxRunTime;
        private final int minIdleTime;
        private final int maxIdleTime;

        private long endTick;
        private long nextStartTick;

        public CrocodileLandStroll(float speed, int minRunTime, int maxRunTime, int minIdleTime, int maxIdleTime) {
            super(ImmutableMap.<MemoryModuleType<?>, MemoryStatus>of());

            this.speed = speed;
            this.minRunTime = minRunTime;
            this.maxRunTime = maxRunTime;
            this.minIdleTime = minIdleTime;
            this.maxIdleTime = maxIdleTime;
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, CrocodileEntity crocodile) {
            if (crocodile.hasEnvironmentTarget())
                return false;

            if (crocodile.isSwimming())
                return false;

            long gameTime = level.getGameTime();

            if (gameTime < this.nextStartTick)
                return false;

            var pos = LandRandomPos.getPos(crocodile, 10, 7);

            if (pos == null)
                return false;

            crocodile.getNavigation().moveTo(pos.x, pos.y, pos.z, this.speed);

            this.endTick = gameTime + Mth.nextInt(crocodile.getRandom(), this.minRunTime, this.maxRunTime);

            return true;
        }

        @Override
        protected boolean canStillUse(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            if (crocodile.hasEnvironmentTarget())
                return false;

            return !crocodile.isInWater()
                    && !crocodile.getNavigation().isDone()
                    && gameTime < this.endTick;
        }

        @Override
        protected void start(ServerLevel level, CrocodileEntity crocodile, long gameTime) {

        }

        @Override
        protected void stop(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            crocodile.getNavigation().stop();

            this.nextStartTick = gameTime + Mth.nextInt(crocodile.getRandom(), this.minIdleTime, this.maxIdleTime);
        }

        @Override
        protected void tick(ServerLevel level, CrocodileEntity crocodile, long gameTime) {

        }
    }

    private static class CrocodileAttack extends Behavior<CrocodileEntity> {
        private int animationLength;
        private int hitTick;
        private int elapsedTicks;

        public CrocodileAttack() {
            super(ImmutableMap.of(
                    MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT,
                    MemoryModuleType.ATTACK_COOLING_DOWN, MemoryStatus.REGISTERED
            ));
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, CrocodileEntity entity) {
            return !entity.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN)
                    && CrocodileAI.getAttackTarget(entity).filter(target -> entity.getGrabbedPrey() == target
                    ? entity.isInWater() && target.isInWater() : entity.canBite(target)).isPresent();
        }

        @Override
        protected void start(ServerLevel level, CrocodileEntity entity, long gameTime) {
            this.elapsedTicks = 0;

            var target = getAttackTarget(entity).orElseThrow();
            var spinning = entity.getGrabbedPrey() == target;

            this.animationLength = spinning ? SPIN_ANIMATION_TICKS : BITE_ANIMATION_TICKS;
            this.hitTick = spinning ? 18 : entity.isDashing() ? 1 : 9;

            entity.startAttackAnimation(spinning);
        }

        @Override
        protected boolean canStillUse(ServerLevel level, CrocodileEntity entity, long gameTime) {
            return this.elapsedTicks < this.animationLength
                    && CrocodileAI.getAttackTarget(entity).filter(LivingEntity::isAlive).isPresent();
        }

        @Override
        protected void tick(ServerLevel level, CrocodileEntity entity, long gameTime) {
            if (++this.elapsedTicks != this.hitTick)
                return;

            var target = getAttackTarget(entity).orElseThrow();
            if (entity.isSpinning() ? entity.getGrabbedPrey() != target : !entity.canBite(target))
                return;

            var grabbed = false;
            if (entity.canGrab(target) && entity.getRandom().nextBoolean()) {
                var water = findDeepWater(level, entity);
                grabbed = water != null && entity.grabPrey(target, water);
            }

            if (grabbed || entity.doHurtTarget(level, target))
                entity.playSound(ReAnimalSoundEvents.CROCODILE_BITE, 1F, 1F);
        }

        @Override
        protected void stop(ServerLevel level, CrocodileEntity entity, long gameTime) {
            entity.setAttacking(false);

            entity.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN, Boolean.TRUE, ATTACK_COOLDOWN_TICKS);
        }
    }
}
