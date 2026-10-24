package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import net.minecraft.network.syncher.EntityDataSerializer;

public class ReAnimalEntityDataSerializers {
    public static final EntityDataSerializer<HedgehogEntity.HedgehogState> HEDGEHOG_STATE =
            EntityDataSerializer.forValueType(HedgehogEntity.HedgehogState.STREAM_CODEC);

    public static void init() {
    }
}
