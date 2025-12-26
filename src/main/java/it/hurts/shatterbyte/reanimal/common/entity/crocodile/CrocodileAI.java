package it.hurts.shatterbyte.reanimal.common.entity.crocodile;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
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
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class CrocodileAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super CrocodileEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            ReAnimalSensorTypes.CROCODILE_TEMPTATIONS.get(),
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
            MemoryModuleType.IS_TEMPTED,
            MemoryModuleType.BREED_TARGET,
            MemoryModuleType.NEAREST_VISIBLE_ADULT,
            MemoryModuleType.DANGER_DETECTED_RECENTLY,
            MemoryModuleType.ATTACK_TARGET,
            MemoryModuleType.ATTACK_COOLING_DOWN,
            MemoryModuleType.NEAREST_VISIBLE_PLAYER
    );

    public static final int ATTACK_ANIMATION_TICKS = 12;
    private static final int ATTACK_HIT_TICK = ATTACK_ANIMATION_TICKS - 1;
    private static final int ATTACK_COOLDOWN_TICKS = 20;

    public static Brain.Provider<CrocodileEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
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
        brain.addActivity(
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
        brain.addActivity(
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, StartAttacking.create(CrocodileAI::findNearestAttackableEntity)),
                        Pair.of(1, SetEntityLookTargetSometimes.create(EntityType.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(2, new AnimalMakeLove(ReAnimalEntities.CROCODILE.get(), 1F, 1)),
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
                        Pair.of(6, new CrocodileSwitchEnvironment(16, 0.1F)),
                        Pair.of(
                                7,
                                new RunOne<>(
                                        ImmutableMap.of(),
                                        ImmutableList.of(
                                                Pair.of(new CrocodileSwim(1.2F), 1),
                                                Pair.of(new CrocodileLandStroll(1F, 50, 100, 100, 200), 1))
                                )
                        )
                )
        );
    }

    private static void initFightActivity(Brain<CrocodileEntity> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(
                Activity.FIGHT,
                10,
                ImmutableList.of(
                        SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1F),
                        new CrocodileAttack(ATTACK_ANIMATION_TICKS, ATTACK_HIT_TICK, ATTACK_COOLDOWN_TICKS),
                        StopAttackingIfTargetInvalid.create()
                ),
                MemoryModuleType.ATTACK_TARGET
        );
    }

    private static void initPanicActivity(Brain<CrocodileEntity> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(
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

    private static Optional<? extends LivingEntity> findNearestAttackableEntity(CrocodileEntity entity) {
        var brain = entity.getBrain();

        if (brain.hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER) || brain.hasMemoryValue(MemoryModuleType.IS_TEMPTED)
                || !brain.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES))
            return Optional.empty();

        var followRange = entity.getAttributeValue(Attributes.FOLLOW_RANGE);
        var maxDistanceSq = followRange * followRange;

        var playerTarget = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER)
                .filter(target -> isValidTarget(entity, target) && target.distanceToSqr(entity) <= maxDistanceSq);

        if (playerTarget.isPresent())
            return playerTarget;

        return brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                .orElse(NearestVisibleLivingEntities.empty())
                .findClosest(target -> isValidTarget(entity, target) && target.distanceToSqr(entity) <= maxDistanceSq);
    }

    public static boolean isValidTarget(CrocodileEntity self, LivingEntity target) {
        return target.getType() != ReAnimalEntities.CROCODILE.get() && !(target instanceof Monster)
                && Sensor.isEntityAttackable(self, target) && !CrocodileAI.isHoldingFavoriteFood(target);
    }

    public static boolean isHoldingFavoriteFood(LivingEntity target) {
        return target.getMainHandItem().is(ReAnimalTags.Items.CROCODILE_FOOD) || target.getOffhandItem().is(ReAnimalTags.Items.CROCODILE_FOOD);
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

            return !crocodile.isInWaterOrBubble()
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

    public static class CrocodileSwim extends Behavior<CrocodileEntity> {
        private final float speed;

        public CrocodileSwim(float speed) {
            super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));

            this.speed = speed;
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, CrocodileEntity crocodile) {
            if (crocodile.hasEnvironmentTarget())
                return false;

            return crocodile.isInWaterOrBubble();
        }

        @Override
        protected void start(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            var random = crocodile.getRandom();

            for (int i = 0; i < 10; i++) {
                var dx = crocodile.getX() + (random.nextDouble() * 2.0D - 1.0D) * 6.0D;
                var dy = crocodile.getY() + (random.nextDouble() * 2.0D - 1.0D);
                var dz = crocodile.getZ() + (random.nextDouble() * 2.0D - 1.0D) * 6.0D;

                var pos = BlockPos.containing(dx, dy, dz);

                if (level.getFluidState(pos).is(FluidTags.WATER)) {
                    crocodile.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, this.speed, 0));

                    return;
                }
            }
        }

        @Override
        protected boolean canStillUse(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            if (crocodile.hasEnvironmentTarget())
                return false;

            return crocodile.isInWaterOrBubble();
        }
    }

    public static class CrocodileSwitchEnvironment extends Behavior<CrocodileEntity> {
        private final int radius;
        private final float startChance;
        private long lastSwitchGameTime;

        public CrocodileSwitchEnvironment(int radius, float startChance) {
            super(ImmutableMap.of());

            this.radius = radius;
            this.startChance = startChance;
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, CrocodileEntity crocodile) {
            var time = level.getGameTime();

            var inWater = crocodile.isInWaterOrBubble();
            var minInterval = inWater ? 400 : 800;

            if (time - this.lastSwitchGameTime < minInterval)
                return false;

            var chance = this.startChance * (inWater ? 1.25F : 0.25F);

            if (crocodile.getRandom().nextFloat() > chance)
                return false;

            return true;
        }

        @Override
        protected void start(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            var best = this.findTarget(level, crocodile);

            if (best != null) {
                crocodile.setEnvironmentTarget(best);

                this.lastSwitchGameTime = gameTime;
            }
        }

        private BlockPos findTarget(ServerLevel level, CrocodileEntity crocodile) {
            var origin = crocodile.blockPosition();
            var random = crocodile.getRandom();

            var toLand = crocodile.isInWaterOrBubble();

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
        protected boolean canStillUse(ServerLevel level, CrocodileEntity crocodile, long gameTime) {
            return false;
        }
    }

    private static class CrocodileAttack extends Behavior<CrocodileEntity> {
        private final int animationLength;
        private final int hitTick;
        private final int cooldownTicks;
        private int elapsedTicks;
        private boolean dealtDamage;

        public CrocodileAttack(int animationLength, int hitTick, int cooldownTicks) {
            super(ImmutableMap.of(
                    MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT,
                    MemoryModuleType.ATTACK_COOLING_DOWN, MemoryStatus.REGISTERED
            ));

            this.animationLength = animationLength;
            this.hitTick = hitTick;
            this.cooldownTicks = cooldownTicks;
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, CrocodileEntity entity) {
            return !entity.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN)
                    && CrocodileAI.getAttackTarget(entity).filter(entity::isWithinMeleeAttackRange).isPresent();
        }

        @Override
        protected void start(ServerLevel level, CrocodileEntity entity, long gameTime) {
            this.elapsedTicks = 0;
            this.dealtDamage = false;

            entity.startAttackAnimation();
        }

        @Override
        protected boolean canStillUse(ServerLevel level, CrocodileEntity entity, long gameTime) {
            return this.elapsedTicks < this.animationLength
                    && CrocodileAI.getAttackTarget(entity).filter(LivingEntity::isAlive).isPresent();
        }

        @Override
        protected void tick(ServerLevel level, CrocodileEntity entity, long gameTime) {
            this.elapsedTicks++;

            CrocodileAI.getAttackTarget(entity).ifPresent(target -> {
                entity.getLookControl().setLookAt(target, 30F, 30F);

                if (!this.dealtDamage && this.elapsedTicks == this.hitTick && entity.isWithinMeleeAttackRange(target)) {
                    this.dealtDamage = entity.doHurtTarget(target);

                    if (this.dealtDamage)
                        entity.playSound(ReAnimalSoundEvents.CROCODILE_BITE.get(), 1F, 1F);
                }
            });
        }

        @Override
        protected void stop(ServerLevel level, CrocodileEntity entity, long gameTime) {
            entity.setAttacking(false);

            entity.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN, Boolean.TRUE, this.cooldownTicks);
        }
    }
}
