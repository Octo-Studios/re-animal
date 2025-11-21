package it.hurts.shatterbyte.reanimal.registry;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.world.entity.hedgehog.HedgehogEntity;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ReAnimalEntityDataSerializers {
    public static final DeferredRegister<EntityDataSerializer<?>> ENTITY_DATA_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, ReAnimal.MODID);

    public static final Supplier<EntityDataSerializer<HedgehogEntity.HedgehogState>> HEDGEHOG_STATE = ENTITY_DATA_SERIALIZERS.register("hedgehog_state", () -> EntityDataSerializer.forValueType(HedgehogEntity.HedgehogState.STREAM_CODEC));

    public static void register(IEventBus modEventBus) {
        ENTITY_DATA_SERIALIZERS.register(modEventBus);
    }
}