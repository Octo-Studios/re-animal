package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.giraffe.GiraffeAI;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogAI;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import it.hurts.shatterbyte.reanimal.common.entity.hippopotamus.HippopotamusAI;
import it.hurts.shatterbyte.reanimal.common.entity.butterfly.ButterflyAI;
import it.hurts.shatterbyte.reanimal.common.entity.kiwi.KiwiAI;
import it.hurts.shatterbyte.reanimal.common.entity.ostrich.OstrichAI;
import it.hurts.shatterbyte.reanimal.common.entity.pigeon.PigeonAI;
import it.hurts.shatterbyte.reanimal.common.entity.capybara.CapybaraAI;
import it.hurts.shatterbyte.reanimal.common.entity.seal.SealAI;
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
    public static final DeferredHolder<SensorType<?>, SensorType<TemptingSensor>> OSTRICH_TEMPTATIONS = SENSOR_TYPES.register("ostrich_temptations", () -> new SensorType<>(() -> new TemptingSensor(OstrichAI.getTemptations())));
    public static final DeferredHolder<SensorType<?>, SensorType<TemptingSensor>> KIWI_TEMPTATIONS = SENSOR_TYPES.register("kiwi_temptations", () -> new SensorType<>(() -> new TemptingSensor(KiwiAI.getTemptations())));
    public static final DeferredHolder<SensorType<?>, SensorType<TemptingSensor>> PIGEON_TEMPTATIONS = SENSOR_TYPES.register("pigeon_temptations", () -> new SensorType<>(() -> new TemptingSensor(PigeonAI.getTemptations())));
    public static final DeferredHolder<SensorType<?>, SensorType<TemptingSensor>> CAPYBARA_TEMPTATIONS = SENSOR_TYPES.register("capybara_temptations", () -> new SensorType<>(() -> new TemptingSensor(CapybaraAI.getTemptations())));
    public static final DeferredHolder<SensorType<?>, SensorType<TemptingSensor>> SEAL_TEMPTATIONS = SENSOR_TYPES.register("seal_temptations", () -> new SensorType<>(() -> new TemptingSensor(SealAI.getTemptations())));
    public static final DeferredHolder<SensorType<?>, SensorType<TemptingSensor>> HIPPOPOTAMUS_TEMPTATIONS = SENSOR_TYPES.register("hippopotamus_temptations", () -> new SensorType<>(() -> new TemptingSensor(HippopotamusAI.getTemptations())));
    public static final DeferredHolder<SensorType<?>, SensorType<TemptingSensor>> GIRAFFE_TEMPTATIONS = SENSOR_TYPES.register("giraffe_temptations", () -> new SensorType<>(() -> new TemptingSensor(GiraffeAI.getTemptations())));
    public static final DeferredHolder<SensorType<?>, SensorType<TemptingSensor>> BUTTERFLY_TEMPTATIONS = SENSOR_TYPES.register("butterfly_temptations", () -> new SensorType<>(() -> new TemptingSensor(ButterflyAI.getTemptations())));

    public static void register(IEventBus modBus) {
        SENSOR_TYPES.register(modBus);
    }
}
