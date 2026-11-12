package it.hurts.shatterbyte.reanimal.common.entity.jellyfish;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import it.hurts.shatterbyte.reanimal.common.entity.glow_stick.GlowStickEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalDamageTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import it.hurts.shatterbyte.reanimal.init.ReAnimalMobEffects;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

public class JellyfishEntity extends Animal implements GeoEntity, Bucketable {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.jellyfish.idle");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.jellyfish.swim");
    private static final RawAnimation LAND = RawAnimation.begin().thenLoop("animation.jellyfish.land");
    private static final RawAnimation BABY_IDLE = RawAnimation.begin().thenLoop("animation.jellyfish_baby.idle");
    private static final RawAnimation BABY_SWIM = RawAnimation.begin().thenLoop("animation.jellyfish_baby.swim");
    private static final RawAnimation BABY_LAND = RawAnimation.begin().thenLoop("animation.jellyfish_baby.land");

    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.46F, 0.46F).withEyeHeight(0.23F);

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(JellyfishEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(JellyfishEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BRIGHTENING = SynchedEntityData.defineId(JellyfishEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int GLOW_PULSE_TICKS = 100;
    private static final int BRIGHTEN_TICKS = 10;
    private static final int GLOW_FADE_TICKS = 40;

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
    private int brightenTicks;
    private float glowStrength;
    private float previousGlowStrength;
    private boolean beached;
    private float landProgress;
    private float previousLandProgress;

    public JellyfishEntity(EntityType<? extends JellyfishEntity> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(PathType.WATER, 0F);
        this.lookControl = new LookControl(this) {
            @Override
            public void tick() {
                if (!JellyfishEntity.this.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET))
                    super.tick();
            }
        };
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

    public float getGlowPulse(float partialTick) {
        return 0.5F + 0.5F * Mth.sin((this.level().getGameTime() % GLOW_PULSE_TICKS + this.getId() * 31 + partialTick) * Mth.TWO_PI / GLOW_PULSE_TICKS);
    }

    public float getGlowIntensity(float partialTick) {
        var reaction = Mth.lerp(partialTick, this.previousGlowStrength, this.glowStrength);
        reaction = reaction * reaction * (3F - 2F * reaction);

        return Mth.lerp(reaction, 0.35F + 0.2F * this.getGlowPulse(partialTick), 1F);
    }

    public int getLightLevel() {
        return Math.round((this.isBaby() ? 8F : 15F) * this.getGlowIntensity(1F));
    }

    public float getLandProgress(float partialTick) {
        return Mth.lerp(partialTick, this.previousLandProgress, this.landProgress);
    }

    private void brighten() {
        this.brightenTicks = BRIGHTEN_TICKS;
        this.entityData.set(BRIGHTENING, true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder buidler) {
        super.defineSynchedData(buidler);

        buidler.define(VARIANT, this.getRandom().nextInt(5));
        buidler.define(FROM_BUCKET, false);
        buidler.define(BRIGHTENING, false);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("variant", this.getVariant());
        tag.putBoolean("FromBucket", this.fromBucket());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.setVariant(tag.getIntOr("variant", 0));
        this.setFromBucket(tag.getBooleanOr("FromBucket", false));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide())
            this.brighten();

        return Bucketable.bucketMobPickup(player, hand, this).orElseGet(() -> super.mobInteract(player, hand));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.JELLYFISH_FOOD);
    }

    @Override
    public boolean canFallInLove() {
        return this.isInWater() && super.canFallInLove();
    }

    @Override
    public boolean canMate(Animal partner) {
        return this.isInWater() && partner.isInWater() && super.canMate(partner);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.JELLYFISH.create(level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            baby.setVariant(this.random.nextBoolean() ? this.getVariant() : ((JellyfishEntity) partner).getVariant());
            baby.setPersistenceRequired();
        }
        return baby;
    }

    @Override
    public boolean fromBucket() {
        return this.getEntityData().get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean fromBucket) {
        this.getEntityData().set(FROM_BUCKET, fromBucket);
    }

    @Override
    public void saveToBucketTag(ItemStack bucket) {
        Bucketable.saveDefaultDataToBucketTag(this, bucket);

        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> {
            tag.putInt("variant", this.getVariant());
            tag.putInt("Age", this.getAge());
            tag.putBoolean("AgeLocked", this.isAgeLocked());
        });
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        Bucketable.loadDefaultDataFromBucketTag(this, tag);

        this.setVariant(tag.getIntOr("variant", 0));
        this.setAge(tag.getIntOr("Age", 0));
        this.setAgeLocked(tag.getBooleanOr("AgeLocked", false));
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(ReAnimalItems.JELLYFISH_BUCKET);
    }

    @Override
    public SoundEvent getPickupSound() {
        return SoundEvents.BUCKET_FILL_FISH;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.fromBucket();
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return !this.fromBucket() && !this.hasCustomName();
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0F;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void baseTick() {
        var air = this.getAirSupply();
        super.baseTick();

        if (this.level() instanceof ServerLevel level) {
            if (this.isAlive() && !this.isInWater()) {
                this.setAirSupply(air - 1);
                if (this.shouldTakeDrowningDamage()) {
                    this.setAirSupply(0);
                    this.hurtServer(level, this.damageSources().drown(), 2F);
                }
            } else {
                this.setAirSupply(this.getMaxAirSupply());
            }
        }
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
        var brightening = this.entityData.get(BRIGHTENING);

        this.previousGlowStrength = this.glowStrength;
        this.glowStrength = Mth.approach(this.glowStrength, brightening ? 1F : 0F,
                brightening ? 1F / BRIGHTEN_TICKS : 1F / GLOW_FADE_TICKS);

        if (level.isClientSide())
            return;

        if (this.brightenTicks > 0 && --this.brightenTicks == 0)
            this.entityData.set(BRIGHTENING, false);

        var targets = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox(), candidate -> !(candidate instanceof JellyfishEntity));

        if (!targets.isEmpty())
            this.brighten();

        GlowStickEntity.tryPlaceGlowLight(level, BlockPos.containing(this.getBoundingBox().getCenter()), this.getLightLevel());

        if (!this.isBaby()) {
            for (var target : targets)
                if (target.hurtOrSimulate(ReAnimalDamageTypes.source(this, ReAnimalDamageTypes.JELLYFISH_STING), 3F))
                    target.addEffect(new MobEffectInstance(ReAnimalMobEffects.CRAMPS, 200, 0));
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result) {
            this.brighten();

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
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        var dimensions = this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
        return dimensions.scale(1F, this.beached ? 0.65F : 1F);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        var beached = this.onGround() && !this.isInWater();
        if (this.beached != beached) {
            this.beached = beached;
            this.refreshDimensions();
        }

        this.previousLandProgress = this.landProgress;
        this.landProgress = Mth.approach(this.landProgress, this.beached ? 1F : 0F, 0.2F);

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
        if (this.getBrain().getMemory(MemoryModuleType.BREED_TARGET).orElse(null) instanceof JellyfishEntity mate) {
            this.swimToMate(mate, thrusting);
            return;
        }

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

    private void swimToMate(JellyfishEntity mate, boolean thrusting) {
        this.chooseHeading = true;
        if (this.closerThan(mate, JellyfishAI.BREED_DISTANCE)) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        var direction = mate.position().subtract(this.position()).normalize();
        this.turnTowards(direction);
        var speed = Math.max(0D, this.getDeltaMovement().dot(direction));
        if (thrusting)
            speed += THRUST_ACCELERATION;
        this.setDeltaMovement(direction.scale(speed * WATER_DRAG));
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
        var baby = entity.isBaby();

        if (entity.beached)
            controller.setAnimation(baby ? BABY_LAND : LAND);
        else if (entity.isInWater() && entity.tentacleMovement < (float) Math.PI)
            controller.setAnimation(baby ? BABY_SWIM : SWIM);
        else
            controller.setAnimation(baby ? BABY_IDLE : IDLE);

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6D)
                .add(Attributes.MOVEMENT_SPEED, 0.1D)
                .add(Attributes.FOLLOW_RANGE, 12D);
    }
}
