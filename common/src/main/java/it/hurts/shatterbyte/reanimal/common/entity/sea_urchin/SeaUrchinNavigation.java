package it.hurts.shatterbyte.reanimal.common.entity.sea_urchin;

import it.hurts.shatterbyte.reanimal.common.entity.SurfaceNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;

public class SeaUrchinNavigation extends SurfaceNavigation<SeaUrchinEntity> {
    public SeaUrchinNavigation(SeaUrchinEntity urchin, Level level) {
        super(urchin, level, (state, side) -> side != Direction.UP, 1D);
    }

    @Override
    protected boolean canNavigate() {
        return !this.climber.isRolling() && !this.climber.isPassenger() && !this.climber.isInLava();
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
