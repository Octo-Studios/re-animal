package it.hurts.shatterbyte.reanimal.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Target;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.List;

public class PathfindingDebugPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PathfindingDebugPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    "the_black_blood",
                    "pathfinding_debug"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, PathfindingDebugPacket> STREAM_CODEC =
            CustomPacketPayload.codec(PathfindingDebugPacket::write, PathfindingDebugPacket::new);

    private final int entityId;
    private final Path path;
    private final float maxNodeDistance;

    public PathfindingDebugPacket(int entityId, Path path, float maxNodeDistance) {
        this.entityId = entityId;
        this.path = path;
        this.maxNodeDistance = maxNodeDistance;
    }

    public PathfindingDebugPacket(RegistryFriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        Path p = null;
        if (buf.readBoolean()) {
            p = Path.createFromStream(buf);
        }
        this.path = p;
        this.maxNodeDistance = buf.readFloat();
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(entityId);

        boolean w = true;
        Path localPath = this.path;

        if (localPath != null) {
            Node target = localPath.getNode(localPath.getNodeCount() - 1);
            Target targetNode = new Target(target.x, target.y, target.z);
            localPath.debugData = new Path.DebugData(
                    new Node[] {},
                    new Node[] {},
                    new HashSet<>(List.of(targetNode))
            );
        }

        if (localPath == null
                || localPath.debugData() == null
                || localPath.debugData().targetNodes().isEmpty()) {
            w = false;
        }

        buf.writeBoolean(w);
        if (w) {
            localPath.writeToStream(buf);
        }

        buf.writeFloat(maxNodeDistance);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
//        if (path != null) {
//            Minecraft.getInstance()
//                    .debugRenderer
//                    .pathfindingRenderer
//                    .addPath(entityId, path, maxNodeDistance);
//        }
    }
}