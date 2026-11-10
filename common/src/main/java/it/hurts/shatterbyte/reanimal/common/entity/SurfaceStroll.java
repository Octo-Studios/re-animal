package it.hurts.shatterbyte.reanimal.common.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.pathfinder.Path;

import java.util.Map;
import java.util.Set;

public class SurfaceStroll extends Behavior<Mob> {
    private final float speed;
    private final int radius;
    private Path path;

    public SurfaceStroll(float speed, int radius) {
        super(Map.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT), 1);

        this.speed = speed;
        this.radius = radius;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Mob entity) {
        var navigation = (SurfaceNavigation<?>) entity.getNavigation();
        var random = entity.getRandom();

        for (var i = 0; i < 10; i++) {
            var cell = entity.blockPosition().offset(random.nextInt(this.radius * 2 + 1) - this.radius,
                    random.nextInt(this.radius * 2 + 1) - this.radius, random.nextInt(this.radius * 2 + 1) - this.radius);

            if (cell.equals(entity.blockPosition()) || navigation.getPoints(cell).isEmpty())
                continue;

            this.path = navigation.createSurfacePath(Set.of(cell), 0, this.radius * 2F);

            if (this.path != null)
                return true;
        }

        return false;
    }

    @Override
    protected void start(ServerLevel level, Mob entity, long timestamp) {
        entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.path.getTarget(), this.speed, 0));
        entity.getNavigation().moveTo(this.path, this.speed);
    }
}
