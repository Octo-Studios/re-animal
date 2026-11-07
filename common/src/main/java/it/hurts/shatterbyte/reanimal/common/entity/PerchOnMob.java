package it.hurts.shatterbyte.reanimal.common.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

import java.util.Map;

public class PerchOnMob extends Behavior<PerchingAnimalEntity> {
    private LivingEntity target;

    public PerchOnMob() {
        super(Map.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT
        ), 400);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, PerchingAnimalEntity animal) {
        if (!animal.canPerch())
            return false;

        var candidates = level.getEntitiesOfClass(LivingEntity.class, animal.getBoundingBox().inflate(8D, 6D, 8D), animal::canPerchOn);

        candidates.removeIf(host -> !this.hasClearApproach(level, animal, host));

        if (candidates.isEmpty()) {
            animal.delayPerching();

            return false;
        }

        this.target = candidates.get(animal.getRandom().nextInt(candidates.size()));

        return true;
    }

    @Override
    protected void start(ServerLevel level, PerchingAnimalEntity animal, long timestamp) {
        animal.getNavigation().stop();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, PerchingAnimalEntity animal, long timestamp) {
        return animal.canPerch() && animal.canPerchOn(this.target)
                && !animal.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING)
                && !animal.getBrain().hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER)
                && !animal.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET)
                && this.hasClearApproach(level, animal, this.target);
    }

    @Override
    protected void tick(ServerLevel level, PerchingAnimalEntity animal, long timestamp) {
        var position = animal.getPerchPosition(this.target);

        if (animal.position().distanceToSqr(position) <= animal.getBbWidth() * animal.getBbWidth()) {
            animal.perchOn(this.target);
            this.doStop(level, animal, timestamp);
        } else
            animal.getMoveControl().setWantedPosition(position.x, position.y, position.z, 1D);
    }

    @Override
    protected void stop(ServerLevel level, PerchingAnimalEntity animal, long timestamp) {
        if (!animal.isPerched()) {
            animal.getNavigation().stop();
            animal.delayPerching();
        }

        this.target = null;
    }

    private boolean hasClearApproach(ServerLevel level, PerchingAnimalEntity animal, LivingEntity host) {
        var position = animal.getPerchPosition(host);

        return level.noCollision(animal, animal.getBoundingBox().move(position.subtract(animal.position())))
                && level.clip(new ClipContext(animal.getEyePosition(), position, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, animal)).getType() == HitResult.Type.MISS;
    }
}
