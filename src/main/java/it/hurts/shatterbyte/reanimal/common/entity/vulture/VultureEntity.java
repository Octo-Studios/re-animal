package it.hurts.shatterbyte.reanimal.common.entity.vulture;

import com.mojang.serialization.Dynamic;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class VultureEntity extends Animal implements GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_GLIDING = SynchedEntityData.defineId(VultureEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.vulture.idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.vulture.fly");
    private static final RawAnimation GLIDING = RawAnimation.begin().thenLoop("animation.vulture.gliding");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.vulture.walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Getter
    @Setter
    private LivingEntity circlingTarget;
    @Setter
    private float circlingAngle;
    @Getter
    @Setter
    private long circlingStartTick;

    private int eggTime = this.random.nextInt(6000) + 6000;

    public boolean isGliding() {
        return this.entityData.get(DATA_GLIDING);
    }

    public void setGliding(boolean gliding) {
        this.entityData.set(DATA_GLIDING, gliding);
    }

    public VultureEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.moveControl = new VultureMoveControl(this);
        this.lookControl = new VultureLookControl(this);
        this.navigation = new FlyingPathNavigation(this, this.level());

        this.getNavigation().setCanFloat(true);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.tickEggLaying();
    }

    @Override
    protected void customServerAiStep() {
        var level = this.level();

        if (!(level instanceof ServerLevel serverLevel)) {
            super.customServerAiStep();

            return;
        }

        var profiler = level.getProfiler();

        profiler.push("vultureBrain");
        ((Brain<VultureEntity>) this.getBrain()).tick(serverLevel, this);
        profiler.pop();

        profiler.push("vultureActivityUpdate");
        VultureAI.updateActivity(this);
        profiler.pop();

        this.tickCircling();

        super.customServerAiStep();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(DATA_GLIDING, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        this.resetFallDistance();
    }

    @Override
    protected Brain.Provider<VultureEntity> brainProvider() {
        return VultureAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return VultureAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.VULTURE_FOOD);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.VULTURE.get().create(level);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainPredicate));
    }

    private void tickEggLaying() {
        if (this.level().isClientSide() || this.isBaby())
            return;

        if (--this.eggTime > 0)
            return;

        this.playSound(SoundEvents.CHICKEN_EGG, 1F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1F);
        this.spawnAtLocation(ReAnimalItems.VULTURE_EGG.get());
        this.gameEvent(GameEvent.ENTITY_PLACE, this);

        this.eggTime = this.random.nextInt(6000) + 6000;
    }

    private void tickCircling() {
        var target = this.circlingTarget;

        if (target == null || !target.isAlive()) {
            this.circlingTarget = null;
            this.setGliding(false);
            return;
        }

        if (this.getTarget() != null) {
            this.circlingTarget = null;
            this.setGliding(false);
            return;
        }

        if (this.distanceToSqr(target) > 64D * 64D) {
            this.circlingTarget = null;
            this.setGliding(false);
            return;
        }

        this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        var radius = 5.0D;
        var height = 12.0D;
        var angleSpeed = 0.08F;

        var targetPos = target.position();
        var centerX = targetPos.x;
        var centerY = targetPos.y + target.getBbHeight() + height;
        var centerZ = targetPos.z;

        var orbitX = centerX + radius * Math.cos(this.circlingAngle);
        var orbitZ = centerZ + radius * Math.sin(this.circlingAngle);
        var orbitY = centerY;

        var oldPos = this.position();
        var orbitPos = new Vec3(orbitX, orbitY, orbitZ);
        var toOrbit = orbitPos.subtract(oldPos);

        var threshold = 2.5D;

        if (toOrbit.lengthSqr() > threshold * threshold) {
            this.getNavigation().moveTo(orbitX, orbitY, orbitZ, 1.15D);

            return;
        }

        this.getNavigation().stop();

        this.circlingAngle += angleSpeed;

        orbitX = centerX + radius * Math.cos(this.circlingAngle);
        orbitZ = centerZ + radius * Math.sin(this.circlingAngle);
        orbitY = centerY;

        var nextPos = new Vec3(orbitX, orbitY, orbitZ);
        var move = nextPos.subtract(oldPos);

        var maxStep = 0.6D;
        var moveLenSq = move.lengthSqr();

        if (moveLenSq > maxStep * maxStep && moveLenSq > 1.0E-4D)
            move = move.normalize().scale(maxStep);

        this.setDeltaMovement(move);

        var delta = this.getDeltaMovement();

        if (delta.lengthSqr() > 1.0E-4D) {
            var yawRad = Math.atan2(delta.z, delta.x);
            var yawDeg = (float) (yawRad * 180F / Math.PI) - 90F;

            this.setYRot(yawDeg);
            this.yRotO = yawDeg;

            this.yBodyRot = yawDeg;
            this.yBodyRotO = yawDeg;

            this.setYHeadRot(yawDeg);
            this.yHeadRotO = yawDeg;

            this.setXRot(0F);
            this.xRotO = 0F;
        }

        this.setGliding(true);
    }

    private PlayState mainPredicate(AnimationState<VultureEntity> state) {
        var controller = state.getController();
        var entity = state.getAnimatable();

        if (state.isMoving()) {
            if (entity.onGround())
                controller.setAnimation(WALK);
            else {
                if (entity.isGliding())
                    controller.setAnimation(GLIDING);
                else
                    controller.setAnimation(FLY);
            }
        } else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    public static boolean checkVultureSpawnRules(EntityType<VultureEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return Animal.checkAnimalSpawnRules(type, level, reason, pos, random);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 10D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 16D)
                .add(Attributes.ATTACK_DAMAGE, 3D);
    }

    private static class VultureMoveControl extends FlyingMoveControl {
        private final VultureEntity mob;

        VultureMoveControl(VultureEntity mob) {
            super(mob, 15, false);
            this.mob = mob;
        }

        @Override
        public void tick() {
            if (this.operation != Operation.MOVE_TO) {
                var delta = mob.getDeltaMovement();
                mob.setDeltaMovement(delta.scale(0.9D));
                return;
            }

            this.operation = Operation.WAIT;

            var dx = this.wantedX - mob.getX();
            var dy = this.wantedY - mob.getY();
            var dz = this.wantedZ - mob.getZ();

            var distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < 1.0E-4D)
                return;

            var dist = Math.sqrt(distSq);
            var baseSpeed = mob.getAttributeValue(Attributes.FLYING_SPEED);

            var accel = 0.3D;
            var vyBoost = 0.15D;

            var vx = dx / dist * baseSpeed;
            var vy = dy / dist * baseSpeed + vyBoost;
            var vz = dz / dist * baseSpeed;

            var current = mob.getDeltaMovement();

            mob.setDeltaMovement(
                    Mth.lerp(accel, current.x, vx),
                    Mth.lerp(accel, current.y, vy),
                    Mth.lerp(accel, current.z, vz)
            );
        }
    }

    private static class VultureLookControl extends LookControl {
        private final VultureEntity vulture;

        VultureLookControl(VultureEntity vulture) {
            super(vulture);
            this.vulture = vulture;
        }

        @Override
        public void tick() {
            var mob = this.vulture;
            var maxYaw = 10.0F;
            var maxPitch = 20.0F;

            var yRot = mob.getYRot();
            var xRot = mob.getXRot();

            double lookX;
            double lookY;
            double lookZ;

            var circlingTarget = mob.getCirclingTarget();

            if (circlingTarget != null && !circlingTarget.isAlive()) {
                mob.setCirclingTarget(null);
                circlingTarget = null;
            }

            var rawTarget = mob.getTarget();
            var attackTarget = rawTarget instanceof LivingEntity living && living.isAlive() ? living : null;

            if (circlingTarget != null && attackTarget == null) {
                var pos = circlingTarget.position();
                lookX = pos.x;
                lookY = pos.y + circlingTarget.getBbHeight() + 6D;
                lookZ = pos.z;
            } else if (attackTarget != null) {
                lookX = attackTarget.getX();
                lookY = attackTarget.getEyeY();
                lookZ = attackTarget.getZ();
            } else {
                if (mob.onGround()) {
                    super.tick();
                    return;
                }

                var delta = mob.getDeltaMovement();

                if (delta.lengthSqr() <= 1.0E-4D) {
                    super.tick();
                    return;
                }

                var dx = delta.x;
                var dz = delta.z;
                var horiz = Math.sqrt(dx * dx + dz * dz);

                if (horiz <= 1.0E-4D) {
                    super.tick();
                    return;
                }

                var yaw = (float)(Mth.atan2(dz, dx) * 180F / Math.PI) - 90F;
                var pitch = (float)(-(Mth.atan2(delta.y, horiz) * 180F / Math.PI));

                yRot = rotateTowards(yRot, yaw, maxYaw);
                xRot = rotateTowards(xRot, pitch, maxPitch);

                mob.setYRot(yRot);
                mob.yBodyRot = yRot;
                mob.setYHeadRot(yRot);
                mob.setXRot(xRot);

                return;
            }

            var dx = lookX - mob.getX();
            var dy = lookY - mob.getEyeY();
            var dz = lookZ - mob.getZ();
            var distSq = dx * dx + dz * dz;

            if (distSq > 1.0E-5D || Math.abs(dy) > 1.0E-3D) {
                var yaw = (float)(Mth.atan2(dz, dx) * 180F / Math.PI) - 90F;
                var pitch = (float)(-(Mth.atan2(dy, Math.sqrt(distSq)) * 180F / Math.PI));

                yRot = rotateTowards(yRot, yaw, maxYaw);
                xRot = rotateTowards(xRot, pitch, maxPitch);

                mob.setYRot(yRot);
                mob.yBodyRot = yRot;
                mob.setYHeadRot(yRot);
                mob.setXRot(xRot);
            }
        }

        public float rotateTowards(float current, float target, float maxChange) {
            var diff = Mth.degreesDifference(current, target);

            if (diff > maxChange)
                diff = maxChange;

            if (diff < -maxChange)
                diff = -maxChange;

            return current + diff;
        }
    }
}