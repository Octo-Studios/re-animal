package it.hurts.shatterbyte.reanimal.common.entity.penguin;

import it.hurts.shatterbyte.reanimal.common.entity.BrainUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Set;
import java.util.function.Predicate;

public class PenguinAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super PenguinEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            ReAnimalSensorTypes.PENGUIN_TEMPTATIONS,
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

    public static Brain.Provider<PenguinEntity> brainProvider() {
        return BrainUtil.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<PenguinEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initPanicActivity(brain);

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<PenguinEntity> brain) {
        BrainUtil.<PenguinEntity>addActivity(brain,
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

    private static void initIdleActivity(Brain<PenguinEntity> brain) {
        BrainUtil.<PenguinEntity>addActivity(brain,
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, SetEntityLookTargetSometimes.create(net.minecraft.world.entity.EntityTypes.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(1, new AnimalMakeLove(ReAnimalEntities.PENGUIN, 1F, 1)),
                        Pair.of(
                                2,
                                new RunOne<>(
                                        ImmutableList.of(
                                                Pair.of(new FollowTemptation(entity -> 1.25F, entity -> entity.isBaby() ? 1D : 2D), 1),
                                                Pair.of(BabyFollowAdult.create(UniformInt.of(2, 4), 1.25F), 1)
                                        )
                                )
                        ),
                        Pair.of(3, new HuntFish()),
                        Pair.of(4, new RandomLookAround(UniformInt.of(150, 250), 30F, 0F, 0F)),
                        Pair.of(5, new SwitchEnvironment()),
                        Pair.of(
                                6,
                                new RunOne<>(
                                        ImmutableMap.of(
                                                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                                                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT
                                        ),
                                        ImmutableList.of(
                                                Pair.of(RandomStroll.stroll(1F, false), 1),
                                                Pair.of(RandomStroll.swim(1.2F), 1),
                                                Pair.of(SetWalkTargetFromLookTarget.create(1F, 3), 1),
                                                Pair.of(new DoNothing(30, 60), 1)
                                        )
                                )
                        )
                )
        );
    }

    private static void initPanicActivity(Brain<PenguinEntity> brain) {
        BrainUtil.<PenguinEntity>addActivityAndRemoveMemoryWhenStopped(brain,
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

    public static void updateActivity(PenguinEntity entity) {
        entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, Activity.IDLE));
    }

    public static Predicate<ItemStack> getTemptations() {
        return stack -> stack.is(ReAnimalTags.Items.PENGUIN_FOOD);
    }

    public static class HuntFish extends Behavior<PenguinEntity> {
        private static final UniformInt HUNT_COOLDOWN = UniformInt.of(400, 800);
        private Entity target;
        private long nextHunt;
        private long nextAttack;

        public HuntFish() {
            super(ImmutableMap.of(
                    MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
                    MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED,
                    MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT,
                    MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.REGISTERED,
                    MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT
            ), 400);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, PenguinEntity penguin) {
            if (penguin.isPassenger() || penguin.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING))
                return false;

            this.target = null;

            var range = penguin.getAttributeValue(Attributes.FOLLOW_RANGE);
            var nearestDistance = range * range;

            for (var item : level.getEntitiesOfClass(ItemEntity.class, penguin.getBoundingBox().inflate(range),
                    item -> item.isAlive() && penguin.isFood(item.getItem()))) {
                var distance = penguin.distanceToSqr(item);

                if (distance <= nearestDistance) {
                    nearestDistance = distance;
                    this.target = item;
                }
            }

            if (this.target != null)
                return true;

            if (penguin.isBaby() || level.getGameTime() < this.nextHunt
                    || penguin.getBrain().hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER))
                return false;

            this.target = penguin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get()
                    .findClosest(entity -> entity instanceof AbstractFish && entity.isAlive() && entity.isInWater()).orElse(null);

            return this.target != null;
        }

        @Override
        protected void start(ServerLevel level, PenguinEntity penguin, long gameTime) {
            if (this.target instanceof AbstractFish fish)
                penguin.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, fish);

            penguin.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.target, 1.2F, 0));
            this.nextAttack = gameTime;
        }

        @Override
        protected boolean canStillUse(ServerLevel level, PenguinEntity penguin, long gameTime) {
            return this.target.isAlive() && penguin.closerThan(this.target, penguin.getAttributeValue(Attributes.FOLLOW_RANGE))
                    && !penguin.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING)
                    && !penguin.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET)
                    && (this.target instanceof ItemEntity || this.target.isInWater()
                    && !penguin.getBrain().hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER));
        }

        @Override
        protected void tick(ServerLevel level, PenguinEntity penguin, long gameTime) {
            penguin.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.target, true));
            penguin.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.target, 1.2F, 0));

            if (gameTime < this.nextAttack || !penguin.getSensing().hasLineOfSight(this.target))
                return;

            if (this.target instanceof ItemEntity item) {
                if (penguin.distanceToSqr(item) > 1.5D * 1.5D)
                    return;

                item.getItem().shrink(1);

                if (item.getItem().isEmpty())
                    item.discard();

                penguin.playSound(SoundEvents.GENERIC_EAT.value(), 1F, 1F);
                penguin.gameEvent(GameEvent.EAT);
                this.nextAttack = gameTime + 20;
            } else if (penguin.isInWater() && this.target instanceof AbstractFish fish && penguin.isWithinMeleeAttackRange(fish)) {
                penguin.doHurtTarget(level, fish);
                this.nextAttack = gameTime + 20;
            }
        }

        @Override
        protected void stop(ServerLevel level, PenguinEntity penguin, long gameTime) {
            penguin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            penguin.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            penguin.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
            penguin.getNavigation().stop();
            this.target = null;
            this.nextHunt = gameTime + HUNT_COOLDOWN.sample(penguin.getRandom());
        }
    }

    public static class SwitchEnvironment extends Behavior<PenguinEntity> {
        private static final UniformInt SWITCH_COOLDOWN = UniformInt.of(400, 800);
        private BlockPos target;
        private boolean toWater;
        private long nextSwitch;

        public SwitchEnvironment() {
            super(ImmutableMap.of(
                    MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT
            ), 400);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, PenguinEntity penguin) {
            if (penguin.isPassenger() || level.getGameTime() < this.nextSwitch
                    || penguin.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING))
                return false;

            this.nextSwitch = level.getGameTime() + SWITCH_COOLDOWN.sample(penguin.getRandom());
            this.toWater = !penguin.isInWater();
            var radius = Mth.ceil(penguin.getAttributeValue(Attributes.FOLLOW_RANGE));

            for (var i = 0; i < 40; i++) {
                var x = penguin.getBlockX() + penguin.getRandom().nextInt(radius * 2 + 1) - radius;
                var z = penguin.getBlockZ() + penguin.getRandom().nextInt(radius * 2 + 1) - radius;
                var surface = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
                var water = level.getFluidState(surface).is(FluidTags.WATER);

                if (water != this.toWater || !level.getFluidState(surface.above()).isEmpty()
                        || !level.getBlockState(surface.above()).getCollisionShape(level, surface.above()).isEmpty())
                    continue;

                if (!this.toWater && !level.getBlockState(surface).isFaceSturdy(level, surface, Direction.UP))
                    continue;

                var target = this.toWater ? surface : surface.above();
                var path = penguin.getNavigation().createPath(target, 0);

                if (path != null && path.canReach()) {
                    this.target = target;
                    return true;
                }
            }

            return false;
        }

        @Override
        protected void start(ServerLevel level, PenguinEntity penguin, long gameTime) {
            penguin.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.target, 1.2F, 0));
        }

        @Override
        protected boolean canStillUse(ServerLevel level, PenguinEntity penguin, long gameTime) {
            return penguin.isInWater() != this.toWater && penguin.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
                    && !penguin.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                    && !penguin.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING)
                    && !penguin.getBrain().hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER)
                    && !penguin.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET);
        }

        @Override
        protected void stop(ServerLevel level, PenguinEntity penguin, long gameTime) {
            penguin.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            penguin.getNavigation().stop();
            this.nextSwitch = gameTime + SWITCH_COOLDOWN.sample(penguin.getRandom());
        }
    }
}
