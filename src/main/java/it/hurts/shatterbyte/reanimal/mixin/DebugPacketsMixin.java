package it.hurts.shatterbyte.reanimal.mixin;

import it.hurts.shatterbyte.reanimal.network.PathfindingDebugPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugPackets.class)
public class DebugPacketsMixin {
    @Inject(method = "sendPathFindingPacket", at = @At("HEAD"))
    private static void sendPathfindingPacket(Level level, Mob mob, Path path, float maxDistanceToWaypoint, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            for (var serverPlauer : serverLevel.getAllEntities()) {
                if (serverPlauer instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new PathfindingDebugPacket(mob.getId(), path, maxDistanceToWaypoint));
                }
            }
        }
    }
}