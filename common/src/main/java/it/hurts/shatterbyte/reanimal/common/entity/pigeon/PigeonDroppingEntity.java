package it.hurts.shatterbyte.reanimal.common.entity.pigeon;

import it.hurts.shatterbyte.reanimal.init.ReAnimalParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class PigeonDroppingEntity extends ThrowableItemProjectile {
    private static final int MAX_LIFETIME = 200;
    private static final int MIN_SPLATS = 3;
    private static final int MAX_SPLATS = 5;
    private static final double SPLAT_MIN_DISTANCE = 0.12D;
    private static final double SPLAT_MAX_DISTANCE = 0.5D;
    private static final double SPLAT_LAYER_STEP = 0.004D;
    private static final double SPLAT_HEIGHT_TOLERANCE = 0.25D;

    public PigeonDroppingEntity(EntityType<? extends PigeonDroppingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.BONE_MEAL;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && (this.isInWater() || this.tickCount > MAX_LIFETIME))
            this.discard();
    }

    @Override
    protected void onHit(HitResult result) {
        if (!(this.level() instanceof ServerLevel level)) {
            this.discard();

            return;
        }

        if (result instanceof BlockHitResult blockHit && blockHit.getDirection() == Direction.UP)
            this.spawnSplats(level, blockHit);

        this.discard();
    }

    private void spawnSplats(ServerLevel level, BlockHitResult hit) {
        var location = hit.getLocation();
        var count = MIN_SPLATS + this.random.nextInt(MAX_SPLATS - MIN_SPLATS + 1);

        for (int i = 0; i < count; i++) {
            var angle = this.random.nextDouble() * Math.PI * 2D;
            var distance = i == 0 ? 0D : SPLAT_MIN_DISTANCE + this.random.nextDouble() * (SPLAT_MAX_DISTANCE - SPLAT_MIN_DISTANCE);
            var x = location.x + Math.cos(angle) * distance;
            var z = location.z + Math.sin(angle) * distance;
            var y = this.findSurface(level, x, location.y, z);

            if (Double.isNaN(y))
                continue;

            level.sendParticles(ReAnimalParticleTypes.PIGEON_SPLAT, x, y + 0.01D + i * SPLAT_LAYER_STEP, z, 1, 0D, 0D, 0D, 0D);
        }
    }

    private double findSurface(ServerLevel level, double x, double referenceY, double z) {
        var above = BlockPos.containing(x, referenceY + 0.02D, z);

        if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty() || !level.getFluidState(above).isEmpty())
            return Double.NaN;

        var below = above.below();
        var shape = level.getBlockState(below).getCollisionShape(level, below);

        if (shape.isEmpty())
            return Double.NaN;

        var top = below.getY() + shape.max(Direction.Axis.Y);

        return Math.abs(top - referenceY) <= SPLAT_HEIGHT_TOLERANCE ? top : Double.NaN;
    }
}
