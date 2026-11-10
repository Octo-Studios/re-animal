package it.hurts.shatterbyte.reanimal.common.entity;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public interface SurfaceClimber {
    Direction getClimbingSide();

    void setClimbingSide(Direction side, Vec3 movement);
}
