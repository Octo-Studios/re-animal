package it.hurts.shatterbyte.reanimal.common.entity.butterfly;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.common.entity.BrainUtil;
import it.hurts.shatterbyte.reanimal.common.entity.PerchOnMob;
import it.hurts.shatterbyte.reanimal.common.entity.insect.RestOnSurface;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class ButterflyAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super ButterflyEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            ReAnimalSensorTypes.BUTTERFLY_TEMPTATIONS,
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

    public static Brain.Provider<ButterflyEntity> brainProvider() {
        return BrainUtil.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<ButterflyEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initPanicActivity(brain);
        BrainUtil.<ButterflyEntity>addActivity(brain, Activity.HIDE, 0, ImmutableList.of(
                new RandomLookAround(UniformInt.of(150, 250), 30F, 0F, 0F)
        ));

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<ButterflyEntity> brain) {
        BrainUtil.<ButterflyEntity>addActivity(brain,
                Activity.CORE,
                0,
                ImmutableList.of(
                        new Swim<>(0.8F),
                        new LookAtTargetSink(45, 90),
                        new MoveToTargetSink(),
                        new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
                        new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)
                )
        );
    }

    private static void initIdleActivity(Brain<ButterflyEntity> brain) {
        BrainUtil.<ButterflyEntity>addActivity(brain,
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, SetEntityLookTargetSometimes.create(net.minecraft.world.entity.EntityTypes.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(1, new AnimalMakeLove(ReAnimalEntities.BUTTERFLY, 1F, 1)),
                        Pair.of(
                                2,
                                new RunOne<>(
                                        ImmutableList.of(
                                                Pair.of(new FollowTemptation(entity -> 1.25F, entity -> entity.isBaby() ? 1D : 2D), 1),
                                                Pair.of(BabyFollowAdult.create(UniformInt.of(2, 4), 1.25F), 1)
                                        )
                                )
                        ),
                        Pair.of(3, new RandomLookAround(UniformInt.of(150, 250), 30F, 0F, 0F)),
                        Pair.of(
                                4,
                                new RunOne<>(
                                        ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                                        ImmutableList.of(
                                                Pair.of(new HoverNearFlower(), 5),
                                                Pair.of(RandomStroll.fly(1F), 1),
                                                Pair.of(new RestOnSurface(), 1),
                                                Pair.of(new PerchOnMob(), 1),
                                                Pair.of(SetWalkTargetFromLookTarget.create(1F, 3), 1),
                                                Pair.of(new DoNothing(30, 60), 1)
                                        )
                                )
                        )
                )
        );
    }

    private static void initPanicActivity(Brain<ButterflyEntity> brain) {
        BrainUtil.<ButterflyEntity>addActivityAndRemoveMemoryWhenStopped(brain,
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

    public static void updateActivity(ButterflyEntity entity) {
        if (!entity.isExposedToRain())
            entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, Activity.IDLE));
        else
            entity.getBrain().setActiveActivityIfPossible(Activity.HIDE);
    }

    public static Predicate<ItemStack> getTemptations() {
        return stack -> stack.is(ReAnimalTags.Items.BUTTERFLY_FOOD);
    }

    private static class HoverNearFlower extends Behavior<ButterflyEntity> {
        private BlockPos flower;
        private Vec3 flowerCenter;
        private Vec3 hoverPos;
        private float hoverAngle;
        private long nextSearchTick;

        public HoverNearFlower() {
            super(Map.of(
                    MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT
            ), 400, 600);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, ButterflyEntity butterfly) {
            if (butterfly.isExposedToRain() || butterfly.isInWater() || butterfly.isPassenger() || butterfly.hurtTime > 0
                    || !butterfly.getNavigation().isDone() || level.getGameTime() < this.nextSearchTick)
                return false;

            var range = Mth.ceil(butterfly.getAttributeValue(Attributes.FOLLOW_RANGE));

            for (var pos : BlockPos.withinManhattan(butterfly.blockPosition(), range)) {
                var state = level.getBlockState(pos);

                if (!state.is(BlockTags.FLOWERS) || !state.getFluidState().isEmpty() || level.getBlockState(pos.above()).is(BlockTags.FLOWERS))
                    continue;

                var shape = state.getShape(level, pos).bounds();
                var target = Vec3.atLowerCornerOf(pos).add(shape.getCenter().with(Direction.Axis.Y, shape.maxY));

                if (!butterfly.canFlyDirectlyTo(target))
                    continue;

                this.flower = pos.immutable();
                this.flowerCenter = target;
                this.hoverPos = target;

                return true;
            }

            this.nextSearchTick = level.getGameTime() + 100 + butterfly.getRandom().nextInt(100);

            return false;
        }

        @Override
        protected void start(ServerLevel level, ButterflyEntity butterfly, long timestamp) {
            butterfly.stopResting();
            butterfly.getNavigation().stop();
            butterfly.setHoveringNearFlower(true);
            this.hoverAngle = butterfly.getRandom().nextFloat() * Mth.TWO_PI;
        }

        @Override
        protected boolean canStillUse(ServerLevel level, ButterflyEntity butterfly, long timestamp) {
            return !butterfly.isExposedToRain() && !butterfly.isInWater() && !butterfly.isPassenger() && butterfly.hurtTime == 0
                    && butterfly.getNavigation().isDone() && this.hasRequiredMemories(butterfly)
                    && level.getBlockState(this.flower).is(BlockTags.FLOWERS) && level.getFluidState(this.flower).isEmpty()
                    && butterfly.canFlyDirectlyTo(this.hoverPos);
        }

        @Override
        protected void tick(ServerLevel level, ButterflyEntity butterfly, long timestamp) {
            butterfly.getLookControl().setLookAt(this.flowerCenter.x, this.flowerCenter.y, this.flowerCenter.z);

            if (butterfly.position().distanceToSqr(this.hoverPos) <= 0.01D) {
                this.hoverAngle += Mth.HALF_PI;

                var target = this.flowerCenter.add(
                        Mth.cos(this.hoverAngle) / 3D,
                        (1D + Mth.sin(this.hoverAngle * 2F)) * butterfly.getBbHeight() / 8D,
                        Mth.sin(this.hoverAngle) / 3D
                );

                if (!butterfly.canFlyDirectlyTo(target)) {
                    this.doStop(level, butterfly, timestamp);
                    return;
                }

                this.hoverPos = target;
            }

            butterfly.getMoveControl().setWantedPosition(this.hoverPos.x, this.hoverPos.y, this.hoverPos.z, 1D);
        }

        @Override
        protected void stop(ServerLevel level, ButterflyEntity butterfly, long timestamp) {
            butterfly.setHoveringNearFlower(false);
            butterfly.getMoveControl().setWait();
            this.nextSearchTick = timestamp + 100 + butterfly.getRandom().nextInt(100);
        }
    }
}
