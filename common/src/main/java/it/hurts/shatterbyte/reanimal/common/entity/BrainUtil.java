package it.hurts.shatterbyte.reanimal.common.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Set;

public class BrainUtil {
    public static <E extends LivingEntity> void addActivity(Brain<E> brain, Activity activity, int priorityStart, ImmutableList<? extends BehaviorControl<? super E>> behaviors) {
        brain.addActivity(activity, ActivityData.createPriorityPairs(priorityStart, behaviors), Set.of(), Set.of());
    }

    public static <E extends LivingEntity> void addActivity(Brain<E> brain, Activity activity, ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> behaviors) {
        brain.addActivity(activity, behaviors, Set.of(), Set.of());
    }

    public static <E extends LivingEntity> void addActivityAndRemoveMemoryWhenStopped(Brain<E> brain, Activity activity, int priorityStart, ImmutableList<? extends BehaviorControl<? super E>> behaviors, MemoryModuleType<?> memory) {
        brain.addActivity(activity, ActivityData.createPriorityPairs(priorityStart, behaviors), Set.of(Pair.of(memory, MemoryStatus.VALUE_PRESENT)), Set.of(memory));
    }

    public static <E extends LivingEntity> void addActivityWithConditions(Brain<E> brain, Activity activity, ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> behaviors, Set<Pair<MemoryModuleType<?>, MemoryStatus>> conditions) {
        brain.addActivity(activity, behaviors, conditions, Set.of());
    }

    public static <E extends LivingEntity> Brain.Provider<E> provider(java.util.Collection<? extends MemoryModuleType<?>> memories, java.util.Collection<? extends net.minecraft.world.entity.ai.sensing.SensorType<? extends net.minecraft.world.entity.ai.sensing.Sensor<? super E>>> sensors) {
        return Brain.provider(memories, sensors, entity -> java.util.List.of());
    }
}
