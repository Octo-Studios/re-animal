package it.hurts.shatterbyte.reanimal.common.entity.egg;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public abstract class ThrowableEggEntity extends ThrowableItemProjectile {
    protected ThrowableEggEntity(EntityType<? extends ThrowableEggEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected Item getDefaultItem() {
        return this.getEggItem();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        var level = this.level();

        if (!level.isClientSide()) {
            this.tryHatch((ServerLevel) level);

            level.broadcastEntityEvent(this, (byte) 3);

            this.discard();
        }
    }

    private void tryHatch(ServerLevel level) {
        if (this.random.nextInt(8) != 0)
            return;

        var hatchlingCount = this.random.nextInt(32) == 0 ? 4 : 1;
        var hatchlingType = this.getHatchlingType();

        for (int i = 0; i < hatchlingCount; i++) {
            var hatchling = hatchlingType.create(level);

            if (hatchling == null)
                continue;

            hatchling.setBaby(true);
            hatchling.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0F);

            level.addFreshEntity(hatchling);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            var particle = new ItemParticleOption(ParticleTypes.ITEM, this.getItem());

            for (int i = 0; i < 8; i++)
                this.level().addParticle(particle, this.getX(), this.getY(), this.getZ(), (this.random.nextDouble() - 0.5D) * 0.08D, (this.random.nextDouble() - 0.5D) * 0.08D, (this.random.nextDouble() - 0.5D) * 0.08D);
        } else
            super.handleEntityEvent(id);
    }

    @Override
    public void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0F);
    }

    protected abstract Item getEggItem();

    protected abstract EntityType<? extends AgeableMob> getHatchlingType();
}
