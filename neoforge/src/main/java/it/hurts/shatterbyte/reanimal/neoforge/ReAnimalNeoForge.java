package it.hurts.shatterbyte.reanimal.neoforge;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntityDataSerializers;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(ReAnimal.MODID)
public class ReAnimalNeoForge {
    public ReAnimalNeoForge(IEventBus modBus) {
        var initialized = new java.util.concurrent.atomic.AtomicBoolean();

        modBus.addListener(RegisterEvent.class, event -> {
            if (initialized.compareAndSet(false, true))
                ReAnimal.init();
        });

        var serializers = DeferredRegister.<EntityDataSerializer<?>>create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, ReAnimal.MODID);
        serializers.register("hedgehog_state", () -> ReAnimalEntityDataSerializers.HEDGEHOG_STATE);
        serializers.register(modBus);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            new ReAnimalNeoForgeClient(modBus);
        }
    }
}
