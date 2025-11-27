package it.hurts.shatterbyte.reanimal;

import com.mojang.logging.LogUtils;
import it.hurts.shatterbyte.reanimal.init.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ReAnimal.MODID)
public class ReAnimal {
    public static final String MODID = "reanimal";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ReAnimal(IEventBus bus) {
        ReAnimalEntities.ENTITY_TYPES.register(bus);
        ReAnimalItems.ITEMS.register(bus);
        ReAnimalEntityDataSerializers.register(bus);
        ReAnimalSensorTypes.register(bus);
        ReAnimalCreativeTabs.register(bus);

        LOGGER.info("ReAnimal initialized");
    }
}
