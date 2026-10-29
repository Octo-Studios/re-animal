package it.hurts.shatterbyte.reanimal;

import it.hurts.shatterbyte.reanimal.init.*;

public class ReAnimal {
    public static final String MODID = "reanimal";

    public static void init() {
        ReAnimalMobEffects.init();
        ReAnimalPotions.init();
        ReAnimalBlocks.init();
        ReAnimalBlockEntities.init();
        ReAnimalEntities.init();
        ReAnimalItems.init();
        ReAnimalSoundEvents.init();
        ReAnimalParticleTypes.init();
        ReAnimalSensorTypes.init();
        ReAnimalEntityDataSerializers.init();
        ReAnimalCreativeTabs.init();
    }
}
