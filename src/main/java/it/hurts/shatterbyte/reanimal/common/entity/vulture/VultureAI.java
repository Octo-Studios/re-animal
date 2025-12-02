package it.hurts.shatterbyte.reanimal.common.entity.vulture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Predicate;

public class VultureAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super VultureEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            ReAnimalSensorTypes.VULTURE_TEMPTATIONS.get(),
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
            MemoryModuleType.DANGER_DETECTED_RECENTLY
    );

    public static Brain.Provider<VultureEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
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
        brain.addActivity(
                Activity.CORE,
                0,
                ImmutableList.of(
                        new net.minecraft.world.entity.ai.behavior.Swim(0.8F),
                        new LookAtTargetSink(45, 90),
                        new MoveToTargetSink(),
                        new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
                        new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)
                )
        );
    }

    private static void initIdleActivity(Brain<VultureEntity> brain) {
        brain.addActivity(
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, SetEntityLookTargetSometimes.create(EntityType.PLAYER, 8F, UniformInt.of(30, 60))),
                        Pair.of(
                                1,
                                new RunOne<>(
                                        ImmutableList.of(
                                                Pair.of(new FollowTemptation(e -> 1.2F, e -> 1.2D), 1),
                                                Pair.of(BabyFollowAdult.create(UniformInt.of(2, 4), 1.1F), 1)
                                        )
                                )
                        ),
                        Pair.of(2, new VultureRetaliateTask()),
                        Pair.of(3, new VultureStartCirclingTask()),
                        Pair.of(4, new VultureAttackFromCircleTask()),
                        Pair.of(5, new VultureChaseAndAttackTask()),
                        Pair.of(6, new VultureStopCirclingTask()),
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

    static class VultureRetaliateTask extends Behavior<VultureEntity> {
        VultureRetaliateTask() {
            super(ImmutableMap.of(
                    MemoryModuleType.HURT_BY_ENTITY, MemoryStatus.VALUE_PRESENT
            ));
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, VultureEntity entity) {
            var optional = entity.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY);

            if (optional.isEmpty())
                return false;

            var attacker = optional.get();

            if (!(attacker instanceof LivingEntity living))
                return false;

            if (!living.isAlive())
                return false;

            if (living.isInvulnerable())
                return false;

            if (living instanceof Player player && (player.isCreative() || player.isSpectator()))
                return false;

            return entity.distanceToSqr(living) <= 64D * 64D;
        }

        @Override
        protected void start(ServerLevel level, VultureEntity entity, long gameTime) {
            var optional = entity.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY);

            if (optional.isEmpty())
                return;

            var attacker = optional.get();

            if (!attacker.isAlive())
                return;

            if (attacker.isInvulnerable())
                return;

            if (attacker instanceof Player player && (player.isCreative() || player.isSpectator()))
                return;

            entity.setCirclingTarget(null);
            entity.setTarget(attacker);
            entity.getBrain().eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
            entity.getBrain().eraseMemory(MemoryModuleType.HURT_BY);
        }
    }

    static class VultureStartCirclingTask extends Behavior<VultureEntity> {
        VultureStartCirclingTask() {
            super(ImmutableMap.of(
                    MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
            ));
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, VultureEntity entity) {
            if (entity.getCirclingTarget() != null)
                return false;

            if (entity.getTarget() != null)
                return false;

            if (!entity.getNavigation().isDone())
                return false;

            if (entity.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY).isPresent())
                return false;

            var optional = entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);

            if (optional.isEmpty())
                return false;

            var gameTime = level.getGameTime();

            if (gameTime % 20L != 0L)
                return false;

            var visible = optional.get();
            var list = visible.findAll(e -> true);
            var candidates = new ArrayList<LivingEntity>();
            var radiusSq = 16D * 16D;

            for (var candidate : list) {
                if (candidate instanceof LivingEntity living
                        && living.isAlive()
                        && !living.isInvulnerable()
                        && !(living instanceof VultureEntity)
                        && !(living instanceof Player player && (player.isCreative() || player.isSpectator()))
                        && living.getHealth() < living.getMaxHealth()
                        && entity.distanceToSqr(living) <= radiusSq)
                    candidates.add(living);
            }

            return !candidates.isEmpty();
        }

        @Override
        protected void start(ServerLevel level, VultureEntity entity, long gameTime) {
            var optional = entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);

            if (optional.isEmpty())
                return;

            var visible = optional.get();
            var list = visible.findAll(e -> true);
            var candidates = new ArrayList<LivingEntity>();
            var radiusSq = 16D * 16D;

            for (var candidate : list) {
                if (candidate instanceof LivingEntity living
                        && living.isAlive()
                        && living.getHealth() < living.getMaxHealth()
                        && entity.distanceToSqr(living) <= radiusSq)
                    candidates.add(living);
            }

            if (candidates.isEmpty())
                return;

            candidates.sort((a, b) -> {
                var da = entity.distanceToSqr(a);
                var db = entity.distanceToSqr(b);
                return Double.compare(da, db);
            });

            var random = entity.getRandom();

            for (var living : candidates) {
                var maxHealth = living.getMaxHealth();

                if (maxHealth <= 0F)
                    continue;

                var healthRatio = living.getHealth() / maxHealth;

                if (healthRatio >= 1.0F)
                    continue;

                var attachChance = 1.0F - healthRatio;
                attachChance = Mth.clamp(attachChance, 0F, 1F);

                if (random.nextFloat() < attachChance) {
                    entity.setCirclingTarget(living);
                    entity.setCirclingAngle(random.nextFloat() * Mth.TWO_PI);
                    entity.setCirclingStartTick(level.getGameTime());
                    return;
                }
            }
        }
    }

    static class VultureStopCirclingTask extends Behavior<VultureEntity> {
        private static final long MAX_NON_PLAYER_CIRCLING_TICKS = 600L;

        VultureStopCirclingTask() {
            super(ImmutableMap.of());
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, VultureEntity entity) {
            var target = entity.getCirclingTarget();

            if (target == null)
                return false;

            if (!target.isAlive())
                return true;

            var optional = entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
            var visible = optional.map(list -> list.contains(target)).orElse(false);

            if (!visible)
                return true;

            if (!(target instanceof Player)) {
                var elapsed = level.getGameTime() - entity.getCirclingStartTick();

                if (elapsed >= MAX_NON_PLAYER_CIRCLING_TICKS)
                    return true;
            }

            return target.getHealth() >= target.getMaxHealth();
        }

        @Override
        protected void start(ServerLevel level, VultureEntity entity, long gameTime) {
            entity.setCirclingTarget(null);

            var pos = entity.blockPosition();
            var groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);

            entity.getNavigation().moveTo(
                    groundPos.getX() + 0.5D,
                    groundPos.getY(),
                    groundPos.getZ() + 0.5D,
                    1.0D
            );

            entity.setDeltaMovement(Vec3.ZERO);
        }
    }

    static class VultureAttackFromCircleTask extends Behavior<VultureEntity> {
        private static final long MIN_CIRCLING_TICKS = 200L;
        private static final long MAX_NON_PLAYER_CIRCLING_TICKS = 600L;

        VultureAttackFromCircleTask() {
            super(ImmutableMap.of());
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, VultureEntity entity) {
            var circlingTarget = entity.getCirclingTarget();

            if (circlingTarget == null)
                return false;

            if (!(circlingTarget instanceof LivingEntity living))
                return false;

            if (!living.isAlive())
                return false;

            if (living.isInvulnerable())
                return false;

            if (living instanceof Player player && (player.isCreative() || player.isSpectator()))
                return false;

            var currentTime = level.getGameTime();
            var startTime = entity.getCirclingStartTick();
            var elapsed = currentTime - startTime;

            if (elapsed < MIN_CIRCLING_TICKS)
                return false;

            if ((elapsed - MIN_CIRCLING_TICKS) % 20L != 0L)
                return false;

            var optional = entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
            var visible = optional.map(list -> list.contains(circlingTarget)).orElse(false);

            if (!visible)
                return false;

            if (!(circlingTarget instanceof Player) && elapsed >= MAX_NON_PLAYER_CIRCLING_TICKS)
                return false;

            var maxHealth = living.getMaxHealth();

            if (maxHealth <= 0F)
                return false;

            var healthRatio = living.getHealth() / maxHealth;

            if (healthRatio >= 1.0F)
                return false;

            if (healthRatio > 0.5F)
                return false;

            var attackChance = 0.5F - healthRatio;

            return entity.getRandom().nextFloat() < attackChance;
        }

        @Override
        protected void start(ServerLevel level, VultureEntity entity, long gameTime) {
            var circlingTarget = entity.getCirclingTarget();

            if (circlingTarget == null)
                return;

            entity.setCirclingTarget(null);
            entity.setTarget(circlingTarget);
        }
    }

    static class VultureChaseAndAttackTask extends Behavior<VultureEntity> {
        VultureChaseAndAttackTask() {
            super(ImmutableMap.of());
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, VultureEntity entity) {
            var target = entity.getTarget();

            if (!(target instanceof LivingEntity living))
                return false;

            if (!living.isAlive())
                return false;

            if (living.isInvulnerable())
                return false;

            if (living instanceof Player player && (player.isCreative() || player.isSpectator()))
                return false;

            return entity.distanceToSqr(living) <= 64D * 64D;
        }

        @Override
        protected boolean canStillUse(ServerLevel level, VultureEntity entity, long gameTime) {
            var target = entity.getTarget();

            if (!(target instanceof LivingEntity living))
                return false;

            if (!living.isAlive())
                return false;

            if (living.isInvulnerable())
                return false;

            if (living instanceof Player player && (player.isCreative() || player.isSpectator()))
                return false;

            return entity.distanceToSqr(living) <= 64D * 64D;
        }

        @Override
        protected void tick(ServerLevel level, VultureEntity entity, long gameTime) {
            var target = entity.getTarget();

            if (!(target instanceof LivingEntity living)) {
                entity.setTarget(null);
                entity.getNavigation().stop();
                return;
            }

            if (!living.isAlive()
                    || living.isInvulnerable()
                    || (living instanceof Player player && (player.isCreative() || player.isSpectator()))
                    || entity.distanceToSqr(living) > 64D * 64D) {
                entity.setTarget(null);
                entity.getNavigation().stop();
                return;
            }

            entity.getNavigation().moveTo(living, 1.4D);

            var distanceSqr = entity.distanceToSqr(living);

            if (distanceSqr <= 4.0D && gameTime % 20L == 0L)
                entity.doHurtTarget(living);
        }

        @Override
        protected void stop(ServerLevel level, VultureEntity entity, long gameTime) {
            var target = entity.getTarget();

            if (target instanceof LivingEntity living) {
                if (!living.isAlive()
                        || living.isInvulnerable()
                        || (living instanceof Player player && (player.isCreative() || player.isSpectator()))
                        || entity.distanceToSqr(living) > 64D * 64D)
                    entity.setTarget(null);
            } else {
                entity.setTarget(null);
            }

            entity.getNavigation().stop();
        }
    }
}