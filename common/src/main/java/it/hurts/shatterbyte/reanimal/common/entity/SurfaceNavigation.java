package it.hurts.shatterbyte.reanimal.common.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public class SurfaceNavigation<E extends Mob & SurfaceClimber> extends GroundPathNavigation {
    private static final double GRIP_REACH = 0.125D;

    protected final E climber;
    private final BiPredicate<BlockState, Direction> surfaces;
    private final double speedMultiplier;
    @Getter
    private Vec3 motion = Vec3.ZERO;

    public SurfaceNavigation(E climber, Level level, BiPredicate<BlockState, Direction> surfaces, double speedMultiplier) {
        super(climber, level);

        this.climber = climber;
        this.surfaces = surfaces;
        this.speedMultiplier = speedMultiplier;
        this.setCanPathToTargetsBelowSurface(true);
    }

    protected boolean canNavigate() {
        return !this.climber.isInLiquid() && !this.climber.isPassenger();
    }

    protected boolean useGroundPath() {
        return this.climber.getClimbingSide() == Direction.DOWN;
    }

    protected boolean canOccupy(BlockPos pos) {
        return this.level.getFluidState(pos).isEmpty();
    }

    protected boolean isPassable(BlockPos pos) {
        return false;
    }

    protected boolean canClimbWithoutSupport(AABB bounds) {
        return false;
    }

    protected boolean canFinishAt(Point point) {
        return true;
    }

    @Override
    protected boolean canUpdatePath() {
        return this.climber.getClimbingSide() != Direction.DOWN || super.canUpdatePath();
    }

    @Override
    protected Path createPath(Set<BlockPos> targets, int radiusOffset, boolean above, int reachRange, float maxPathLength) {
        if (this.isOnSurfacePath() && targets.contains(this.path.getTarget()))
            return this.path;

        var groundPath = this.useGroundPath() ? super.createPath(targets, radiusOffset, above, reachRange, maxPathLength) : null;

        if (!this.canNavigate() || targets.isEmpty())
            return groundPath;

        if (groundPath != null) {
            var end = groundPath.getEntityPosAtNode(this.climber, groundPath.getNodeCount() - 1);

            if (!this.canRest(new Vec3(end.x, this.getGroundY(end), end.z)))
                groundPath = null;
        }

        if (groundPath != null && groundPath.canReach())
            return groundPath;

        var surfacePath = this.createSurfacePath(targets, reachRange, maxPathLength);

        return surfacePath == null ? groundPath : surfacePath;
    }

    public Path createSurfacePath(Set<BlockPos> targets, int reachRange, float maxPathLength) {
        var target = targets.stream().min(Comparator.comparingDouble(pos -> pos.distSqr(this.climber.blockPosition()))).orElse(this.climber.blockPosition());
        var destination = Vec3.atCenterOf(target);

        return this.createSurfacePath(targets, reachRange, maxPathLength, this::canFinishAt,
                point -> targets.isEmpty() ? 0D : point.position().distanceTo(destination), Mth.ceil(maxPathLength * 16F));
    }

    protected Path createSurfacePath(float maxPathLength, Predicate<Point> destinationFilter) {
        return this.createSurfacePath(maxPathLength, destinationFilter, point -> 0D, Mth.ceil(maxPathLength * 16F));
    }

    protected Path createSurfacePath(float maxPathLength, Predicate<Point> destinationFilter, ToDoubleFunction<Point> priorityBias, int maxVisitedNodes) {
        return this.createSurfacePath(Set.of(), 0, maxPathLength, destinationFilter, priorityBias, maxVisitedNodes);
    }

    private Path createSurfacePath(Set<BlockPos> targets, int reachRange, float maxPathLength, Predicate<Point> destinationFilter,
                                   ToDoubleFunction<Point> priorityBias, int maxVisitedNodes) {
        var pointsByCell = new HashMap<BlockPos, List<Point>>();
        var entries = new HashMap<Point, Entry>();
        var closed = new HashSet<Point>();
        var pending = new PriorityQueue<Entry>(Comparator.comparingDouble(entry -> entry.cost() + priorityBias.applyAsDouble(entry.point())));
        var start = new Entry(new Point(this.climber.blockPosition(), this.climber.position(), this.climber.getClimbingSide()), null, 0D, null);

        for (var cell : BlockPos.betweenClosed(this.climber.blockPosition().offset(-1, -1, -1), this.climber.blockPosition().offset(1, 1, 1))) {
            for (var point : pointsByCell.computeIfAbsent(cell.immutable(), this::getPoints)) {
                var entry = this.connect(start, point);
                if (entry != null) {
                    entries.put(point, entry);
                    pending.add(entry);
                }
            }
        }

        while (!pending.isEmpty() && closed.size() < maxVisitedNodes) {
            var current = pending.remove();

            if (entries.get(current.point()) != current || !closed.add(current.point()))
                continue;

            if (destinationFilter.test(current.point())) {
                if (targets.isEmpty())
                    return this.buildPath(current, current.point().cell());

                for (var pos : targets) {
                    if (pos.distManhattan(current.point().cell()) <= reachRange)
                        return this.buildPath(current, pos);
                }
            }

            var cells = new ArrayList<BlockPos>();
            var unsupported = this.canClimbWithoutSupport(this.getBoundingBox(current.point().position(), current.point().side()));

            cells.add(current.point().cell());

            for (var direction : Direction.values()) {
                var adjacent = current.point().cell().relative(direction);

                if (direction.getAxis() != current.point().side().getAxis()) {
                    cells.add(adjacent);
                    cells.add(adjacent.relative(current.point().side()));
                } else if (unsupported || direction == Direction.DOWN && this.isPassable(adjacent))
                    cells.add(adjacent);
            }

            for (var cell : cells) {
                if (!cell.closerToCenterThan(this.climber.position(), maxPathLength))
                    continue;

                for (var point : pointsByCell.computeIfAbsent(cell.immutable(), this::getPoints)) {
                    if (closed.contains(point))
                        continue;

                    var entry = this.connect(current, point);
                    if (entry == null)
                        continue;

                    var previous = entries.get(point);
                    if (previous == null || entry.cost() < previous.cost()) {
                        entries.put(point, entry);
                        pending.add(entry);
                    }
                }
            }
        }

        return null;
    }

    @Override
    public boolean isStableDestination(BlockPos pos) {
        var position = Vec3.atBottomCenterOf(pos);

        return this.canRest(new Vec3(position.x, this.getGroundY(position), position.z));
    }

    private Path buildPath(Entry end, BlockPos target) {
        var nodes = new ArrayList<Node>();

        for (var entry = end; entry.previous() != null; entry = entry.previous()) {
            nodes.add(new SurfaceNode(entry.point(), false));

            if (entry.corner() != null) {
                var previous = entry.previous().point();

                nodes.add(new SurfaceNode(new Point(previous.cell(), entry.corner(), previous.side()), true));
            }
        }

        Collections.reverse(nodes);

        return new Path(nodes, target, true);
    }

    @Override
    protected void trimPath() {
        if (!this.isOnSurfacePath())
            super.trimPath();
    }

    @Override
    public boolean moveTo(Path path, double speedModifier) {
        if (this.isOnSurfacePath() || (path != null && !path.isDone() && path.getNextNode() instanceof SurfaceNode))
            this.path = null;

        return super.moveTo(path, speedModifier);
    }

    @Override
    public void recomputePath() {
        if (!this.isOnSurfacePath()) {
            super.recomputePath();

            return;
        }

        if (this.level.getGameTime() - this.timeLastRecompute > 20L) {
            var target = this.path.getTarget();

            this.path = null;
            this.path = this.createPath(target, 0);
            this.timeLastRecompute = this.level.getGameTime();
            this.hasDelayedRecomputation = false;
        } else
            this.hasDelayedRecomputation = true;
    }

    @Override
    public void tick() {
        this.motion = Vec3.ZERO;

        if (this.hasDelayedRecomputation)
            this.recomputePath();

        if (!this.isOnSurfacePath()) {
            super.tick();

            return;
        }

        this.tick++;

        if (!this.canNavigate()) {
            this.stop();

            return;
        }

        while (!this.path.isDone() && this.climber.position().distanceToSqr(((SurfaceNode) this.path.getNextNode()).point.position()) <= MoveControl.MIN_SPEED_SQR) {
            var side = ((SurfaceNode) this.path.getNextNode()).point.side();

            if (side != this.climber.getClimbingSide() && this.isClear(this.climber.position(), this.climber.position(), side))
                this.climber.setClimbingSide(side, Vec3.ZERO);

            this.path.advance();
        }

        if (this.path.isDone()) {
            this.climber.setDeltaMovement(Vec3.ZERO);

            return;
        }

        var node = (SurfaceNode) this.path.getNextNode();
        var point = node.point;
        var delta = point.position().subtract(this.climber.position());
        var speed = this.climber.getAttributeValue(Attributes.MOVEMENT_SPEED) * this.speedModifier * this.speedMultiplier;
        var movement = delta.normalize().scale(Math.min(speed, delta.length()));
        var side = point.side();

        if (!node.corner && !this.canSupport(point.cell().relative(side), side)
                && !this.canClimbWithoutSupport(this.getBoundingBox(point.position(), side))) {
            this.stop();

            return;
        }

        var attachedSide = this.climber.getClimbingSide();
        var from = this.climber.position();
        var to = from.add(movement);
        var corner = node.corner || this.path.getPreviousNode() instanceof SurfaceNode previous && previous.corner;
        var canMove = this.isConnected(from, to, side) || corner && this.isClear(from, to, side);

        if (side != attachedSide && (!canMove || !this.hasSupport(from, side) && this.hasSupport(from, attachedSide))
                && (this.isConnected(from, to, attachedSide) || corner && this.isClear(from, to, attachedSide))) {
            side = attachedSide;
            canMove = true;
        }

        if (!canMove) {
            this.stop();

            return;
        }

        this.motion = movement;
        this.climber.setClimbingSide(side, movement);
        this.climber.setSpeed((float) speed);

        if (side == Direction.DOWN && movement.horizontalDistanceSqr() > 0D)
            this.climber.setYRot((float) Mth.atan2(-movement.x, movement.z) * Mth.RAD_TO_DEG);

        this.doStuckDetection(this.climber.position());
    }

    @Override
    public void stop() {
        super.stop();

        this.motion = Vec3.ZERO;
    }

    public boolean isOnSurfacePath() {
        return !this.isDone() && this.path.getNextNode() instanceof SurfaceNode;
    }

    public List<Point> getPoints(BlockPos cell) {
        var points = new ArrayList<Point>();
        var passable = this.isPassable(cell);

        for (var side : Direction.values()) {
            var supported = this.canSupport(cell.relative(side), side);

            if (!supported && !(side == Direction.DOWN && passable))
                continue;

            var center = Vec3.atCenterOf(cell);
            var bounds = this.getBoundingBox(center, side);
            var halfHeight = bounds.getYsize() * 0.5D;
            var extent = switch (side.getAxis()) {
                case X -> bounds.getXsize() * 0.5D;
                case Y -> halfHeight;
                case Z -> bounds.getZsize() * 0.5D;
            };
            var position = side == Direction.DOWN ? center.with(Direction.Axis.Y, supported ? this.getGroundY(center) : cell.getY())
                    : center.add(Vec3.atLowerCornerOf(side.getUnitVec3i()).scale(0.5D - extent)).subtract(0D, halfHeight, 0D);

            if (this.isClear(position, position, side) && ((side == Direction.DOWN && passable) || this.hasSupport(position, side))
                    && this.canOccupy(cell))
                points.add(new Point(cell.immutable(), position, side));
        }

        return points;
    }

    private boolean canSupport(BlockPos pos, Direction side) {
        var level = this.climber.level();

        if (!level.hasChunkAt(pos))
            return false;

        var state = level.getBlockState(pos);
        var shape = state.getCollisionShape(level, pos);

        return this.surfaces.test(state, side)
                && !this.climber.getType().isBlockDangerous(state)
                && (side == Direction.DOWN ? !shape.isEmpty() : Block.isFaceFull(shape, side.getOpposite()));
    }

    public boolean canRest(Vec3 position) {
        return this.hasSupport(position, Direction.DOWN) && this.isClear(position, position, Direction.DOWN)
                && !this.canClimbWithoutSupport(this.getBoundingBox(position, Direction.DOWN));
    }

    public boolean hasSupport(Vec3 position, Direction side) {
        return this.hasSupport(this.getBoundingBox(position, side), side);
    }

    private boolean hasSupport(AABB bounds, Direction side) {
        var probe = bounds.deflate(Shapes.EPSILON).move(side.getUnitVec3().scale(GRIP_REACH));
        var collisions = this.getSupportCollisions(bounds, side, probe);

        while (collisions.hasNext()) {
            if (!collisions.next().isEmpty())
                return true;
        }

        return false;
    }

    public Vec3 getSupportOffset(Vec3 position, Direction side) {
        var bounds = this.getBoundingBox(position, side);
        var axis = side.getAxis();
        var thickness = axis.choose(bounds.getXsize(), bounds.getYsize(), bounds.getZsize());
        var reach = GRIP_REACH + Math.max(0D, (this.climber.getBbWidth() - thickness) * 0.5D);
        var probe = bounds.deflate(Shapes.EPSILON).expandTowards(side.getUnitVec3().scale(reach));
        var collisions = this.getSupportCollisions(bounds, side, probe);
        var limit = reach * side.getAxisDirection().getStep();
        var offset = limit;

        while (collisions.hasNext())
            offset = collisions.next().collide(axis, bounds, offset);

        return offset == limit ? Vec3.ZERO : Vec3.ZERO.with(axis, offset);
    }

    private BlockCollisions<VoxelShape> getSupportCollisions(AABB bounds, Direction side, AABB probe) {
        return new BlockCollisions<>(this.climber.level(), CollisionContext.empty(), probe, false,
                (pos, shape) -> this.canSupport(pos, side) && (side != Direction.DOWN || shape.max(Direction.Axis.Y) <= bounds.minY + Shapes.EPSILON)
                        ? shape : Shapes.empty());
    }

    private Entry connect(Entry from, Point to) {
        var start = from.point();
        var corner = this.getCorner(start, to);
        if (corner == null && !this.isConnected(start.position(), to.position(), to.side())
                && (start.side().getAxis() == to.side().getAxis() || !this.isConnected(start.position(), to.position(), start.side())))
            return null;

        var distance = corner == null ? start.position().distanceTo(to.position())
                : start.position().distanceTo(corner) + corner.distanceTo(to.position());
        return new Entry(to, from, from.cost() + distance, corner);
    }

    private boolean isConnected(Vec3 from, Vec3 to, Direction orientation) {
        if (!this.isClear(from, to, orientation))
            return false;

        var steps = Math.max(1, Mth.ceil(from.distanceTo(to) / GRIP_REACH));

        for (var i = 0; i <= steps; i++) {
            var bounds = this.getBoundingBox(from.lerp(to, (double) i / steps), orientation);

            if (this.canClimbWithoutSupport(bounds))
                continue;

            var supported = false;

            for (var side : Direction.values()) {
                if (this.hasSupport(bounds, side)) {
                    supported = true;

                    break;
                }
            }

            if (!supported)
                return false;
        }

        return true;
    }

    private Vec3 getCorner(Point from, Point to) {
        if (from.side().getAxis() == to.side().getAxis())
            return null;

        var first = from.position();
        var second = to.position();
        var sharedSupport = from.cell().relative(from.side()).equals(to.cell().relative(to.side()));
        var corner = switch (to.side().getAxis()) {
            case X -> new Vec3(second.x, first.y, first.z);
            case Y -> new Vec3(first.x, second.y, first.z);
            case Z -> new Vec3(first.x, first.y, second.z);
        };

        if (this.canTurnAt(from, corner, to, sharedSupport))
            return corner;

        corner = first.add(second).subtract(corner);
        return this.canTurnAt(from, corner, to, sharedSupport) ? corner : null;
    }

    private boolean canTurnAt(Point from, Vec3 corner, Point to, boolean sharedSupport) {
        if (sharedSupport)
            return this.isClear(from.position(), corner, from.side()) && this.isClear(corner, to.position(), to.side());

        return this.hasSupport(corner, from.side()) && this.hasSupport(corner, to.side())
                && this.isConnected(from.position(), corner, from.side()) && this.isConnected(corner, to.position(), to.side());
    }

    protected boolean isClear(Vec3 from, Vec3 to, Direction side) {
        var level = this.climber.level();
        var box = this.getBoundingBox(from, side).expandTowards(to.subtract(from)).deflate(Shapes.EPSILON);
        var collisions = new BlockCollisions<>(level, this.climber, box, false, (pos, shape) -> !this.isPassable(pos));

        while (collisions.hasNext()) {
            if (collisions.next())
                return false;
        }

        return level.noEntityCollision(this.climber, box) && level.noBorderCollision(this.climber, box);
    }

    private AABB getBoundingBox(Vec3 position, Direction side) {
        var halfWidth = this.climber.getBbWidth() * 0.5D;
        var bounds = new AABB(position.x - halfWidth, position.y, position.z - halfWidth,
                position.x + halfWidth, position.y + this.climber.getBbHeight(), position.z + halfWidth);

        return this.climber.getSurfaceBounds(bounds, side);
    }

    public record Point(BlockPos cell, Vec3 position, Direction side) {
    }

    private record Entry(Point point, Entry previous, double cost, Vec3 corner) {
    }

    private static class SurfaceNode extends Node {
        private final Point point;
        private final boolean corner;

        private SurfaceNode(Point point, boolean corner) {
            super(point.cell().getX(), point.cell().getY(), point.cell().getZ());

            this.point = point;
            this.corner = corner;
        }
    }
}
