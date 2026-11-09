package it.hurts.shatterbyte.reanimal.common.entity.giraffe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.hurts.shatterbyte.reanimal.common.entity.BrainUtil;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSensorTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class GiraffeAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super GiraffeEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            ReAnimalSensorTypes.GIRAFFE_TEMPTATIONS,
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

    public static Brain.Provider<GiraffeEntity> brainProvider() {
        return BrainUtil.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<GiraffeEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initPanicActivity(brain);
        BrainUtil.addActivity(brain, Activity.WORK, 0, ImmutableList.of());

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<GiraffeEntity> brain) {
        BrainUtil.<GiraffeEntity>addActivity(brain,
                Activity.CORE,
                0,
                ImmutableList.of(
                        new EatLeavesBehavior(),
                        new GrazeCooldownBehavior(),
                        new Swim<>(0.8F),
                        new LookAtTargetSink(45, 90),
                        new MoveToTargetSink(),
                        new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
                        new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)
                )
        );
    }

    private static void initIdleActivity(Brain<GiraffeEntity> brain) {
        BrainUtil.<GiraffeEntity>addActivity(brain,
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(1, new GrazeBehavior()),
                        Pair.of(2, SetEntityLookTargetSometimes.create(net.minecraft.world.entity.EntityTypes.PLAYER, 6F, UniformInt.of(30, 60))),
                        Pair.of(3, new AnimalMakeLove(ReAnimalEntities.GIRAFFE, 1F, 1)),
                        Pair.of(
                                4,
                                new RunOne<>(
                                        ImmutableList.of(
                                                Pair.of(new FollowTemptation(entity -> 1.25F, entity -> entity.isBaby() ? 1D : 2D), 1),
                                                Pair.of(BabyFollowAdult.create(UniformInt.of(5, 10), 1.25F), 1)
                                        )
                                )
                        ),
                        Pair.of(5, new RandomLookAround(UniformInt.of(150, 250), 30F, 0F, 0F)),
                        Pair.of(
                                6,
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

    private static void initPanicActivity(Brain<GiraffeEntity> brain) {
        BrainUtil.<GiraffeEntity>addActivityAndRemoveMemoryWhenStopped(brain,
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

    public static void updateActivity(GiraffeEntity entity) {
        entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, entity.eatingLeaves ? Activity.WORK : Activity.IDLE));
    }

    public static Predicate<ItemStack> getTemptations() {
        return stack -> stack.is(ReAnimalTags.Items.GIRAFFE_FOOD);
    }

    public static class EatLeavesBehavior extends Behavior<GiraffeEntity> {
        private static final UniformInt EAT_DURATION = UniformInt.of(60, 100);
        private static final int SEARCH_INTERVAL = 20;

        private BlockPos target;
        private Path path;
        private int eatTime;
        private int eatDuration;
        private long nextSearchTick;

        public EatLeavesBehavior() {
            super(ImmutableMap.of(
                    MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT,
                    MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT
            ), 200 + EAT_DURATION.maxInclusive());
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, GiraffeEntity entity) {
            if (!entity.getBrain().isActive(Activity.IDLE) || entity.grazeCooldown > 0 || entity.isGrazing()
                    || entity.isInWater() || level.getGameTime() < this.nextSearchTick)
                return false;

            this.nextSearchTick = level.getGameTime() + SEARCH_INTERVAL;

            var eyePosition = entity.getEyePosition();
            var headPos = BlockPos.containing(eyePosition);
            var radius = Mth.ceil(entity.getAttributeValue(Attributes.FOLLOW_RANGE));
            var reach = Mth.ceil(entity.getBbWidth());
            var nodeOffset = (int) (entity.getBbWidth() + 1F) * 0.5D;

            for (var pos : BlockPos.withinBoxByManhattanDistance(headPos, radius, 0, radius)) {
                if (!level.getBlockState(pos).is(ReAnimalTags.Blocks.GIRAFFE_FOOD))
                    continue;

                if (isInReach(pos, eyePosition, reach)) {
                    this.target = pos.immutable();
                    this.path = null;
                    return true;
                }

                var destinations = new HashSet<BlockPos>();

                for (var groundPos : BlockPos.withinBoxByManhattanDistance(pos.atY(entity.blockPosition().getY()), reach, 0, reach)) {
                    if (isInReach(pos, new Vec3(groundPos.getX() + nodeOffset, eyePosition.y, groundPos.getZ() + nodeOffset), reach))
                        destinations.add(groundPos.immutable());
                }

                var path = entity.getNavigation().createPath(destinations, 0);

                if (path != null && path.canReach()) {
                    this.target = pos.immutable();
                    this.path = path;
                    return true;
                }
            }

            return false;
        }

        @Override
        protected void start(ServerLevel level, GiraffeEntity entity, long gameTime) {
            this.eatTime = 0;
            this.eatDuration = EAT_DURATION.sample(entity.getRandom());
            entity.eatingLeaves = true;
            updateActivity(entity);
            entity.getNavigation().moveTo(this.path, 1D);
        }

        @Override
        protected boolean canStillUse(ServerLevel level, GiraffeEntity entity, long gameTime) {
            var brain = entity.getBrain();

            return this.eatTime < this.eatDuration
                    && !entity.isInWater()
                    && !brain.hasMemoryValue(MemoryModuleType.IS_PANICKING)
                    && !brain.hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER)
                    && !brain.hasMemoryValue(MemoryModuleType.BREED_TARGET)
                    && level.getBlockState(this.target).is(ReAnimalTags.Blocks.GIRAFFE_FOOD);
        }

        @Override
        protected void tick(ServerLevel level, GiraffeEntity entity, long gameTime) {
            entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.target));

            if (!isInReach(this.target, entity.getEyePosition(), Mth.ceil(entity.getBbWidth()))) {
                this.eatTime = 0;

                if (!entity.getNavigation().moveTo(this.path, 1D))
                    this.doStop(level, entity, gameTime);

                return;
            }

            entity.getNavigation().stop();
            entity.getMoveControl().setWait();

            if (this.eatTime % 4 == 0) {
                var state = level.getBlockState(this.target);
                var sound = state.getSoundType();

                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        this.target.getX() + 0.5D, this.target.getY() + 0.5D, this.target.getZ() + 0.5D,
                        6, 0.3D, 0.3D, 0.3D, 0.05D);
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
        protected void stop(ServerLevel level, GiraffeEntity entity, long gameTime) {
            entity.eatingLeaves = false;
            entity.getNavigation().stop();
            entity.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
            this.nextSearchTick = gameTime + SEARCH_INTERVAL;

            if (this.eatTime == this.eatDuration)
                entity.grazeCooldown = GiraffeEntity.GRAZE_COOLDOWN.sample(entity.getRandom());
        }

        private static boolean isInReach(BlockPos pos, Vec3 eyePosition, int reach) {
            return Mth.floor(eyePosition.y) == pos.getY()
                    && Mth.square(eyePosition.x - pos.getX() - 0.5D) + Mth.square(eyePosition.z - pos.getZ() - 0.5D) <= reach * reach;
        }
    }

    public static class GrazeBehavior extends Behavior<GiraffeEntity> {
        public GrazeBehavior() {
            super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT), 1200);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, GiraffeEntity entity) {
            return entity.grazeCooldown == 0 && entity.canStartGrazing();
        }

        @Override
        protected void start(ServerLevel level, GiraffeEntity entity, long gameTime) {
            entity.startGrazingDown();
        }

        @Override
        protected boolean canStillUse(ServerLevel level, GiraffeEntity entity, long gameTime) {
            return entity.getGrazeState() != GiraffeEntity.GrazeState.STANDING;
        }

        @Override
        protected void tick(ServerLevel level, GiraffeEntity entity, long gameTime) {
            var brain = entity.getBrain();
            var grazeState = entity.getGrazeState();

            var shouldAbort = brain.hasMemoryValue(MemoryModuleType.IS_PANICKING)
                    || brain.hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER)
                    || entity.isInWater();

            if ((grazeState == GiraffeEntity.GrazeState.GRAZING_DOWN || grazeState == GiraffeEntity.GrazeState.GRAZING) && shouldAbort) {
                entity.startGettingUp();
                grazeState = entity.getGrazeState();
            }

            switch (grazeState) {
                case GRAZING_DOWN -> {
                    if (--entity.grazeTime <= 0) {
                        entity.grazeTime = GiraffeEntity.GRAZE_DURATION.sample(entity.getRandom());
                        entity.setGrazeState(GiraffeEntity.GrazeState.GRAZING);
                    }
                }
                case GRAZING -> {
                    entity.getNavigation().stop();

                    if (--entity.grazeTime <= 0)
                        entity.startGettingUp();
                }
                case GETTING_UP -> {
                    if (--entity.grazeTime <= 0)
                        entity.finishGettingUp();
                }
                default -> {
                }
            }
        }

        @Override
        protected void stop(ServerLevel level, GiraffeEntity entity, long gameTime) {
            if (entity.getGrazeState() != GiraffeEntity.GrazeState.STANDING)
                entity.finishGettingUp();
        }
    }

    public static class GrazeCooldownBehavior extends Behavior<GiraffeEntity> {
        public GrazeCooldownBehavior() {
            super(ImmutableMap.of(), 1200);
        }

        @Override
        protected boolean canStillUse(ServerLevel level, GiraffeEntity entity, long gameTime) {
            return true;
        }

        @Override
        protected void tick(ServerLevel level, GiraffeEntity entity, long gameTime) {
            if (entity.grazeCooldown > 0)
                entity.grazeCooldown--;
        }
    }
}
