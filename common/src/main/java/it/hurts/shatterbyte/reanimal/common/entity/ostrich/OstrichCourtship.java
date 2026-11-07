package it.hurts.shatterbyte.reanimal.common.entity.ostrich;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

public class OstrichCourtship extends Behavior<OstrichEntity> {
    private static final int DANCE_DURATION_TICKS = 100;

    private OstrichEntity partner;
    private long danceEndsAt;

    public OstrichCourtship() {
        super(Map.of(
                MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT
        ), 200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, OstrichEntity entity) {
        if (!entity.isInLove() || entity.isVehicle() || entity.isPassenger())
            return false;

        this.partner = entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get()
                .findClosest(target -> target instanceof OstrichEntity ostrich && entity.canMate(ostrich)
                        && !ostrich.isPanicking() && !ostrich.isVehicle() && !ostrich.isPassenger()
                        && !ostrich.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET))
                .map(OstrichEntity.class::cast).orElse(null);

        return this.partner != null;
    }

    @Override
    protected void start(ServerLevel level, OstrichEntity entity, long timestamp) {
        this.danceEndsAt = 0L;
        entity.getBrain().setMemory(MemoryModuleType.BREED_TARGET, this.partner);
        this.partner.getBrain().setMemory(MemoryModuleType.BREED_TARGET, entity);

        BehaviorUtils.lockGazeAndWalkToEachOther(entity, this.partner, 1F, 1);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, OstrichEntity entity, long timestamp) {
        return this.partner.isAlive() && entity.canMate(this.partner)
                && !entity.isPanicking() && !this.partner.isPanicking()
                && !entity.isVehicle() && !entity.isPassenger() && !this.partner.isVehicle() && !this.partner.isPassenger()
                && BehaviorUtils.entityIsVisible(entity.getBrain(), this.partner)
                && (this.danceEndsAt == 0L || entity.onGround() && this.partner.onGround() && entity.closerThan(this.partner, 3D));
    }

    @Override
    protected void tick(ServerLevel level, OstrichEntity entity, long timestamp) {
        if (this.danceEndsAt == 0L) {
            BehaviorUtils.lockGazeAndWalkToEachOther(entity, this.partner, 1F, 1);

            if (!entity.closerThan(this.partner, 3D) || !entity.onGround() || !this.partner.onGround())
                return;

            this.danceEndsAt = timestamp + DANCE_DURATION_TICKS;
            entity.startCourtship();
            this.partner.startCourtship();
        }

        entity.getNavigation().stop();
        this.partner.getNavigation().stop();
        entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.partner.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        BehaviorUtils.lookAtEntity(entity, this.partner);
        BehaviorUtils.lookAtEntity(this.partner, entity);

        var yaw = (float) Mth.atan2(entity.getX() - this.partner.getX(), this.partner.getZ() - entity.getZ()) * Mth.RAD_TO_DEG;

        entity.setYRot(yaw);
        this.partner.setYRot(yaw + 180F);

        if (timestamp >= this.danceEndsAt) {
            entity.spawnChildFromBreeding(level, this.partner);
            this.doStop(level, entity, timestamp);
        }
    }

    @Override
    protected boolean timedOut(long timestamp) {
        return this.danceEndsAt == 0L && super.timedOut(timestamp);
    }

    @Override
    protected void stop(ServerLevel level, OstrichEntity entity, long timestamp) {
        entity.stopCourtship();
        this.partner.stopCourtship();
        this.partner = null;
    }
}
