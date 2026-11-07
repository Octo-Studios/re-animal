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
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class MobPerch {
    public static final DataTicket<Matrix4f> TRANSFORM = DataTicket.create("reanimal_mob_perch", Matrix4f.class);
    private static final Map<PerchingAnimalEntity, Attachment> ATTACHMENTS = new WeakHashMap<>();

    private MobPerch() {
    }

    public static Matrix4f extract(PerchingAnimalEntity animal, EntityRenderState animalState, float partialTick, float footSpan) {
        if (!(animal.getVehicle() instanceof LivingEntity host)) {
            ATTACHMENTS.remove(animal);

            return null;
        }

        var renderer = (EntityRenderer<LivingEntity, EntityRenderState>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(host);
        var state = renderer.createRenderState(host, partialTick);
        var poseStack = new PoseStack();
        var offset = renderer.getRenderOffset(state);
        poseStack.translate(offset.x, offset.y, offset.z);
        var attachment = getAttachment(animal, host);

        if (renderer instanceof GeoEntityRenderer<?, ?> geoRenderer) {
            var geo = (GeoRenderer) geoRenderer;
            var info = RenderPassInfo.create(geo, (GeoRenderState) state, poseStack, new CameraRenderState(), false);

            geo.scaleModelForRender(info, 1F, 1F);
            geo.adjustRenderPose(info);

            info.captureModelRenderPose();

            info.renderPosed(() -> {
                if (!attachment.bound) {
                    for (var bone : info.model().topLevelBones())
                        visitBone(bone, poseStack, attachment, footSpan * animal.getScale(), true);

                    attachment.bound = attachment.transform != null;
                    attachment.transform = null;
                }

                for (var bone : info.model().topLevelBones())
                    visitBone(bone, poseStack, attachment, footSpan * animal.getScale(), false);
            });
        } else {
            var collector = new SubmitNodeStorage() {
                private final SubmitNodeCollection collection = new SubmitNodeCollection(false, this.seeThrough()) {
                    @Override
                    public <S> void submitModel(Model<? super S> model, S modelState, PoseStack stack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, UvMapping uvMapping, int outlineColor) {
                        model.setupAnim(modelState);

                        if (!attachment.bound) {
                            var parts = model.root().getAllParts();
                            var poses = parts.stream().map(ModelPart::storePose).toList();

                            parts.forEach(ModelPart::resetPose);
                            visitModel(model.root(), stack, attachment, footSpan * animal.getScale());

                            for (var i = 0; i < parts.size(); i++)
                                parts.get(i).loadPose(poses.get(i));
                        } else
                            visitModel(model.root(), stack, attachment, footSpan * animal.getScale());
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

    private static Attachment getAttachment(PerchingAnimalEntity animal, LivingEntity host) {
        var attachment = ATTACHMENTS.get(animal);

        if (attachment == null || attachment.hostId != host.getId()) {
            attachment = new Attachment(host.getId());

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

            if (face < 0)
                return;

            var polygon = cube.polygons[face];
            var vertices = polygon.vertices();
            var center = new Vector3f();

            for (var vertex : vertices)
                center.add(vertex.worldX(), vertex.worldY(), vertex.worldZ());

            var first = new Vector3f(vertices[0].worldX(), vertices[0].worldY(), vertices[0].worldZ());
            var edge = new Vector3f(vertices[1].worldX(), vertices[1].worldY(), vertices[1].worldZ()).sub(first);
            var otherEdge = new Vector3f(vertices[3].worldX(), vertices[3].worldY(), vertices[3].worldZ()).sub(first);

            attachment.offer(path, cube, pose, center.div(vertices.length), new Vector3f(polygon.normal()), edge, otherEdge, footSpan);
        });
    }

    private static void visitBone(GeoBone bone, PoseStack poseStack, Attachment attachment, float footSpan, boolean referencePose) {
        poseStack.pushPose();

        var snapshot = bone.frameSnapshot;

        if (referencePose)
            bone.frameSnapshot = null;

        RenderUtil.prepMatrixForBone(poseStack, bone);
        bone.frameSnapshot = snapshot;

        if (bone instanceof CuboidGeoBone cuboid && (bone.frameSnapshot == null || !bone.frameSnapshot.isHidden())) {
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

                if (face >= 0) {
                    var quad = cube.quads()[face];
                    var vertices = quad.vertices();
                    var center = new Vector3f();

                    for (var vertex : vertices)
                        center.add(vertex.posX(), vertex.posY(), vertex.posZ());

                    var first = new Vector3f(vertices[0].posX(), vertices[0].posY(), vertices[0].posZ());
                    var edge = new Vector3f(vertices[1].posX(), vertices[1].posY(), vertices[1].posZ()).sub(first);
                    var otherEdge = new Vector3f(vertices[3].posX(), vertices[3].posY(), vertices[3].posZ()).sub(first);

                    attachment.offer(bone.name(), cube, pose, center.div(vertices.length), quad.normalVec(), edge, otherEdge, footSpan);
                }

                poseStack.popPose();
            }
        }

        if (bone.frameSnapshot == null || !bone.frameSnapshot.areChildrenHidden()) {
            for (var child : bone.children())
                visitBone(child, poseStack, attachment, footSpan, referencePose);
        }

        poseStack.popPose();
    }

    private static class Attachment {
        private final int hostId;
        private boolean bound;
        private String bone;
        private Object cube;
        private int priority = -1;
        private float height;
        private Matrix4f transform;

        private Attachment(int hostId) {
            this.hostId = hostId;
        }

        private void offer(String bone, Object cube, PoseStack.Pose pose, Vector3f center, Vector3f normal, Vector3f edge, Vector3f otherEdge, float footSpan) {
            var span = Math.min(pose.pose().transformDirection(edge).length(), pose.pose().transformDirection(otherEdge).length());

            if (span == 0F)
                return;

            var position = pose.pose().transformPosition(center);
            var name = bone.substring(bone.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
            var priority = (span >= footSpan ? 2 : 0)
                    + (name.contains("head") || name.contains("body") || name.contains("torso") || name.contains("back") || name.contains("chest") ? 1 : 0);

            if (!this.bound && (priority < this.priority || priority == this.priority && position.y <= this.height))
                return;

            var up = pose.normal().transform(normal, new Vector3f()).normalize();
            var rotation = new Quaternionf().rotationTo(new Vector3f(0F, 1F, 0F), normal);
            var forward = pose.pose().transformDirection(new Vector3f(0F, 0F, 1F).rotate(rotation)).normalize();
            var right = up.cross(forward, new Vector3f()).normalize();
            forward = right.cross(up, new Vector3f());

            this.bone = bone;
            this.cube = cube;
            this.priority = priority;
            this.height = position.y;
            this.transform = new Matrix4f(new Matrix3f(right, up, forward)).setTranslation(position);
        }
    }
}
