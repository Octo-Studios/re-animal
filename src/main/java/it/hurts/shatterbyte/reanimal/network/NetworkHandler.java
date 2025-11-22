package it.hurts.shatterbyte.reanimal.network;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber
public class NetworkHandler {
    @SubscribeEvent
    public static void onRegisterPayloadHandler(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ReAnimal.MODID)
                .versioned("1.0")
                .optional();

        registrar.playToClient(PathfindingDebugPacket.TYPE, PathfindingDebugPacket.STREAM_CODEC, PathfindingDebugPacket::handle);
    }

    public static <MSG extends CustomPacketPayload> void sendToServer(MSG message) {
        PacketDistributor.sendToServer(message);
    }

    public static <MSG extends CustomPacketPayload> void sendToClient(MSG message, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, message);
    }

    public static <MSG extends CustomPacketPayload> void sendToClientsTrackingEntity(MSG message, Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, message);
    }

    public static <MSG extends CustomPacketPayload> void sendToClientsTrackingEntityAndSelf(MSG message, Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, message);
    }

    public static <MSG extends CustomPacketPayload> void sendToClientsTrackingChunk(MSG message, ServerLevel level, ChunkPos chunkPos) {
        PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, message);
    }
}