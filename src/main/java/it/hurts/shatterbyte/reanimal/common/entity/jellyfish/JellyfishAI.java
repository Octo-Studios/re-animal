package it.hurts.shatterbyte.reanimal.common.entity.jellyfish;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomLookAround;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class JellyfishAI {
    private static final ImmutableList<SensorType<? extends Sensor<? super JellyfishEntity>>> SENSOR_TYPES =
             ImmutableList.of(
                    SensorType.NEAREST_LIVING_ENTITIES,
                    SensorType.HURT_BY
            );

    private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
            MemoryModuleType.HURT_BY,
            MemoryModuleType.HURT_BY_ENTITY,
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryModuleType.PATH,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryModuleType.DANGER_DETECTED_RECENTLY
    );

    public static Brain.Provider<JellyfishEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<JellyfishEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    private static void initCoreActivity(Brain<JellyfishEntity> brain) {
        brain.addActivity(
                Activity.CORE,
                0,
                ImmutableList.of(
                        new LookAtTargetSink(45, 90),
                        new MoveToTargetSink()
                )
        );
    }

    private static void initIdleActivity(Brain<JellyfishEntity> brain) {
        brain.addActivity(
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, new JellyfishFlee()),
                        Pair.of(2, new JellyfishWander()),
                        Pair.of(3, new RandomLookAround(UniformInt.of(120, 200), 30F, 0F, 0F))
                )
        );
    }

    public static void updateActivity(JellyfishEntity entity) {
        entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.IDLE));
    }

    private static class JellyfishWander extends Behavior<JellyfishEntity> {
        public JellyfishWander() {
            super(ImmutableMap.of());
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, JellyfishEntity entity) {
            return true;
        }

        @Override
        protected boolean canStillUse(ServerLevel level, JellyfishEntity entity, long gameTime) {
            return true;
        }

        @Override
        protected void tick(ServerLevel level, JellyfishEntity jellyfish, long gameTime) {
            int idleTicks = jellyfish.getNoActionTime();

            if (idleTicks > 160) {
                jellyfish.setMovementVector(0.0F, 0.0F, 0.0F);
            } else if (jellyfish.getRandom().nextInt(80) == 0 || !jellyfish.isInWaterOrBubble() || !jellyfish.hasMovementVector()) {
                float angle = jellyfish.getRandom().nextFloat() * (float) (Math.PI * 2);
                float x = Mth.cos(angle) * 0.08F;
                float y = -0.14F + jellyfish.getRandom().nextFloat() * 0.28F;
                float z = Mth.sin(angle) * 0.08F;

                jellyfish.setMovementVector(x, y, z);
            }
        }
    }

    private static class JellyfishFlee extends Behavior<JellyfishEntity> {
        public JellyfishFlee() {
            super(ImmutableMap.of(MemoryModuleType.HURT_BY_ENTITY, MemoryStatus.VALUE_PRESENT));
        }

        @Override
        protected boolean canStillUse(ServerLevel level, JellyfishEntity jellyfish, long gameTime) {
            return jellyfish.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY_ENTITY);
        }

        @Override
        protected void tick(ServerLevel level, JellyfishEntity jellyfish, long gameTime) {
            var attacker = jellyfish.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY).orElse(null);

            if (attacker == null)
                return;

            Vec3 away = new Vec3(
                    jellyfish.getX() - attacker.getX(),
                    jellyfish.getY() - attacker.getY(),
                    jellyfish.getZ() - attacker.getZ()
            );

            BlockState state = level.getBlockState(BlockPos.containing(jellyfish.getX() + away.x, jellyfish.getY() + away.y, jellyfish.getZ() + away.z));
            FluidState fluidState = level.getFluidState(BlockPos.containing(jellyfish.getX() + away.x, jellyfish.getY() + away.y, jellyfish.getZ() + away.z));

            if (fluidState.is(FluidTags.WATER) || state.isAir()) {
                double distance = away.length();

                if (distance > 0.0) {
                    away = away.normalize();
                    double strength = 2.0;
                    if (distance > 5.0)
                        strength -= (distance - 5.0) / 5.0;

                    if (strength > 0.0)
                        away = away.scale(strength);
                }

                if (state.isAir())
                    away = away.subtract(0.0, away.y, 0.0);

                jellyfish.setMovementVector((float) away.x / 25.0F, (float) away.y / 25.0F, (float) away.z / 25.0F);
            }
        }
    }
}
