package it.hurts.shatterbyte.reanimal.common.entity.insect;

import it.hurts.shatterbyte.reanimal.common.entity.SurfaceNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Set;

final class InsectSurfaceNavigation extends SurfaceNavigation<FlyingInsectEntity> {
    private static final int CRAWL_RADIUS = 2;

    InsectSurfaceNavigation(FlyingInsectEntity insect, Level level) {
        super(insect, level, (state, side) -> true, 0.2D);
    }

    @Override
    protected boolean useGroundPath() {
        return false;
    }

    @Override
    protected boolean isClear(Vec3 from, Vec3 to, Direction side) {
        return (this.climber.isSeekingShelter() || this.climber.hasDryPath(from, to)) && super.isClear(from, to, side);
    }

    boolean seekShelter() {
        var range = (float) this.climber.getAttributeValue(Attributes.FOLLOW_RANGE);
        var path = this.createSurfacePath(range, point -> !this.level.isRainingAt(BlockPos.containing(point.position())));
        return this.moveTo(path, 1D);
    }

    void stroll() {
        var origin = this.climber.blockPosition();
        var targets = new ArrayList<BlockPos>();

        for (var pos : BlockPos.withinManhattan(origin, CRAWL_RADIUS)) {
            if (!pos.equals(origin) && !this.getPoints(pos).isEmpty())
                targets.add(pos.immutable());
        }

        Util.shuffle(targets, this.climber.getRandom());

        for (var target : targets) {
            if (this.moveTo(this.createSurfacePath(Set.of(target), 0, CRAWL_RADIUS * 2F), 1D))
                return;
        }
    }
}
