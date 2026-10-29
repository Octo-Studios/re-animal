package it.hurts.shatterbyte.reanimal.common.entity.pigeon.behaviors;

import it.hurts.shatterbyte.reanimal.common.entity.pigeon.PigeonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.IronGolem;

import java.util.Map;
import java.util.Optional;

public class PerchOnIronGolemBehavior extends Behavior<PigeonEntity> {
    private IronGolem targetGolem;
    private int cooldown = 0;

    public PerchOnIronGolemBehavior() {
        super(Map.of(
            MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
            MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
        ), 100, 300);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, PigeonEntity pigeon) {
        if (pigeon.isPassenger() || this.cooldown > 0) {
            if (this.cooldown > 0) this.cooldown--;
            return false;
        }

        Optional<NearestVisibleLivingEntities> visibleEntities = pigeon.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if (visibleEntities.isEmpty()) {
            return false;
        }

        Optional<IronGolem> nearestGolem = visibleEntities.get().findClosest(entity ->
            entity instanceof IronGolem golem && golem.getPassengers().isEmpty() && !golem.isBaby() && pigeon.distanceToSqr(golem) < 144.0D
        ).map(entity -> (IronGolem) entity);

        if (nearestGolem.isPresent()) {
            this.targetGolem = nearestGolem.get();
            return true;
        }

        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, PigeonEntity pigeon, long gameTime) {
        return this.targetGolem != null
                && this.targetGolem.isAlive()
                && !pigeon.isPassenger()
                && pigeon.distanceToSqr(this.targetGolem) < 256.0D;
    }

    @Override
    protected void start(ServerLevel level, PigeonEntity pigeon, long gameTime) {
        if (this.targetGolem != null) {
            pigeon.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetGolem, 1.25F, 1));
            pigeon.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new PositionTracker(this.targetGolem.position(), true));
        }
    }

    @Override
    protected void stop(ServerLevel level, PigeonEntity pigeon, long gameTime) {
        this.targetGolem = null;
        this.cooldown = 200; // Cooldown ticks before looking for another golem
        pigeon.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pigeon.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    protected void tick(ServerLevel level, PigeonEntity pigeon, long gameTime) {
        if (this.targetGolem == null) return;

        pigeon.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new PositionTracker(this.targetGolem.position(), true));
        pigeon.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetGolem, 1.25F, 1));

        if (pigeon.distanceToSqr(this.targetGolem) < 3.0D) {
            pigeon.startRiding(this.targetGolem, true);
            this.stop(level, pigeon, gameTime);
        }
    }
}
