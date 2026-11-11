package it.hurts.shatterbyte.reanimal.common.entity;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;

public final class BodyBounds {
    private BodyBounds() {
    }

    public static AABB alongYaw(AABB bounds, double lengthScale, float yaw) {
        var angle = yaw * Mth.DEG_TO_RAD;
        var sin = Math.abs(Mth.sin(angle));
        var cos = Math.abs(Mth.cos(angle));
        var halfWidth = bounds.getXsize() * 0.5D;

        return bounds.inflate(halfWidth * (cos + sin * lengthScale - 1D), 0D,
                halfWidth * (sin + cos * lengthScale - 1D));
    }

    public static AABB fitAlongYaw(Entity entity, AABB bounds, double lengthScale) {
        var elongated = alongYaw(bounds, lengthScale, entity.getYRot());

        return entity.level().noBlockCollision(entity, elongated) ? elongated : bounds;
    }

    public static AABB onSurface(AABB bounds, Direction side) {
        if (side.getAxis() == Direction.Axis.Y)
            return bounds;

        var width = bounds.getXsize();
        var inset = (width - bounds.getYsize()) * 0.5D;

        return new AABB(bounds.minX + (side.getAxis() == Direction.Axis.X ? inset : 0D), bounds.minY,
                bounds.minZ + (side.getAxis() == Direction.Axis.Z ? inset : 0D),
                bounds.maxX - (side.getAxis() == Direction.Axis.X ? inset : 0D), bounds.minY + width,
                bounds.maxZ - (side.getAxis() == Direction.Axis.Z ? inset : 0D));
    }

    public static boolean isInWall(Entity entity) {
        return !entity.noPhysics && entity.level().collidesWithSuffocatingBlock(entity, entity.getBoundingBox().deflate(Shapes.EPSILON));
    }
}
