package it.hurts.shatterbyte.reanimal.common.entity.hippopotamus;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class HippopotamusAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super HippopotamusEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            ReAnimalSensorTypes.HIPPOPOTAMUS_TEMPTATIONS.get(),
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

    public static Brain.Provider<HippopotamusEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<HippopotamusEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initPanicActivity(brain);
        initFightActivity(brain);

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<HippopotamusEntity> brain) {
        brain.addActivity(
                Activity.CORE,
                0,
                ImmutableList.of(
                        new Swim(0.8F),
                        new LookAtTargetSink(45, 90),
                        new MoveToTargetSink(),
                        new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
                        new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)
                )
        );
    }

    private static void initIdleActivity(Brain<HippopotamusEntity> brain) {
        brain.addActivity(
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, StartAttacking.create(HippopotamusAI::findNearestAttackableEntity)),
                        Pair.of(1, SetEntityLookTargetSometimes.create(EntityType.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(2, new AnimalMakeLove(ReAnimalEntities.HIPPOPOTAMUS.get(), 1F, 1)),
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

    private static void initFightActivity(Brain<HippopotamusEntity> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(
                Activity.FIGHT,
                10,
                ImmutableList.of(
                        SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1F),
                        new HippopotamusAttack(ATTACK_ANIMATION_TICKS, ATTACK_HIT_TICK, ATTACK_COOLDOWN_TICKS),
                        StopAttackingIfTargetInvalid.create()
                ),
                MemoryModuleType.ATTACK_TARGET
        );
    }

    private static void initPanicActivity(Brain<HippopotamusEntity> brain) {
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

    public static void updateActivity(HippopotamusEntity entity) {
        entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, Activity.FIGHT, Activity.IDLE));
    }

    public static Predicate<ItemStack> getTemptations() {
        return stack -> stack.is(ReAnimalTags.Items.HIPPOPOTAMUS_FOOD);
    }

    private static Optional<LivingEntity> getAttackTarget(HippopotamusEntity entity) {
        return entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
    }

    private static Optional<? extends LivingEntity> findNearestAttackableEntity(HippopotamusEntity entity) {
        var brain = entity.getBrain();

        if (brain.hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER) || brain.hasMemoryValue(MemoryModuleType.IS_TEMPTED)
                || !brain.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES))
            return Optional.empty();

        return brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                .orElse(NearestVisibleLivingEntities.empty())
                .findClosest(target -> isValidTarget(entity, target) && target.distanceToSqr(entity) <= 64D);
    }

    public static boolean isValidTarget(HippopotamusEntity self, LivingEntity target) {
        return target.getType() != ReAnimalEntities.HIPPOPOTAMUS.get() && !(target instanceof Monster)
                && Sensor.isEntityAttackable(self, target) && !HippopotamusAI.isHoldingFavoriteFood(target);
    }

    public static boolean isHoldingFavoriteFood(LivingEntity target) {
        return target.getMainHandItem().is(ReAnimalTags.Items.HIPPOPOTAMUS_FOOD) || target.getOffhandItem().is(ReAnimalTags.Items.HIPPOPOTAMUS_FOOD);
    }

    private static class HippopotamusAttack extends Behavior<HippopotamusEntity> {
        private final int animationLength;
        private final int hitTick;
        private final int cooldownTicks;
        private int elapsedTicks;
        private boolean dealtDamage;

        public HippopotamusAttack(int animationLength, int hitTick, int cooldownTicks) {
            super(ImmutableMap.of(
                    MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT,
                    MemoryModuleType.ATTACK_COOLING_DOWN, MemoryStatus.REGISTERED
            ));

            this.animationLength = animationLength;
            this.hitTick = hitTick;
            this.cooldownTicks = cooldownTicks;
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, HippopotamusEntity entity) {
            return !entity.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN)
                    && HippopotamusAI.getAttackTarget(entity).filter(entity::isWithinMeleeAttackRange).isPresent();
        }

        @Override
        protected void start(ServerLevel level, HippopotamusEntity entity, long gameTime) {
            this.elapsedTicks = 0;
            this.dealtDamage = false;

            entity.startAttackAnimation();
        }

        @Override
        protected boolean canStillUse(ServerLevel level, HippopotamusEntity entity, long gameTime) {
            return this.elapsedTicks < this.animationLength
                    && HippopotamusAI.getAttackTarget(entity).filter(LivingEntity::isAlive).isPresent();
        }

        @Override
        protected void tick(ServerLevel level, HippopotamusEntity entity, long gameTime) {
            this.elapsedTicks++;

            HippopotamusAI.getAttackTarget(entity).ifPresent(target -> {
                entity.getLookControl().setLookAt(target, 30F, 30F);

                if (!this.dealtDamage && this.elapsedTicks == this.hitTick && entity.isWithinMeleeAttackRange(target))
                    this.dealtDamage = entity.doHurtTarget(target);
            });
        }

        @Override
        protected void stop(ServerLevel level, HippopotamusEntity entity, long gameTime) {
            entity.setAttacking(false);

            entity.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN, Boolean.TRUE, this.cooldownTicks);
        }
    }
}
