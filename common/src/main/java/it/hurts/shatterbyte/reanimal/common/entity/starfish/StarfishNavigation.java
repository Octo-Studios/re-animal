package it.hurts.shatterbyte.reanimal.common.entity.starfish;

import it.hurts.shatterbyte.reanimal.common.entity.SurfaceNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class StarfishNavigation extends SurfaceNavigation<StarfishEntity> {
    public StarfishNavigation(StarfishEntity starfish, Level level) {
        super(starfish, level, (state, side) -> true, 0.04D);
    }

    @Override
    protected boolean canNavigate() {
        return !this.climber.isPassenger() && !this.climber.isInLava();
    }

    @Override
    protected boolean useGroundPath() {
        return false;
    }

    @Override
    protected boolean canOccupy(BlockPos pos) {
        return super.canOccupy(pos) || this.level.getFluidState(pos).is(FluidTags.WATER);
    }

    @Override
    public Vec3 adjustSurfaceMovement(Vec3 from, Vec3 movement, Direction side) {
        var up = this.climber.getSurfaceRotation(1F).transformPositiveY(new Vector3f());
        var alignment = Math.min(1F, Math.abs(up.dot(side.getUnitVec3f())));
        var clearance = (this.climber.getBbWidth() - this.climber.getBbHeight()) * (1F - alignment);
        if (side.getAxis().isHorizontal())
            clearance *= 0.5F;

        var to = from.add(movement);
        var offset = this.getSupportOffset(to.relative(side, clearance), side);

        return offset.equals(Vec3.ZERO) || !this.isClear(from, to.add(offset), side) ? movement : movement.add(offset);
    }
}
