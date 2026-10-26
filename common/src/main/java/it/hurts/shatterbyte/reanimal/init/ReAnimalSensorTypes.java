package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.butterfly.ButterflyAI;
import it.hurts.shatterbyte.reanimal.common.entity.capybara.CapybaraAI;
import it.hurts.shatterbyte.reanimal.common.entity.crocodile.CrocodileAI;
import it.hurts.shatterbyte.reanimal.common.entity.giraffe.GiraffeAI;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogAI;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import it.hurts.shatterbyte.reanimal.common.entity.hippopotamus.HippopotamusAI;
import it.hurts.shatterbyte.reanimal.common.entity.kiwi.KiwiAI;
import it.hurts.shatterbyte.reanimal.common.entity.meerkat.MeerkatAI;
import it.hurts.shatterbyte.reanimal.common.entity.ostrich.OstrichAI;
import it.hurts.shatterbyte.reanimal.common.entity.pigeon.PigeonAI;
import it.hurts.shatterbyte.reanimal.common.entity.seal.SealAI;
import it.hurts.shatterbyte.reanimal.common.entity.vulture.VultureAI;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.MobSensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.sensing.TemptingSensor;

public class ReAnimalSensorTypes {
    public static SensorType<TemptingSensor> HEDGEHOG_TEMPTATIONS;
    public static SensorType<MobSensor<HedgehogEntity>> HEDGEHOG_SCARE_DETECTED;
    public static SensorType<TemptingSensor> OSTRICH_TEMPTATIONS;
    public static SensorType<TemptingSensor> KIWI_TEMPTATIONS;
    public static SensorType<TemptingSensor> PIGEON_TEMPTATIONS;
    public static SensorType<TemptingSensor> CAPYBARA_TEMPTATIONS;
    public static SensorType<TemptingSensor> MEERKAT_TEMPTATIONS;
    public static SensorType<TemptingSensor> SEAL_TEMPTATIONS;
    public static SensorType<TemptingSensor> HIPPOPOTAMUS_TEMPTATIONS;
    public static SensorType<TemptingSensor> CROCODILE_TEMPTATIONS;
    public static SensorType<TemptingSensor> GIRAFFE_TEMPTATIONS;
    public static SensorType<TemptingSensor> BUTTERFLY_TEMPTATIONS;
    public static SensorType<TemptingSensor> VULTURE_TEMPTATIONS;

    public static void init() {
        HEDGEHOG_TEMPTATIONS = register("hedgehog_temptations", new SensorType<>(() -> new TemptingSensor(HedgehogAI.getTemptations())));
        HEDGEHOG_SCARE_DETECTED = register("hedgehog_scare_detected", new SensorType<>(() -> new MobSensor<>(5, HedgehogEntity::isScaredBy, HedgehogEntity::canStayRolledUp, MemoryModuleType.DANGER_DETECTED_RECENTLY, 80)));
        OSTRICH_TEMPTATIONS = register("ostrich_temptations", new SensorType<>(() -> new TemptingSensor(OstrichAI.getTemptations())));
        KIWI_TEMPTATIONS = register("kiwi_temptations", new SensorType<>(() -> new TemptingSensor(KiwiAI.getTemptations())));
        PIGEON_TEMPTATIONS = register("pigeon_temptations", new SensorType<>(() -> new TemptingSensor(PigeonAI.getTemptations())));
        CAPYBARA_TEMPTATIONS = register("capybara_temptations", new SensorType<>(() -> new TemptingSensor(CapybaraAI.getTemptations())));
        MEERKAT_TEMPTATIONS = register("meerkat_temptations", new SensorType<>(() -> new TemptingSensor(MeerkatAI.getTemptations())));
        SEAL_TEMPTATIONS = register("seal_temptations", new SensorType<>(() -> new TemptingSensor(SealAI.getTemptations())));
        HIPPOPOTAMUS_TEMPTATIONS = register("hippopotamus_temptations", new SensorType<>(() -> new TemptingSensor(HippopotamusAI.getTemptations())));
        CROCODILE_TEMPTATIONS = register("crocodile_temptations", new SensorType<>(() -> new TemptingSensor(CrocodileAI.getTemptations())));
        GIRAFFE_TEMPTATIONS = register("giraffe_temptations", new SensorType<>(() -> new TemptingSensor(GiraffeAI.getTemptations())));
        BUTTERFLY_TEMPTATIONS = register("butterfly_temptations", new SensorType<>(() -> new TemptingSensor(ButterflyAI.getTemptations())));
        VULTURE_TEMPTATIONS = register("vulture_temptations", new SensorType<>(() -> new TemptingSensor(VultureAI.getTemptations())));
    }

    @SuppressWarnings("unchecked")
    private static <T extends SensorType<?>> T register(String id, T sensorType) {
        return (T) Registry.register(BuiltInRegistries.SENSOR_TYPE, Identifier.fromNamespaceAndPath(ReAnimal.MODID, id), sensorType);
    }
}
