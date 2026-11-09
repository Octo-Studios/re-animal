package it.hurts.shatterbyte.reanimal.client.renderer;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.reanimal.common.entity.PerchingAnimalEntity;
import it.hurts.shatterbyte.reanimal.common.entity.glow_stick.GlowStickEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.RandomSupport;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class MobPerch {
    public static final DataTicket<Matrix4f> TRANSFORM = DataTicket.create("reanimal_mob_perch", Matrix4f.class);
    private static final Map<Entity, Attachment> ATTACHMENTS = new WeakHashMap<>();

    private MobPerch() {
    }

    public static Matrix4f extract(LivingEntity animal, EntityRenderState animalState, float partialTick, float footSpan) {
        return extract(animal, animalState, partialTick, footSpan, null);
    }

    public static Matrix4f extract(LivingEntity animal, EntityRenderState animalState, float partialTick, float footSpan, String boneName) {
        if (!(animal.getVehicle() instanceof LivingEntity host)) {
            ATTACHMENTS.remove(animal);

            return null;
        }

        return extract(animal, host, animalState, partialTick, footSpan * animal.getScale(), boneName);
    }

    public static Matrix4f extract(GlowStickEntity stick, EntityRenderState stickState, float partialTick) {
        var host = stick.getAttachedEntity();

        if (host == null) {
            ATTACHMENTS.remove(stick);

            return null;
        }

        return extract(stick, host, stickState, partialTick, 0F, null);
    }

    private static Matrix4f extract(Entity animal, LivingEntity host, EntityRenderState animalState, float partialTick, float footSpan, String boneName) {
        var renderer = (EntityRenderer<LivingEntity, EntityRenderState>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(host);
        var state = renderer.createRenderState(host, partialTick);
        var poseStack = new PoseStack();
        var offset = renderer.getRenderOffset(state);
        poseStack.translate(offset.x, offset.y, offset.z);
        var attachment = getAttachment(animal, host, partialTick);

        if (renderer instanceof GeoEntityRenderer<?, ?> geoRenderer) {
            var geo = (GeoRenderer) geoRenderer;
            var info = RenderPassInfo.create(geo, (GeoRenderState) state, poseStack, new CameraRenderState(), false);

            geo.scaleModelForRender(info, 1F, 1F);
            geo.adjustRenderPose(info);

            info.captureModelRenderPose();

            info.renderPosed(() -> {
                if (!attachment.bound) {
                    for (var bone : info.model().topLevelBones())
                        visitBone(bone, poseStack, attachment, footSpan, attachment.point == null, boneName);

                    attachment.bound = attachment.transform != null;
                    attachment.transform = null;
                }

                for (var bone : info.model().topLevelBones())
                    visitBone(bone, poseStack, attachment, footSpan, false, boneName);
            });
        } else {
            var collector = new SubmitNodeStorage() {
                private final SubmitNodeCollection collection = new SubmitNodeCollection(false, this.seeThrough()) {
                    @Override
                    public <S> void submitModel(Model<? super S> model, S modelState, PoseStack stack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, UvMapping uvMapping, int outlineColor) {
                        model.setupAnim(modelState);

                        if (!attachment.bound && attachment.point == null) {
                            var parts = model.root().getAllParts();
                            var poses = parts.stream().map(ModelPart::storePose).toList();

                            parts.forEach(ModelPart::resetPose);
                            visitModel(model.root(), stack, attachment, footSpan);

                            for (var i = 0; i < parts.size(); i++)
                                parts.get(i).loadPose(poses.get(i));
                        } else
                            visitModel(model.root(), stack, attachment, footSpan);
                    }
                };

                @Override
                public SubmitNodeCollection order(int order) {
                    return this.collection;
                }
            };
            renderer.submit(state, poseStack, collector, new CameraRenderState());

            if (!attachment.bound && attachment.transform != null) {
                attachment.bound = true;
                attachment.transform = null;
                renderer.submit(state, poseStack, collector, new CameraRenderState());
            }
        }

        if (attachment.transform == null) {
            ATTACHMENTS.remove(animal);

            return null;
        }

        attachment.bound = true;

        var transform = new Matrix4f().translation((float) (state.x - animalState.x), (float) (state.y - animalState.y), (float) (state.z - animalState.z));

        return transform.mul(attachment.transform);
    }

    private static Attachment getAttachment(Entity animal, LivingEntity host, float partialTick) {
        var attachment = ATTACHMENTS.get(animal);
        var seed = animal instanceof PerchingAnimalEntity perching ? perching.getPerchSeed() : 0;

        if (attachment == null || attachment.hostId != host.getId() || attachment.seed != seed) {
            var point = animal.getPosition(partialTick).subtract(host.getPosition(partialTick));
            attachment = new Attachment(host.getId(), seed, animal instanceof PerchingAnimalEntity,
                    animal instanceof GlowStickEntity ? new Vector3f((float) point.x, (float) point.y, (float) point.z) : null);

            ATTACHMENTS.put(animal, attachment);
        }

        attachment.transform = null;

        return attachment;
    }

    private static void visitModel(ModelPart root, PoseStack poseStack, Attachment attachment, float footSpan) {
        root.visit(poseStack, (pose, path, cubeIndex, cube) -> {
            var part = root;

            if (!part.visible)
                return;

            for (var name : path.split("/")) {
                if (name.isEmpty())
                    continue;

                part = part.getChild(name);

                if (!part.visible)
                    return;
            }

            if (part.skipDraw || attachment.bound && (attachment.cube != cube || !attachment.bone.equals(path)))
                return;

            var face = -1;
            var upward = 0F;

            for (var i = 0; i < cube.polygons.length; i++) {
                var normal = pose.normal().transform(cube.polygons[i].normal(), new Vector3f()).normalize();

                if (normal.y > upward) {
                    upward = normal.y;
                    face = i;
                }
            }

            if (face < 0 && attachment.point == null)
                return;

            for (var i = 0; i < cube.polygons.length; i++) {
                if (attachment.point == null && i != face)
                    continue;

                var polygon = cube.polygons[i];
                var vertices = polygon.vertices();
                var center = new Vector3f();

                for (var vertex : vertices)
                    center.add(vertex.worldX(), vertex.worldY(), vertex.worldZ());

                var first = new Vector3f(vertices[0].worldX(), vertices[0].worldY(), vertices[0].worldZ());
                var edge = new Vector3f(vertices[1].worldX(), vertices[1].worldY(), vertices[1].worldZ()).sub(first);
                var otherEdge = new Vector3f(vertices[3].worldX(), vertices[3].worldY(), vertices[3].worldZ()).sub(first);

                attachment.offer(path, cube, pose, center.div(vertices.length), new Vector3f(polygon.normal()), edge, otherEdge, footSpan, i);
            }
        });
    }

    private static void visitBone(GeoBone bone, PoseStack poseStack, Attachment attachment, float footSpan, boolean referencePose, String boneName) {
        var snapshot = bone.frameSnapshot;

        if (snapshot != null && (snapshot.getScaleX() == 0F || snapshot.getScaleY() == 0F || snapshot.getScaleZ() == 0F))
            return;

        poseStack.pushPose();

        if (referencePose)
            bone.frameSnapshot = null;

        RenderUtil.prepMatrixForBone(poseStack, bone);
        bone.frameSnapshot = snapshot;

        if (bone instanceof CuboidGeoBone cuboid && (boneName == null || boneName.equals(bone.name()))
                && (bone.frameSnapshot == null || !bone.frameSnapshot.isHidden())) {
            for (var cube : cuboid.cubes) {
                if (cube.quads() == null || attachment.bound && (attachment.cube != cube || !attachment.bone.equals(bone.name())))
                    continue;

                poseStack.pushPose();

                cube.translateToPivotPoint(poseStack);
                cube.rotate(poseStack);
                cube.translateAwayFromPivotPoint(poseStack);

                var pose = poseStack.last();
                var face = -1;
                var upward = 0F;

                for (var i = 0; i < cube.quads().length; i++) {
                    var quad = cube.quads()[i];

                    if (quad == null)
                        continue;

                    var normal = pose.normal().transform(quad.normalVec()).normalize();

                    if (normal.y > upward) {
                        upward = normal.y;
                        face = i;
                    }
                }

                for (var i = 0; i < cube.quads().length; i++) {
                    if (attachment.point == null && i != face)
                        continue;

                    var quad = cube.quads()[i];

                    if (quad == null)
                        continue;

                    var vertices = quad.vertices();
                    var center = new Vector3f();

                    for (var vertex : vertices)
                        center.add(vertex.posX(), vertex.posY(), vertex.posZ());

                    var first = new Vector3f(vertices[0].posX(), vertices[0].posY(), vertices[0].posZ());
                    var edge = new Vector3f(vertices[1].posX(), vertices[1].posY(), vertices[1].posZ()).sub(first);
                    var otherEdge = new Vector3f(vertices[3].posX(), vertices[3].posY(), vertices[3].posZ()).sub(first);

                    attachment.offer(bone.name(), cube, pose, center.div(vertices.length), quad.normalVec(), edge, otherEdge, footSpan, i);
                }

                poseStack.popPose();
            }
        }

        if (bone.frameSnapshot == null || !bone.frameSnapshot.areChildrenHidden()) {
            for (var child : bone.children())
                visitBone(child, poseStack, attachment, footSpan, referencePose, boneName);
        }

        poseStack.popPose();
    }

    private static class Attachment {
        private final int hostId;
        private final int seed;
        private final boolean randomized;
        private final float yaw;
        private final Vector3f point;
        private boolean bound;
        private String bone;
        private Object cube;
        private int priority = -1;
        private long rank;
        private float height;
        private Matrix4f transform;
        private int face;
        private Vector3f anchor;
        private float distance = Float.POSITIVE_INFINITY;

        private Attachment(int hostId, int seed, boolean randomized, Vector3f point) {
            this.hostId = hostId;
            this.seed = seed;
            this.randomized = randomized;
            this.yaw = randomized ? RandomSource.create(seed).nextFloat() * Mth.TWO_PI : 0F;
            this.point = point;
        }

        private void offer(String bone, Object cube, PoseStack.Pose pose, Vector3f center, Vector3f normal, Vector3f edge, Vector3f otherEdge, float footSpan, int face) {
            var worldEdge = pose.pose().transformDirection(edge, new Vector3f());
            var worldOtherEdge = pose.pose().transformDirection(otherEdge, new Vector3f());
            var span = Math.min(worldEdge.length(), worldOtherEdge.length());

            if (span == 0F)
                return;

            var position = pose.pose().transformPosition(center, new Vector3f());
            var name = bone.substring(bone.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
            var priority = (span >= footSpan ? 2 : 0)
                    + (name.contains("head") || name.contains("body") || name.contains("torso") || name.contains("back") || name.contains("chest") ? 1 : 0);

            if (this.point != null) {
                if (this.bound) {
                    if (this.face != face)
                        return;
                } else {
                    var origin = new Vector3f(center).fma(-0.5F, edge).fma(-0.5F, otherEdge);
                    var delta = new Vector3f(this.point).sub(pose.pose().transformPosition(origin, new Vector3f()));
                    var anchor = new Vector3f(origin)
                            .fma(Mth.clamp(delta.dot(worldEdge) / worldEdge.lengthSquared(), 0F, 1F), edge)
                            .fma(Mth.clamp(delta.dot(worldOtherEdge) / worldOtherEdge.lengthSquared(), 0F, 1F), otherEdge);
                    var distance = pose.pose().transformPosition(anchor, new Vector3f()).distanceSquared(this.point);

                    if (distance >= this.distance)
                        return;

                    this.distance = distance;
                    this.anchor = anchor;
                    this.face = face;
                }

                position = pose.pose().transformPosition(this.anchor, new Vector3f());
            } else if (!this.bound) {
                var rank = this.randomized ? RandomSupport.mixStafford13(((long) this.seed << 32) ^ bone.hashCode()) : 0L;

                if (priority < this.priority || priority == this.priority && (rank < this.rank || rank == this.rank && position.y <= this.height))
                    return;

                this.rank = rank;
            }

            var up = pose.normal().transform(normal, new Vector3f()).normalize();
            var rotation = new Quaternionf().rotationTo(new Vector3f(0F, 1F, 0F), normal);
            var forward = pose.pose().transformDirection(new Vector3f(0F, 0F, 1F).rotate(rotation)).normalize();
            var right = up.cross(forward, new Vector3f()).normalize();
            forward = right.cross(up, new Vector3f());

            this.bone = bone;
            this.cube = cube;
            this.priority = priority;
            this.height = position.y;
            this.transform = new Matrix4f(new Matrix3f(right, up, forward)).setTranslation(position).rotateY(this.yaw);
        }
    }
}
