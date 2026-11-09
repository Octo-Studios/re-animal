package it.hurts.shatterbyte.reanimal.client.renderer.glow_stick;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import it.hurts.shatterbyte.reanimal.client.model.glow_stick.GlowStickModel;
import it.hurts.shatterbyte.reanimal.client.renderer.SoftGlowLayer;
import it.hurts.shatterbyte.reanimal.client.renderer.MobPerch;
import it.hurts.shatterbyte.reanimal.common.entity.glow_stick.GlowStickEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class GlowStickRenderer extends GeoEntityRenderer<GlowStickEntity, EntityRenderState> {
    private static final AABB PICK_BOUNDS = new AABB(-1.3D / 16D, -0.3D / 16D, -1.3D / 16D, 1.3D / 16D, 10.3D / 16D, 1.3D / 16D);
    private static final DataTicket<Float> GLOW_YAW = DataTicket.create("reanimal_glow_yaw", Float.class);
    private static final DataTicket<Float> GLOW_HEIGHT = DataTicket.create("reanimal_glow_height", Float.class);
    private static final DataTicket<Float> GLOW_TUMBLE = DataTicket.create("reanimal_glow_tumble", Float.class);
    private static final DataTicket<Integer> GLOW_FACE = DataTicket.create("reanimal_glow_face", Integer.class);

    public GlowStickRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GlowStickModel());

        this.withRenderLayer(new SoftGlowLayer(this,
                new SoftGlowLayer.Glow("bb_main", 0.125F, 0.32F, 0.45F),
                new SoftGlowLayer.Glow("bb_main", 0.25F, 0.32F, 0.45F),
                new SoftGlowLayer.Glow("bb_main", 0.375F, 0.32F, 0.45F),
                new SoftGlowLayer.Glow("bb_main", 0.5F, 0.32F, 0.45F)));
    }

    @Override
    protected int getBlockLightLevel(GlowStickEntity entity, BlockPos pos) {
        return 15;
    }

    public static HitResult pickAttached(LocalPlayer player, Entity cameraEntity, float partialTick, HitResult original) {
        var client = Minecraft.getInstance();
        var from = cameraEntity.getEyePosition(partialTick);
        var blockHit = cameraEntity.pick(player.entityInteractionRange(), partialTick, false);
        var to = blockHit.getLocation();
        var closest = from.distanceToSqr(to);
        var result = original;

        for (var entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof GlowStickEntity stick) || !player.isWithinEntityInteractionRange(stick, 0D))
                continue;

            var host = stick.getAttachedEntity();

            if (host == null || host == cameraEntity && !client.gameRenderer.mainCamera().isDetached())
                continue;

            var renderer = (GlowStickRenderer) client.getEntityRenderDispatcher().getRenderer(stick);
            var hit = renderer.pick(stick, from, to, partialTick);

            if (hit == null)
                continue;

            var distance = from.distanceToSqr(hit);

            if (distance >= closest || original instanceof EntityHitResult entityHit && entityHit.getEntity() != host
                    && distance >= from.distanceToSqr(entityHit.getLocation()))
                continue;

            closest = distance;
            result = new EntityHitResult(stick, hit);
        }

        return result;
    }

    private Vec3 pick(GlowStickEntity entity, Vec3 from, Vec3 to, float partialTick) {
        var state = this.createRenderState(entity, partialTick);
        var attachment = ((GeoRenderState) state).getGeckolibData(MobPerch.TRANSFORM);

        if (attachment != null && attachment.transformDirection(0F, 1F, 0F, new Vector3f()).dot(
                new Vector3f((float) (to.x - from.x), (float) (to.y - from.y), (float) (to.z - from.z))) >= 0F)
            return null;

        var poseStack = new PoseStack();
        var info = RenderPassInfo.create((GeoRenderer) this, (GeoRenderState) state, poseStack, new CameraRenderState(), false);

        this.scaleModelForRender(info, 1F, 1F);
        this.adjustRenderPose(info);

        var inverse = new Matrix4f(poseStack.last().pose()).invert();
        var localFrom = inverse.transformPosition(new Vector3f((float) (from.x - state.x), (float) (from.y - state.y), (float) (from.z - state.z)));
        var localTo = inverse.transformPosition(new Vector3f((float) (to.x - state.x), (float) (to.y - state.y), (float) (to.z - state.z)));
        var hit = PICK_BOUNDS.clip(new Vec3(localFrom.x, localFrom.y, localFrom.z), new Vec3(localTo.x, localTo.y, localTo.z));

        if (hit.isEmpty())
            return null;

        var point = hit.get();
        var world = poseStack.last().pose().transformPosition(new Vector3f((float) point.x, (float) point.y, (float) point.z));

        return new Vec3(state.x + world.x, state.y + world.y, state.z + world.z);
    }

    @Override
    public void extractRenderState(GlowStickEntity entity, EntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        var geoState = (GeoRenderState) renderState;
        geoState.addGeckolibData(GLOW_YAW, entity.getRenderYaw(partialTick));
        geoState.addGeckolibData(GLOW_HEIGHT, entity.getRenderHeight(partialTick));
        geoState.addGeckolibData(GLOW_TUMBLE, entity.getRenderTumble(partialTick));
        geoState.addGeckolibData(GLOW_FACE, entity.getStuckFace() == null ? -1 : entity.getStuckFace().get3DDataValue());
        geoState.addGeckolibData(GlowStickModel.ITEM_NAME, BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()).getPath());
        geoState.addGeckolibData(MobPerch.TRANSFORM, MobPerch.extract(entity, renderState, partialTick));
    }

    @Override
    public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutoutCull(texture);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo renderPassInfo) {
        var geoState = (GeoRenderState) renderPassInfo.renderState();
        var attachment = geoState.getGeckolibData(MobPerch.TRANSFORM);

        if (attachment != null)
            renderPassInfo.poseStack().mulPose(attachment);

        var yaw = geoState.getOrDefaultGeckolibData(GLOW_YAW, 0F);
        var tumble = geoState.getOrDefaultGeckolibData(GLOW_TUMBLE, 90F);
        var height = geoState.getOrDefaultGeckolibData(GLOW_HEIGHT, 0.07F);

        var face = geoState.getOrDefaultGeckolibData(GLOW_FACE, -1);

        if (face >= 0) {
            var direction = Direction.from3DDataValue(face);
            var normal = direction.getUnitVec3i();

            if (direction == Direction.DOWN)
                renderPassInfo.poseStack().translate(0.0F, GlowStickEntity.SIZE + 0.01F - height, 0.0F);
            else
                renderPassInfo.poseStack().translate(normal.getX() * height, normal.getY() * height, normal.getZ() * height);

            switch (direction) {
                case DOWN -> renderPassInfo.poseStack().rotateDegrees(Axis.XP, 180.0F);
                case SOUTH -> renderPassInfo.poseStack().rotateDegrees(Axis.XP, 90.0F);
                case NORTH -> renderPassInfo.poseStack().rotateDegrees(Axis.XP, -90.0F);
                case EAST -> renderPassInfo.poseStack().rotateDegrees(Axis.ZP, -90.0F);
                case WEST -> renderPassInfo.poseStack().rotateDegrees(Axis.ZP, 90.0F);
                default -> {
                }
            }
        } else
            renderPassInfo.poseStack().translate(0.0F, height, 0.0F);

        renderPassInfo.poseStack().rotateDegrees(Axis.YP, yaw);
        renderPassInfo.poseStack().rotateDegrees(Axis.XP, tumble);
        renderPassInfo.poseStack().translate(0.0F, -0.3125F, 0.0F);
    }
}
