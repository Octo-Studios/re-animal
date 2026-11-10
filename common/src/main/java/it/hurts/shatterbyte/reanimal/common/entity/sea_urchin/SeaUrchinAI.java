package it.hurts.shatterbyte.reanimal.common.entity.sea_urchin;

import it.hurts.shatterbyte.reanimal.common.entity.BrainUtil;
import it.hurts.shatterbyte.reanimal.common.entity.SurfaceMoveToTargetSink;
import it.hurts.shatterbyte.reanimal.common.entity.SurfaceStroll;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.function.Predicate;

public class SeaUrchinAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super SeaUrchinEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            ReAnimalSensorTypes.KIWI_TEMPTATIONS,
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

    public static Brain.Provider<SeaUrchinEntity> brainProvider() {
        return BrainUtil.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<SeaUrchinEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        BrainUtil.addActivity(brain, Activity.WORK, 0, ImmutableList.of());

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<SeaUrchinEntity> brain) {
        BrainUtil.<SeaUrchinEntity>addActivity(brain,
                Activity.CORE,
                0,
                ImmutableList.of(
                        new EatAlgae(),
                        new LookAtTargetSink(45, 90),
                        new SurfaceMoveToTargetSink(),
                        new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
                        new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)
                )
        );
    }

    private static void initIdleActivity(Brain<SeaUrchinEntity> brain) {
        BrainUtil.<SeaUrchinEntity>addActivity(brain,
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, SetEntityLookTargetSometimes.create(net.minecraft.world.entity.EntityTypes.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(1, new AnimalMakeLove(ReAnimalEntities.SEA_URCHIN, 1F, 1)),
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
                                                Pair.of(new SurfaceStroll(1F, 4), 1),
                                                Pair.of(new DoNothing(30, 60), 1)
                                        )
                                )
                        )
                )
        );
    }

    public static void updateActivity(SeaUrchinEntity entity) {
        entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, entity.isEating() ? Activity.WORK : Activity.IDLE));
    }

    public static Predicate<ItemStack> getTemptations() {
        return stack -> stack.is(ReAnimalTags.Items.SEA_URCHIN_FOOD);
    }

    private static class EatAlgae extends Behavior<SeaUrchinEntity> {
        private static final UniformInt EAT_DURATION = UniformInt.of(60, 100);
        private static final UniformInt EAT_COOLDOWN = UniformInt.of(200, 400);
        private static final int SEARCH_INTERVAL = 20;

        private BlockPos target;
        private Path path;
        private int eatTime;
        private int eatDuration;
        private long nextSearchTick;

        private EatAlgae() {
            super(ImmutableMap.of(
                    MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT
            ));
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, SeaUrchinEntity entity) {
            if (!entity.getBrain().isActive(Activity.IDLE) || !entity.isInWater() || entity.isPassenger()
                    || entity.isRolling() || level.getGameTime() < this.nextSearchTick)
                return false;

            this.nextSearchTick = level.getGameTime() + SEARCH_INTERVAL;

            var navigation = (SeaUrchinNavigation) entity.getNavigation();
            var radius = Mth.ceil(entity.getAttributeValue(Attributes.FOLLOW_RANGE));

            for (var pos : BlockPos.withinBoxByManhattanDistance(entity.blockPosition(), radius, radius, radius)) {
                if (!level.getBlockState(pos).is(ReAnimalTags.Blocks.SEA_URCHIN_FOOD))
                    continue;

                if (isInReach(pos, entity) && navigation.hasSupport(entity.position(), entity.getClimbingSide())) {
                    this.target = pos.immutable();
                    this.path = null;
                    return true;
                }

                var path = navigation.createSurfacePath(Set.of(pos.immutable()), 0, radius);

                if (path != null) {
                    this.target = pos.immutable();
                    this.path = path;
                    return true;
                }
            }

            return false;
        }

        @Override
        protected void start(ServerLevel level, SeaUrchinEntity entity, long gameTime) {
            this.eatTime = 0;
            this.eatDuration = EAT_DURATION.sample(entity.getRandom());
            entity.setEating(true);
            updateActivity(entity);
            entity.getNavigation().moveTo(this.path, 1D);
        }

        @Override
        protected boolean canStillUse(ServerLevel level, SeaUrchinEntity entity, long gameTime) {
            var brain = entity.getBrain();

            return entity.isEating() && this.eatTime < this.eatDuration
                    && entity.isInWater() && !entity.isPassenger() && !entity.isRolling()
                    && !brain.hasMemoryValue(MemoryModuleType.IS_PANICKING)
                    && !brain.hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER)
                    && !brain.hasMemoryValue(MemoryModuleType.BREED_TARGET)
                    && level.getBlockState(this.target).is(ReAnimalTags.Blocks.SEA_URCHIN_FOOD);
        }

        @Override
        protected boolean timedOut(long gameTime) {
            return false;
        }

        @Override
        protected void tick(ServerLevel level, SeaUrchinEntity entity, long gameTime) {
            var navigation = (SeaUrchinNavigation) entity.getNavigation();

            if (!isInReach(this.target, entity) || !navigation.hasSupport(entity.position(), entity.getClimbingSide())) {
                this.eatTime = 0;

                if (!navigation.moveTo(this.path, 1D))
                    this.doStop(level, entity, gameTime);

                return;
            }

            navigation.stop();
            entity.getMoveControl().setWait();
            entity.setDeltaMovement(Vec3.ZERO);

            if (this.eatTime % 4 == 0) {
                var state = level.getBlockState(this.target);
                var sound = state.getSoundType();

                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        this.target.getX() + 0.5D, this.target.getY() + 0.5D, this.target.getZ() + 0.5D,
                        4, 0.2D, 0.2D, 0.2D, 0.03D);
                level.playSound(null, this.target, sound.getHitSound(), SoundSource.NEUTRAL, sound.getVolume(), sound.getPitch());
            }

            if (++this.eatTime == this.eatDuration) {
                if (level.getGameRules().get(GameRules.MOB_GRIEFING))
                    level.destroyBlock(this.target, false, entity);

                entity.playSound(SoundEvents.GENERIC_EAT.value(), 1F, 1F);
                entity.gameEvent(GameEvent.EAT);
                entity.ate();
            }
        }

        @Override
        protected void stop(ServerLevel level, SeaUrchinEntity entity, long gameTime) {
            entity.setEating(false);
            entity.getNavigation().stop();
            this.nextSearchTick = gameTime + (this.eatTime == this.eatDuration ? EAT_COOLDOWN.sample(entity.getRandom()) : SEARCH_INTERVAL);
            updateActivity(entity);
        }

        private static boolean isInReach(BlockPos pos, SeaUrchinEntity entity) {
            return entity.getBoundingBox().distanceToSqr(Vec3.atCenterOf(pos)) <= 0.25D;
        }
    }
}
