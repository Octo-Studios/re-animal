package it.hurts.shatterbyte.reanimal.common.entity.jellyfish;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import it.hurts.shatterbyte.reanimal.init.ReAnimalDamageTypes;
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
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

public class JellyfishEntity extends WaterAnimal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.jellyfish.idle");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.jellyfish.swim");

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(JellyfishEntity.class, EntityDataSerializers.INT);

    private static final float SWIM_PHASE_SPEED = (float) Math.PI / 40F;
    private static final float THRUST_START = 0.75F;
    private static final float THRUST_END = 0.9F;
    private static final float THRUST_ACCELERATION = 0.022F;
    private static final double WATER_DRAG = 0.96D;
    private static final float TURN_SPEED = 0.1F;
    private static final int CLEARANCE_STEPS = 6;
    private static final int[] HEADING_TURNS = {0, 45, -45, 90, -90, 135, -135, 180};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public float tentacleMovement;
    private float tx;
    private float ty;
    private float tz;
    private boolean chooseHeading = true;
    private Vec3 heading;

    public JellyfishEntity(EntityType<? extends WaterAnimal> entityType, Level level) {
        super(entityType, level);
    }

    private float getTentacleSpeed() {
        if (this.tentacleMovement < (float) Math.PI)
            return SWIM_PHASE_SPEED;

        return 0.05F + 0.0285F * (Math.floorMod(this.getId() * 31, 100) / 100F);
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
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("variant", this.getVariant());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.setVariant(tag.getIntOr("variant", 0));
    }
    protected Brain.Provider<JellyfishEntity> brainProvider() {
        return JellyfishAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return JellyfishAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        var level = this.level();

        ((Brain<JellyfishEntity>) this.getBrain()).tick(serverLevel, this);

        JellyfishAI.updateActivity(this);

        super.customServerAiStep(serverLevel);
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
                if (target.hurtOrSimulate(ReAnimalDamageTypes.source(this, ReAnimalDamageTypes.JELLYFISH_STING), 3F))
                    target.addEffect(new MobEffectInstance(ReAnimalMobEffects.CRAMPS, 200, 0));
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result) {
            var attacker = source.getEntity();

            if (attacker instanceof LivingEntity entity && entity.getMainHandItem().isEmpty())
                if (entity.hurtOrSimulate(ReAnimalDamageTypes.source(this, ReAnimalDamageTypes.JELLYFISH_STING), 1F))
                    entity.addEffect(new MobEffectInstance(ReAnimalMobEffects.CRAMPS, 100, 0));
        }

        return result;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.JELLYFISH_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.JELLYFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.JELLYFISH_DEATH;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.tentacleMovement += this.getTentacleSpeed();

        if (this.tentacleMovement > Math.PI * 2) {
            if (this.level().isClientSide()) {
                this.tentacleMovement = (float) (Math.PI * 2);
            } else {
                this.tentacleMovement -= (float) (Math.PI * 2);
                this.chooseHeading = true;
                this.level().broadcastEntityEvent(this, (byte) 19);
            }
        }

        if (this.level().isClientSide())
            return;

        if (!this.isInWater()) {
            double vertical = this.getDeltaMovement().y;

            if (this.hasEffect(MobEffects.LEVITATION))
                vertical = 0.05 * (double) (this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1);
            else
                vertical -= this.getGravity();

            this.setDeltaMovement(0.0, vertical * 0.98F, 0.0);

            return;
        }

        var progress = this.tentacleMovement / (float) Math.PI;
        var thrusting = progress >= THRUST_START && progress <= THRUST_END;

        if (this.chooseHeading) {
            this.chooseHeading = false;
            this.heading = this.hasMovementVector() ? this.findSafeHeading(new Vec3(this.tx, this.ty, this.tz)) : null;
        }

        if (this.heading != null && progress < THRUST_START)
            this.turnTowards(this.heading);

        var motion = this.getDeltaMovement();

        if (thrusting && this.heading != null) {
            var look = Vec3.directionFromRotation(this.getXRot(), this.getYRot());

            if (this.level().noCollision(this, this.getBoundingBox().move(look.scale(0.5D))))
                motion = motion.add(look.scale(THRUST_ACCELERATION));
        }

        this.setDeltaMovement(motion.scale(WATER_DRAG));
    }

    private void turnTowards(Vec3 direction) {
        var yaw = -((float) Mth.atan2(direction.x, direction.z)) * Mth.RAD_TO_DEG;
        var pitch = -((float) Mth.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z))) * Mth.RAD_TO_DEG;

        this.yHeadRot = Mth.rotLerp(TURN_SPEED, this.yHeadRot, yaw);
        this.yBodyRot = this.yHeadRot;

        this.setYRot(this.yBodyRot);
        this.setXRot(Mth.rotLerp(TURN_SPEED, this.getXRot(), Mth.clamp(pitch, -60F, 60F)));
    }

    private Vec3 findSafeHeading(Vec3 desired) {
        var horizontal = Math.sqrt(desired.x * desired.x + desired.z * desired.z);
        var baseAngle = horizontal < 1.0E-4D ? this.random.nextFloat() * Math.PI * 2 : Math.atan2(desired.z, desired.x);
        var side = this.random.nextBoolean() ? 1 : -1;

        Vec3 best = null;
        var bestClearance = -1;

        for (var turn : HEADING_TURNS) {
            var angle = baseAngle + Math.toRadians(turn * side);
            var candidate = new Vec3(Math.cos(angle) * horizontal, desired.y, Math.sin(angle) * horizontal);

            if (horizontal < 1.0E-4D)
                candidate = new Vec3(Math.cos(angle), desired.y, Math.sin(angle));

            candidate = candidate.normalize();

            var clearance = this.countClearSteps(candidate);

            if (clearance >= CLEARANCE_STEPS)
                return candidate;

            if (clearance > bestClearance) {
                best = candidate;
                bestClearance = clearance;
            }
        }

        return best;
    }

    private int countClearSteps(Vec3 direction) {
        var center = this.getBoundingBox().getCenter();

        for (var step = 1; step <= CLEARANCE_STEPS; step++) {
            var offset = direction.scale(step);

            if (!this.level().noCollision(this, this.getBoundingBox().move(offset)))
                return step - 1;

            if (!this.level().getFluidState(BlockPos.containing(center.add(offset))).is(FluidTags.WATER))
                return step - 1;
        }

        return CLEARANCE_STEPS;
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
        var previous = new Vec3(this.tx, this.ty, this.tz);
        var next = new Vec3(tx, ty, tz);

        if (previous.lengthSqr() < 1.0E-8D || next.lengthSqr() < 1.0E-8D || previous.normalize().dot(next.normalize()) < 0.9D)
            this.chooseHeading = true;

        this.tx = tx;
        this.ty = ty;
        this.tz = tz;
    }

    public boolean hasMovementVector() {
        return this.tx != 0.0F || this.ty != 0.0F || this.tz != 0.0F;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationTest<JellyfishEntity> state) {
        var controller = state.controller();
        var entity = state.animatable();

        if (entity.isInWater() && entity.tentacleMovement < (float) Math.PI)
            controller.setAnimation(SWIM);
        else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6D)
                .add(Attributes.MOVEMENT_SPEED, 0.1D)
                .add(Attributes.FOLLOW_RANGE, 12D);
    }
}
