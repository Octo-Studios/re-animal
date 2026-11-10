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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiPredicate;

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

    protected boolean canClimbWithoutSupport(Vec3 position) {
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
        var pointsByCell = new HashMap<BlockPos, List<Point>>();
        var entries = new HashMap<Point, Entry>();
        var closed = new HashSet<Point>();
        var pending = new PriorityQueue<Entry>(Comparator.comparingDouble(entry -> entry.cost()
                + (targets.isEmpty() ? 0D : entry.point().position().distanceTo(destination))));

        for (var cell : BlockPos.betweenClosed(this.climber.blockPosition().offset(-1, -1, -1), this.climber.blockPosition().offset(1, 1, 1))) {
            for (var point : pointsByCell.computeIfAbsent(cell.immutable(), this::getPoints)) {
                if (this.isConnected(this.climber.position(), point.position())) {
                    var entry = new Entry(point, null, this.climber.position().distanceTo(point.position()), null);

                    entries.put(point, entry);
                    pending.add(entry);
                }
            }
        }

        while (!pending.isEmpty() && closed.size() < maxPathLength * 16F) {
            var current = pending.remove();

            if (entries.get(current.point()) != current || !closed.add(current.point()))
                continue;

            if (this.canFinishAt(current.point())) {
                if (targets.isEmpty())
                    return this.buildPath(current, current.point().cell());

                for (var pos : targets) {
                    if (pos.distManhattan(current.point().cell()) <= reachRange)
                        return this.buildPath(current, pos);
                }
            }

            var cells = new ArrayList<BlockPos>();
            var unsupported = this.canClimbWithoutSupport(current.point().position());

            cells.add(current.point().cell());

            for (var direction : Direction.values()) {
                if (direction.getAxis() != current.point().side().getAxis()) {
                    cells.add(current.point().cell().relative(direction));
                    cells.add(current.point().cell().relative(direction).relative(current.point().side()));
                } else if (unsupported)
                    cells.add(current.point().cell().relative(direction));
            }

            for (var cell : cells) {
                if (!cell.closerToCenterThan(this.climber.position(), maxPathLength))
                    continue;

                for (var point : pointsByCell.computeIfAbsent(cell.immutable(), this::getPoints)) {
                    if (closed.contains(point))
                        continue;

                    var connected = this.isConnected(current.point().position(), point.position());
                    var corner = connected ? null : this.getCorner(current.point(), point);

                    if (!connected && corner == null)
                        continue;

                    var cost = current.cost() + (corner == null ? current.point().position().distanceTo(point.position())
                            : current.point().position().distanceTo(corner) + corner.distanceTo(point.position()));
                    var previous = entries.get(point);

                    if (previous == null || cost < previous.cost()) {
                        var entry = new Entry(point, current, cost, corner);

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

        for (var entry = end; entry != null; entry = entry.previous()) {
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

        while (!this.path.isDone() && this.climber.position().distanceToSqr(((SurfaceNode) this.path.getNextNode()).point.position()) <= MoveControl.MIN_SPEED_SQR)
            this.path.advance();

        if (this.path.isDone()) {
            this.climber.setDeltaMovement(Vec3.ZERO);

            return;
        }

        var node = (SurfaceNode) this.path.getNextNode();
        var point = node.point;
        var delta = point.position().subtract(this.climber.position());
        var speed = this.climber.getAttributeValue(Attributes.MOVEMENT_SPEED) * this.speedModifier * this.speedMultiplier;
        var movement = delta.normalize().scale(Math.min(speed, delta.length()));
        var canMove = this.isConnected(this.climber.position(), this.climber.position().add(movement));

        if (!canMove && (node.corner || this.path.getPreviousNode() instanceof SurfaceNode previous && previous.corner))
            canMove = this.isClear(this.climber.position(), this.climber.position().add(movement));

        if ((!this.canSupport(point.cell().relative(point.side()), point.side()) && !this.canClimbWithoutSupport(point.position())) || !canMove) {
            this.stop();

            return;
        }

        this.motion = movement;
        this.climber.setClimbingSide(point.side(), movement);
        this.climber.setSpeed((float) speed);

        if (point.side() == Direction.DOWN && movement.horizontalDistanceSqr() > 0D)
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
            var support = cell.relative(side);

            if (!this.canSupport(support, side) && !(side == Direction.DOWN && passable))
                continue;

            var center = Vec3.atCenterOf(cell);
            var halfHeight = this.climber.getBbHeight() * 0.5D;
            var extent = side.getAxis() == Direction.Axis.Y ? halfHeight : this.climber.getBbWidth() * 0.5D;
            var position = center.add(Vec3.atLowerCornerOf(side.getUnitVec3i()).scale(0.5D - extent)).subtract(0D, halfHeight, 0D);

            if (this.isClear(position, position) && ((side == Direction.DOWN && passable) || this.hasSupport(position, side))
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
        return this.hasSupport(position, Direction.DOWN) && this.isClear(position, position) && !this.canClimbWithoutSupport(position);
    }

    public boolean hasSupport(Vec3 position, Direction side) {
        var box = this.getBoundingBox(position).deflate(Shapes.EPSILON);
        var offset = Vec3.atLowerCornerOf(side.getUnitVec3i()).scale(GRIP_REACH);
        var probe = box.move(offset);
        var collisions = new BlockCollisions<>(this.climber.level(), CollisionContext.empty(), probe, false,
                (pos, shape) -> this.canSupport(pos, side) && (side != Direction.DOWN || shape.max(Direction.Axis.Y) <= position.y + Shapes.EPSILON)
                        ? shape : Shapes.empty());

        while (collisions.hasNext()) {
            if (!collisions.next().isEmpty())
                return true;
        }

        return false;
    }

    private boolean isConnected(Vec3 from, Vec3 to) {
        if (!this.isClear(from, to))
            return false;

        var steps = Math.max(1, Mth.ceil(from.distanceTo(to) / GRIP_REACH));

        for (var i = 0; i <= steps; i++) {
            var position = from.lerp(to, (double) i / steps);

            if (this.canClimbWithoutSupport(position))
                continue;

            var supported = false;

            for (var side : Direction.values()) {
                if (this.hasSupport(position, side)) {
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

        if (this.isClear(first, corner) && this.isClear(corner, second)
                && (sharedSupport || this.hasSupport(corner, from.side()) && this.hasSupport(corner, to.side())))
            return corner;

        corner = first.add(second).subtract(corner);

        return this.isClear(first, corner) && this.isClear(corner, second)
                && (sharedSupport || this.hasSupport(corner, from.side()) && this.hasSupport(corner, to.side())) ? corner : null;
    }

    private boolean isClear(Vec3 from, Vec3 to) {
        var level = this.climber.level();
        var box = this.getBoundingBox(from).expandTowards(to.subtract(from)).deflate(Shapes.EPSILON);
        var collisions = new BlockCollisions<>(level, this.climber, box, false, (pos, shape) -> !this.isPassable(pos));

        while (collisions.hasNext()) {
            if (collisions.next())
                return false;
        }

        return level.noEntityCollision(this.climber, box) && level.noBorderCollision(this.climber, box);
    }

    private AABB getBoundingBox(Vec3 position) {
        return this.climber.getBoundingBox().move(position.subtract(this.climber.position()));
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
