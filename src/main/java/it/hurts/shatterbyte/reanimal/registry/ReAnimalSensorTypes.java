package it.hurts.shatterbyte.reanimal.registry;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.world.entity.ai.HedgehogAI;
import it.hurts.shatterbyte.reanimal.world.entity.HedgehogEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.MobSensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.sensing.TemptingSensor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ReAnimalSensorTypes {
    public static final DeferredRegister<SensorType<?>> SENSOR_TYPES = DeferredRegister.create(BuiltInRegistries.SENSOR_TYPE, ReAnimal.MODID);

    public static final DeferredHolder<SensorType<?>, SensorType<TemptingSensor>> HEDGEHOG_TEMPTATIONS = SENSOR_TYPES.register("hedgehog_temptations", () -> new SensorType<>(() -> new TemptingSensor(HedgehogAI.getTemptations())));
    public static final DeferredHolder<SensorType<?>, SensorType<MobSensor<HedgehogEntity>>> HEDGEHOG_SCARE_DETECTED = SENSOR_TYPES.register("hedgehog_scare_detected", () -> new SensorType<>(() -> new MobSensor<>(5, HedgehogEntity::isScaredBy, HedgehogEntity::canStayRolledUp, MemoryModuleType.DANGER_DETECTED_RECENTLY, 80)));

    public static void register(IEventBus modBus) {
        SENSOR_TYPES.register(modBus);
    }
}