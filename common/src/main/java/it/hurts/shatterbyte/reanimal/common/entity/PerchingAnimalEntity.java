package it.hurts.shatterbyte.reanimal.common.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;

public abstract class PerchingAnimalEntity extends Animal {
    private static final double MAX_PERCH_SPEED = 0.3D;
    private static final double MAX_PERCH_VERTICAL_SPEED = 0.2D;
    private static final float MAX_PERCH_TURN = 35F;

    private int perchCooldown;
    private int perchTicks;

    protected PerchingAnimalEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public boolean isPerched() {
        return this.getVehicle() instanceof Mob;
    }

    @Override
    public boolean canControlVehicle() {
        return false;
    }

    public boolean canPerch() {
        return !this.isBaby() && !this.isPassenger() && !this.isVehicle() && this.hurtTime == 0
                && !this.isInWater() && !this.isInLove() && this.perchCooldown == 0;
    }

    public boolean canPerchOn(Mob mob) {
        return this.canStayPerched(mob) && mob.getPassengers().stream().noneMatch(PerchingAnimalEntity.class::isInstance);
    }

    protected boolean canStayPerched(Mob mob) {
        return !mob.isPassenger() && mob.getBbWidth() > this.getBbWidth() && mob.getBbHeight() > this.getBbHeight()
                && mob.isAlive() && !mob.isInWater() && !mob.isOnFire() && mob.getTarget() == null
                && mob.hurtTime == 0 && !mob.isSwinging() && !this.isTooRestlessToPerchOn(mob)
                && this.hurtTime == 0 && !this.isBaby() && !this.isInLove();
    }

    public Vec3 getPerchPosition(Mob mob) {
        return new Vec3(mob.getX(), mob.getBoundingBox().maxY, mob.getZ());
    }

    public void perchOn(Mob mob) {
        if (this.startRiding(mob, true, true)) {
            this.perchTicks = 400 + this.random.nextInt(400);
            this.getNavigation().stop();
            this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            this.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
            this.getBrain().eraseMemory(MemoryModuleType.PATH);
            this.setDeltaMovement(Vec3.ZERO);
            this.setPos(this.getPerchPosition(mob));
        }
    }

    public void delayPerching() {
        this.perchCooldown = 200 + this.random.nextInt(200);
    }

    private boolean isTooRestlessToPerchOn(Mob mob) {
        var movement = mob.position().subtract(mob.xo, mob.yo, mob.zo);

        return mob.isSprinting() || movement.horizontalDistanceSqr() > MAX_PERCH_SPEED * MAX_PERCH_SPEED
                || mob.getDeltaMovement().horizontalDistanceSqr() > MAX_PERCH_SPEED * MAX_PERCH_SPEED
                || Math.abs(movement.y) > MAX_PERCH_VERTICAL_SPEED
                || Math.abs(mob.getDeltaMovement().y) > MAX_PERCH_VERTICAL_SPEED
                || Math.abs(Mth.wrapDegrees(mob.yBodyRot - mob.yBodyRotO)) > MAX_PERCH_TURN
                || Math.abs(Mth.wrapDegrees(mob.yHeadRot - mob.yHeadRotO)) > MAX_PERCH_TURN;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide())
            return;

        if (this.perchCooldown > 0)
            --this.perchCooldown;

        if (this.getVehicle() instanceof Mob mob && (!this.canStayPerched(mob) || --this.perchTicks <= 0))
            this.leavePerch(mob);
    }

    @Override
    public void rideTick() {
        super.rideTick();

        if (this.getVehicle() instanceof Mob mob) {
            this.setPos(this.getPerchPosition(mob));
            this.setYRot(mob.yBodyRot);
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            this.setXRot(0F);
        }
    }

    protected void leavePerch(Mob mob) {
        var position = this.getPerchPosition(mob);
        var away = Vec3.directionFromRotation(0F, mob.yBodyRot + (this.random.nextBoolean() ? 90F : -90F));

        this.removeVehicle();
        this.setPos(position);
        this.delayPerching();
        this.setDeltaMovement(mob.getDeltaMovement().add(away.scale(0.3D)).add(0D, 0.35D, 0D));
        this.getNavigation().moveTo(position.x + away.x * 6D, position.y + 3D, position.z + away.z * 6D, 1D);
        this.onPerchTakeoff();
    }

    protected abstract void onPerchTakeoff();

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        this.perchTicks = 400 + this.random.nextInt(400);
    }
}
