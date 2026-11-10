package it.hurts.shatterbyte.reanimal.common.entity.sea_urchin;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import it.hurts.shatterbyte.reanimal.common.entity.SurfaceClimber;
import it.hurts.shatterbyte.reanimal.init.ReAnimalDamageTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalMobEffects;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public class SeaUrchinEntity extends Animal implements GeoEntity, SurfaceClimber {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.sea_urchin.idle");
    private static final RawAnimation WOBBLE = RawAnimation.begin().thenLoop("animation.sea_urchin.wobbling");
    private static final EntityDataAccessor<Boolean> EATING = SynchedEntityData.defineId(SeaUrchinEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ROLLING = SynchedEntityData.defineId(SeaUrchinEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Quaternionfc> ROLL_ROTATION = SynchedEntityData.defineId(SeaUrchinEntity.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Direction> CLIMBING_SIDE = SynchedEntityData.defineId(SeaUrchinEntity.class, EntityDataSerializers.DIRECTION);
    private static final int RIGHTING_DELAY = 40;
    private static final int RIGHTING_DURATION = 100;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Quaternionf rotation = new Quaternionf();
    private final Quaternionf previousRotation = new Quaternionf();
    private int rollTicks;

    public SeaUrchinEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.setPathfindingMalus(PathType.WATER, 0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 16F);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new SeaUrchinNavigation(this, level);
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        if (!this.isRolling()) {
            ((Brain<SeaUrchinEntity>) this.getBrain()).tick(serverLevel, this);
            SeaUrchinAI.updateActivity(this);
        }

        super.customServerAiStep(serverLevel);
    }

    @Override
    public void tick() {
        this.previousRotation.set(this.rotation);
        var start = this.position();

        super.tick();

        var level = this.level();

        if (level.isClientSide()) {
            this.rotation.set(this.entityData.get(ROLL_ROTATION));

            return;
        }

        if (this.isRolling()) {
            var movement = this.position().subtract(start);
            var horizontal = movement.horizontalDistance();

            if (horizontal > 0D) {
                var angle = (float) Math.min(horizontal / (this.getBbWidth() * 0.5D), Mth.PI * 0.25F);

                this.rotation.premul(new Quaternionf().rotationAxis(angle, (float) (movement.z / horizontal), 0F, (float) (-movement.x / horizontal))).normalize();
            }

            if (this.onGround()) {
                if (this.rollTicks <= RIGHTING_DURATION)
                    this.rotation.slerp(new Quaternionf(), 1F / this.rollTicks);

                if (--this.rollTicks == 0) {
                    this.rotation.identity();
                    this.entityData.set(ROLLING, false);
                }
            }
        } else {
            this.rotation.slerp(this.getClimbingSide().getOpposite().getRotation(), 0.25F);
        }

        this.entityData.set(ROLL_ROTATION, new Quaternionf(this.rotation));

        if (!this.isBaby()) {
            var targets = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox(), (candidate) -> !(candidate instanceof SeaUrchinEntity));

            for (var target : targets)
                if (target.hurtOrSimulate(ReAnimalDamageTypes.source(this, ReAnimalDamageTypes.SEA_URCHIN_SPIKES), 5F))
                    target.addEffect(new MobEffectInstance(ReAnimalMobEffects.CRAMPS, 200, 0));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(EATING, false);
        builder.define(ROLLING, false);
        builder.define(CLIMBING_SIDE, Direction.DOWN);
        builder.define(ROLL_ROTATION, new Quaternionf());
    }

    public boolean isEating() {
        return this.entityData.get(EATING);
    }

    public void setEating(boolean eating) {
        this.entityData.set(EATING, eating);
    }

    public boolean isRolling() {
        return this.entityData.get(ROLLING);
    }

    @Override
    public Direction getClimbingSide() {
        return this.entityData.get(CLIMBING_SIDE);
    }

    @Override
    public void setClimbingSide(Direction side, Vec3 movement) {
        this.entityData.set(CLIMBING_SIDE, side);
    }

    @Override
    public float maxUpStep() {
        return this.isRolling() ? 0F : super.maxUpStep();
    }

    public Quaternionf getRenderRotation(float partialTick) {
        return new Quaternionf(this.previousRotation).slerp(this.rotation, partialTick);
    }

    private void startRolling() {
        this.rollTicks = RIGHTING_DELAY + RIGHTING_DURATION;
        this.entityData.set(ROLLING, true);
        this.setEating(false);
        this.setClimbingSide(Direction.DOWN, Vec3.ZERO);
        this.getNavigation().stop();
        this.getMoveControl().setWait();
        this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    @Override
    public void push(double x, double y, double z) {
        super.push(x, y, z);

        if (!this.level().isClientSide() && (x != 0D || z != 0D))
            this.startRolling();
    }

    @Override
    public void knockback(double power, double x, double z, DamageSource source, float damage, boolean comesFromEffect) {
        super.knockback(power, x, z, source, damage, comesFromEffect);

        if (!this.level().isClientSide())
            this.startRolling();
    }

    @Override
    public void baseTick() {
        int air = this.getAirSupply();
        super.baseTick();

        if (!this.isNoAi())
            this.handleAirSupply(air);
    }

    protected void handleAirSupply(int airSupply) {
        if (this.isAlive() && !this.isInWater()) {
            this.setAirSupply(airSupply - 1);

            if (this.getAirSupply() == -20) {
                this.setAirSupply(0);
                this.hurt(this.damageSources().dryOut(), 2F);
            }
        } else {
            this.setAirSupply(this.getMaxAirSupply());
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result) {
            var attacker = source.getEntity();

            if (attacker instanceof LivingEntity entity && entity.getMainHandItem().isEmpty())
                if (entity.hurtOrSimulate(ReAnimalDamageTypes.source(this, ReAnimalDamageTypes.SEA_URCHIN_SPIKES), 3F))
                    entity.addEffect(new MobEffectInstance(ReAnimalMobEffects.CRAMPS, 100, 0));
        }

        return result;
    }

    @Override
    public void travel(Vec3 vec) {
        if (this.isRolling()) {
            var water = this.isInWater();
            var gravity = this.getEffectiveGravity() / (water ? 16D : 1D);
            var movement = this.getDeltaMovement().add(0D, -gravity, 0D);

            this.setDeltaMovement(movement);
            this.move(MoverType.SELF, movement);

            var collided = this.getDeltaMovement();
            var hitX = this.horizontalCollision && collided.x == 0D;
            var hitZ = this.horizontalCollision && collided.z == 0D;
            var bounceY = this.verticalCollision && Math.abs(movement.y) > gravity * 2D;
            var drag = water ? 0.8D : this.onGround() ? 0.9D : 0.98D;

            this.setDeltaMovement(
                    hitX ? -movement.x * 0.35D : movement.x * drag,
                    this.verticalCollision ? (bounceY ? -movement.y * 0.25D : 0D) : movement.y * (water ? 0.8D : 0.98D),
                    hitZ ? -movement.z * 0.35D : movement.z * drag
            );
        } else {
            var navigation = (SeaUrchinNavigation) this.getNavigation();

            if (this.isEffectiveAi() && !this.isNoAi() && !this.isPassenger() && !this.isInLava()
                    && (navigation.isOnSurfacePath() || this.getClimbingSide() != Direction.DOWN
                    && navigation.hasSupport(this.position(), this.getClimbingSide()))) {
                this.setDeltaMovement(navigation.getMotion());
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setOnGround(this.getClimbingSide() == Direction.DOWN);
                this.resetFallDistance();
            } else {
                if (!this.level().isClientSide())
                    this.setClimbingSide(Direction.DOWN, Vec3.ZERO);

                super.travel(vec);
            }
        }
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
            }
        };
    }

    @Override
    public void jumpFromGround() {

    }

    @Override
    public void setJumping(boolean jumping) {
        super.setJumping(false);
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason type) {
        return true;
    }
    protected Brain.Provider<SeaUrchinEntity> brainProvider() {
        return SeaUrchinAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return SeaUrchinAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("ClimbingSide", this.getClimbingSide().get3DDataValue());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.setClimbingSide(Direction.from3DDataValue(tag.getIntOr("ClimbingSide", Direction.DOWN.get3DDataValue())), Vec3.ZERO);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.SEA_URCHIN_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.SEA_URCHIN_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.SEA_URCHIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.SEA_URCHIN_DEATH;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.SEA_URCHIN.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationTest<SeaUrchinEntity> state) {
        var controller = state.controller();
        var entity = state.animatable();

        if (entity.isEating() || entity.isRolling() || !entity.onGround() || entity.getDeltaMovement().multiply(1, 0, 1).length() > 0) {
            controller.setAnimation(WOBBLE);
        } else {
            controller.setAnimation(IDLE);
        }

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 4D)
                .add(Attributes.MOVEMENT_SPEED, 0.025D)
                .add(Attributes.FOLLOW_RANGE, 4D)
                .add(Attributes.STEP_HEIGHT, 1.1D);
    }
}
