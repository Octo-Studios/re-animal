package it.hurts.shatterbyte.reanimal.common.entity.jellyfish;

import com.mojang.serialization.Dynamic;
import it.hurts.shatterbyte.reanimal.init.ReAnimalMobEffects;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class JellyfishEntity extends WaterAnimal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.jellyfish.idle");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.jellyfish.swim");

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(JellyfishEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public float xBodyRot;
    public float xBodyRotO;
    public float tentacleMovement;
    private float speed;
    private float tentacleSpeed;
    private float tx;
    private float ty;
    private float tz;

    public JellyfishEntity(EntityType<? extends WaterAnimal> entityType, Level level) {
        super(entityType, level);

        this.random.setSeed((long) this.getId());
        this.tentacleSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.15F;
    }

    public int getVariant() {
        return this.getEntityData().get(VARIANT);
    }

    public void setVariant(int variant) {
        this.getEntityData().set(VARIANT, variant);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder buidler) {
        super.defineSynchedData(buidler);

        buidler.define(VARIANT, this.getRandom().nextInt(5));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("variant", this.getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.setVariant(tag.getInt("variant"));
    }

    @Override
    protected Brain.Provider<JellyfishEntity> brainProvider() {
        return JellyfishAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return JellyfishAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    protected void customServerAiStep() {
        var level = this.level();
        var profiler = level.getProfiler();

        profiler.push("jellyfishBrain");
        ((Brain<JellyfishEntity>) this.getBrain()).tick((ServerLevel) this.level(), this);
        profiler.pop();

        profiler.push("jellyfishActivityUpdate");
        JellyfishAI.updateActivity(this);
        profiler.pop();

        super.customServerAiStep();
    }

    @Override
    public void tick() {
        super.tick();

        var level = this.level();

        if (level.isClientSide())
            return;

        if (!this.isBaby()) {
            var targets = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox(), (candidate) -> !(candidate instanceof JellyfishEntity));

            for (var target : targets)
                if (target.hurt(this.damageSources().thorns(this), 3F))
                    target.addEffect(new MobEffectInstance(ReAnimalMobEffects.CRAMPS, 200, 0));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        var result = super.hurt(source, amount);

        if (result && !this.level().isClientSide()) {
            var attacker = source.getEntity();

            if (attacker instanceof LivingEntity entity && entity.getMainHandItem().isEmpty())
                if (entity.hurt(this.damageSources().thorns(this), 1F))
                    entity.addEffect(new MobEffectInstance(ReAnimalMobEffects.CRAMPS, 100, 0));
        }

        return result;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.JELLYFISH_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.JELLYFISH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.JELLYFISH_DEATH.get();
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.xBodyRotO = this.xBodyRot;
        this.tentacleMovement = this.tentacleMovement + this.tentacleSpeed;

        if ((double) this.tentacleMovement > Math.PI * 2) {
            if (this.level().isClientSide()) {
                this.tentacleMovement = (float) (Math.PI * 2);
            } else {
                this.tentacleMovement -= (float) (Math.PI * 2);
                if (this.random.nextInt(10) == 0)
                    this.tentacleSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.15F;

                this.level().broadcastEntityEvent(this, (byte) 19);
            }
        }

        if (this.isInWaterOrBubble()) {
            if (this.tentacleMovement < (float) Math.PI) {
                float progress = this.tentacleMovement / (float) Math.PI;
                if ((double) progress > 0.85) {
                    this.speed = 0.11F;
                } else
                    this.speed = 0.06F;
            } else {
                this.speed *= 0.85F;
            }

            if (this.speed < 0.02F)
                this.speed = 0.02F;

            var desired = new Vec3(this.tx, this.ty, this.tz);
            var desiredSqr = desired.lengthSqr();
            var motion = this.getDeltaMovement();
            var motionSqr = motion.lengthSqr();

            Vec3 facing = null;

            if (desiredSqr > 1.0E-4D && !this.level().isClientSide())
                facing = desired.normalize();
            else if (motionSqr > 1.0E-4D)
                facing = motion.normalize();

            if (facing != null) {
                float targetYaw = -((float) Mth.atan2(facing.x, facing.z)) * (180.0F / (float) Math.PI);
                float targetPitch = -((float) Mth.atan2(facing.y, Math.sqrt(facing.x * facing.x + facing.z * facing.z))) * (180.0F / (float) Math.PI);
                targetPitch = Mth.clamp(targetPitch, -60F, 60F);

                var abovePos = this.blockPosition().above(3);
                var belowPos = this.blockPosition().below(3);
                var aboveFluid = this.level().getFluidState(abovePos);
                var belowFluid = this.level().getFluidState(belowPos);
                boolean surfaceBlocked = !aboveFluid.is(FluidTags.WATER);
                boolean groundBlocked = !belowFluid.is(FluidTags.WATER);
                boolean verticalBlocked = surfaceBlocked && groundBlocked;

                if (verticalBlocked) {
                    targetPitch = Mth.lerp(0.35F, targetPitch, 0F);
                } else if (surfaceBlocked && targetPitch < 0F)
                    targetPitch = Mth.clamp(Mth.lerp(0.6F, targetPitch, 40F), 15F, 70F);
                else if (groundBlocked && targetPitch > 0F)
                    targetPitch = Mth.clamp(Mth.lerp(0.35F, targetPitch, -30F), -60F, -10F);

                this.yHeadRot = Mth.rotLerp(0.015F, this.yHeadRot, targetYaw);
                this.yBodyRot = this.yHeadRot;
                this.setYRot(this.yBodyRot);
                this.xBodyRot = Mth.rotLerp(0.03F, this.xBodyRot, targetPitch);
                this.setXRot(this.xBodyRot);

                if (!this.level().isClientSide()) {
                    var moveFacing = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
                    var target = moveFacing.scale(this.speed);
                    if (verticalBlocked) {
                        target = new Vec3(target.x, 0.0D, target.z);
                    } else {
                        if (surfaceBlocked)
                            target = new Vec3(target.x, Math.min(target.y, -0.02D), target.z);

                        if (groundBlocked && target.y < 0.0D)
                            target = new Vec3(target.x, 0.0D, target.z);
                    }

                    if (target.lengthSqr() > 1.0E-6D) {
                        var nextBox = this.getBoundingBox().move(target);
                        if (!this.level().noCollision(this, nextBox) || this.horizontalCollision || this.isWallAhead(moveFacing))
                            target = this.findWaterAvoidance(target);
                    }

                    if (target.lengthSqr() > 1.0E-6D) {
                        var forwardPos = this.position().add(target.normalize());
                        var forwardBlock = BlockPos.containing(forwardPos);
                        var forwardFluid = this.level().getFluidState(forwardBlock);

                        if (!forwardFluid.is(FluidTags.WATER)) {
                            target = new Vec3(target.x * -0.5D, Math.max(0.05D, target.y), target.z * -0.5D);
                        }
                    }

                    var current = this.getDeltaMovement();
                    this.setDeltaMovement(current.add(target.subtract(current).scale(0.1D)));
                }
            } else {
                this.xBodyRot = Mth.rotLerp(0.01F, this.xBodyRot, 0F);
                this.setXRot(this.xBodyRot);
            }
        } else {
            if (!this.level().isClientSide()) {
                double vertical = this.getDeltaMovement().y;
                if (this.hasEffect(MobEffects.LEVITATION))
                    vertical = 0.05 * (double) (this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1);
                else
                    vertical -= this.getGravity();

                this.setDeltaMovement(0.0, vertical * 0.98F, 0.0);
            }

            this.xBodyRot = this.xBodyRot + (-90.0F - this.xBodyRot) * 0.02F;
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 19)
            this.tentacleMovement = 0.0F;
        else
            super.handleEntityEvent(id);
    }

    public void setMovementVector(float tx, float ty, float tz) {
        this.tx = tx;
        this.ty = ty;
        this.tz = tz;
    }

    public boolean hasMovementVector() {
        return this.tx != 0.0F || this.ty != 0.0F || this.tz != 0.0F;
    }

    private Vec3 findWaterAvoidance(Vec3 currentTarget) {
        var horizontal = new Vec3(currentTarget.x, 0.0D, currentTarget.z);
        if (horizontal.lengthSqr() < 1.0E-6D)
            return currentTarget.scale(-0.5D);

        var dir = horizontal.normalize();
        Vec3[] options = new Vec3[] {
                new Vec3(-dir.z, 0.0D, dir.x),
                new Vec3(dir.z, 0.0D, -dir.x),
                dir.scale(-1.0D)
        };

        for (var option : options) {
            var candidate = new Vec3(option.x * this.speed, currentTarget.y, option.z * this.speed);
            if (this.isWaterPath(candidate))
                return candidate;
        }

        return currentTarget.scale(-0.5D);
    }

    private boolean isWallAhead(Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6D)
            return false;

        var forwardPos = BlockPos.containing(this.position().add(direction.normalize().scale(3.0D)));
        var state = this.level().getBlockState(forwardPos);
        if (!state.getCollisionShape(this.level(), forwardPos).isEmpty())
            return true;

        var fluid = this.level().getFluidState(forwardPos);
        return !fluid.is(FluidTags.WATER);
    }

    private boolean isWaterPath(Vec3 candidate) {
        if (candidate.lengthSqr() < 1.0E-6D)
            return false;

        var nextBox = this.getBoundingBox().move(candidate);
        if (!this.level().noCollision(this, nextBox))
            return false;

        var aheadPos = BlockPos.containing(this.position().add(candidate.normalize()));
        var aheadFluid = this.level().getFluidState(aheadPos);
        return aheadFluid.is(FluidTags.WATER);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationState<JellyfishEntity> state) {
        var controller = state.getController();
        var entity = state.getAnimatable();

        if (entity.isInWaterOrBubble() && entity.tentacleMovement < (float) Math.PI)
            controller.setAnimation(SWIM);
        else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6D)
                .add(Attributes.MOVEMENT_SPEED, 0.1D)
                .add(NeoForgeMod.SWIM_SPEED, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 12D);
    }
}
