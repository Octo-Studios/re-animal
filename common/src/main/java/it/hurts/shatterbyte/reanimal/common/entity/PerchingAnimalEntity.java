package it.hurts.shatterbyte.reanimal.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
        return this.getVehicle() instanceof LivingEntity;
    }

    @Override
    public boolean canControlVehicle() {
        return false;
    }

    public boolean canPerch() {
        return !this.isBaby() && !this.isPassenger() && !this.isVehicle() && this.hurtTime == 0
                && !this.isInWater() && !this.isInLove() && this.perchCooldown == 0;
    }

    public boolean canPerchOn(LivingEntity host) {
        return this.canStayPerched(host) && host.getPassengers().stream().noneMatch(PerchingAnimalEntity.class::isInstance);
    }

    protected boolean canStayPerched(LivingEntity host) {
        return (host instanceof Mob || host instanceof Player) && !host.isSpectator() && !host.isPassenger()
                && host.getBbWidth() > this.getBbWidth() && host.getBbHeight() > this.getBbHeight()
                && host.isAlive() && !host.isInWater() && !host.isOnFire() && !(host instanceof Mob mob && mob.getTarget() != null)
                && host.hurtTime == 0 && !host.isSwinging() && !this.isTooRestlessToPerchOn(host)
                && this.hurtTime == 0 && !this.isBaby() && !this.isInLove();
    }

    public Vec3 getPerchPosition(LivingEntity host) {
        return new Vec3(host.getX(), host.getBoundingBox().maxY, host.getZ());
    }

    public void perchOn(LivingEntity host) {
        if (this.startRiding(host, true, true)) {
            this.perchTicks = 400 + this.random.nextInt(400);
            this.getNavigation().stop();
            this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            this.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
            this.getBrain().eraseMemory(MemoryModuleType.PATH);
            this.setDeltaMovement(Vec3.ZERO);
            this.setPos(this.getPerchPosition(host));
        }
    }

    public void delayPerching() {
        this.perchCooldown = 200 + this.random.nextInt(200);
    }

    private boolean isTooRestlessToPerchOn(LivingEntity host) {
        var movement = host.position().subtract(host.xo, host.yo, host.zo);

        return host.isSprinting() || movement.horizontalDistanceSqr() > MAX_PERCH_SPEED * MAX_PERCH_SPEED
                || host.getKnownMovement().horizontalDistanceSqr() > MAX_PERCH_SPEED * MAX_PERCH_SPEED
                || Math.abs(movement.y) > MAX_PERCH_VERTICAL_SPEED
                || Math.abs(host.getKnownMovement().y) > MAX_PERCH_VERTICAL_SPEED
                || Math.abs(Mth.wrapDegrees(host.yBodyRot - host.yBodyRotO)) > MAX_PERCH_TURN
                || Math.abs(Mth.wrapDegrees(host.yHeadRot - host.yHeadRotO)) > MAX_PERCH_TURN;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide())
            return;

        if (this.perchCooldown > 0)
            --this.perchCooldown;

        if (this.getVehicle() instanceof LivingEntity host && (!this.canStayPerched(host) || --this.perchTicks <= 0))
            this.leavePerch(host);
    }

    @Override
    public void rideTick() {
        super.rideTick();

        if (this.getVehicle() instanceof LivingEntity host) {
            this.setPos(this.getPerchPosition(host));
            this.setYRot(host.yBodyRot);
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            this.setXRot(0F);
        }
    }

    protected void leavePerch(LivingEntity host) {
        var position = this.getPerchPosition(host);
        var away = Vec3.directionFromRotation(0F, host.yBodyRot + (this.random.nextBoolean() ? 90F : -90F));
        var target = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(position.add(away.scale(6D))));

        this.removeVehicle();
        this.setPos(position);
        this.delayPerching();
        this.setDeltaMovement(Vec3.ZERO);
        this.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1D);
        this.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, 1F, 0));
        this.onPerchTakeoff();
    }

    protected abstract void onPerchTakeoff();

    @Override
    public boolean shouldBeSaved() {
        if (this.getVehicle() instanceof Player) {
            var reason = this.getRemovalReason();

            return reason == null || reason.shouldSave();
        }

        return super.shouldBeSaved();
    }

    @Override
    public boolean save(ValueOutput output) {
        return this.getVehicle() instanceof Player ? super.saveAsPassenger(output) : super.save(output);
    }

    @Override
    public boolean saveAsPassenger(ValueOutput output) {
        return !(this.getVehicle() instanceof Player) && super.saveAsPassenger(output);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        this.perchTicks = 400 + this.random.nextInt(400);
    }
}
