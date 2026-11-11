package it.hurts.shatterbyte.reanimal.common.entity;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;

public final class BodyBounds {
    private BodyBounds() {
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
