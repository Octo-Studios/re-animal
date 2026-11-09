package it.hurts.shatterbyte.reanimal.common.entity;

import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import org.joml.Vector3f;
import org.joml.Vector3fc;

public abstract class PerchingAnimalEntity extends Animal {
    private static final EntityDataAccessor<Integer> PERCH_SEED = SynchedEntityData.defineId(PerchingAnimalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3fc> PERCH_OFFSET = SynchedEntityData.defineId(PerchingAnimalEntity.class, EntityDataSerializers.VECTOR3);
    private static final double MAX_PERCH_SPEED = 0.3D;
    private static final double MAX_PERCH_VERTICAL_SPEED = 0.2D;
    private static final float MAX_PERCH_TURN = 35F;

    private int perchCooldown;
    private int perchTicks;

    protected PerchingAnimalEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(PERCH_SEED, 0);
        builder.define(PERCH_OFFSET, new Vector3f());
    }

    public int getPerchSeed() {
        return this.getEntityData().get(PERCH_SEED);
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
        return this.canStayPerched(host) && this.findPerchOffset(host) != null;
    }

    public static boolean isPerchOccupied(LivingEntity host) {
        return host.getPassengers().stream().anyMatch(passenger -> passenger instanceof PerchingAnimalEntity || passenger instanceof HedgehogEntity);
    }

    protected boolean canStayPerched(LivingEntity host) {
        return (host instanceof Mob || host instanceof Player) && !host.isSpectator() && !host.isPassenger()
                && host.getBbWidth() > this.getBbWidth() && host.getBbHeight() > this.getBbHeight()
                && host.isAlive() && !host.isInWater() && !host.isOnFire() && !(host instanceof Mob mob && mob.getTarget() != null)
                && host.hurtTime == 0 && !host.isSwinging() && !this.isTooRestlessToPerchOn(host)
                && this.hurtTime == 0 && !this.isBaby() && !this.isInLove();
    }

    public Vec3 getPerchPosition(LivingEntity host) {
        var offset = this.getVehicle() == host ? this.getEntityData().get(PERCH_OFFSET) : this.findPerchOffset(host);

        return new Vec3(host.getX(), host.getBoundingBox().maxY, host.getZ()).add(
                new Vec3(offset.x(), 0D, offset.z()).yRot(-host.yBodyRot * Mth.DEG_TO_RAD));
    }

    public void perchOn(LivingEntity host) {
        var offset = this.findPerchOffset(host);

        if (offset == null)
            return;

        this.getEntityData().set(PERCH_SEED, this.random.nextInt());
        this.getEntityData().set(PERCH_OFFSET, offset);

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

    public abstract float getPerchFootSpan();

    private Vector3fc findPerchOffset(LivingEntity host) {
        if (host.hasPassenger(passenger -> passenger instanceof HedgehogEntity))
            return null;

        var width = this.getBbWidth();
        var columns = Mth.floor((host.getBbWidth() - this.getPerchFootSpan() * this.getScale()) / width) + 1;
        var count = columns * columns;

        for (var i = 0; i < count; i++) {
            var index = (Math.floorMod(this.getPerchSeed(), count) + i) % count;
            var x = (index % columns - (columns - 1) * 0.5F) * width;
            var z = (index / columns - (columns - 1) * 0.5F) * width;
            var occupied = false;

            for (var passenger : host.getPassengers()) {
                if (passenger instanceof PerchingAnimalEntity perched) {
                    var offset = perched.getEntityData().get(PERCH_OFFSET);
                    var spacing = (width + perched.getBbWidth()) * 0.5F;

                    if (Math.abs(x - offset.x()) < spacing && Math.abs(z - offset.z()) < spacing) {
                        occupied = true;
                        break;
                    }
                }
            }

            if (!occupied)
                return new Vector3f(x, 0F, z);
        }

        return null;
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

    public void leavePerch(LivingEntity host) {
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
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);

        output.putInt("perch_seed", this.getPerchSeed());
        var offset = this.getEntityData().get(PERCH_OFFSET);
        output.store("perch_offset", Vec3.CODEC, new Vec3(offset.x(), offset.y(), offset.z()));
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        this.getEntityData().set(PERCH_SEED, input.getIntOr("perch_seed", 0));
        input.read("perch_offset", Vec3.CODEC).ifPresent(offset -> this.getEntityData().set(PERCH_OFFSET,
                new Vector3f((float) offset.x, (float) offset.y, (float) offset.z)));

        this.perchTicks = 400 + this.random.nextInt(400);
    }
}
