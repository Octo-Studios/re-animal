package it.hurts.shatterbyte.reanimal.common.entity.starfish;

import it.hurts.shatterbyte.reanimal.common.entity.SurfaceNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;

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
}
