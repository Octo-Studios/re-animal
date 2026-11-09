package it.hurts.shatterbyte.reanimal.common.network;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.PerchingAnimalEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public record PerchTakeoffPayload(int entityId, int hostId, int seed) implements CustomPacketPayload {
    public static final Type<PerchTakeoffPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ReAnimal.MODID, "perch_takeoff"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PerchTakeoffPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PerchTakeoffPayload::entityId,
            ByteBufCodecs.VAR_INT, PerchTakeoffPayload::hostId,
            ByteBufCodecs.INT, PerchTakeoffPayload::seed,
            PerchTakeoffPayload::new);

    public void handle(ServerPlayer player) {
        if (player.level().getEntity(this.entityId) instanceof PerchingAnimalEntity animal
                && animal.getPerchSeed() == this.seed && player.getChunkTrackingView().contains(animal.chunkPosition())
                && animal.getVehicle() instanceof LivingEntity host && host.getId() == this.hostId)
            animal.leavePerch(host);
    }

    @Override
    public Type<PerchTakeoffPayload> type() {
        return TYPE;
    }
}
