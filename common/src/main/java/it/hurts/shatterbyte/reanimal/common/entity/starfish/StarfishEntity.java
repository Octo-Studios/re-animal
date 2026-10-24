package it.hurts.shatterbyte.reanimal.common.entity.starfish;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import it.hurts.shatterbyte.reanimal.init.ReAnimalDamageTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalMobEffects;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

public class StarfishEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.starfish.idle");
    private static final RawAnimation WOBBLE = RawAnimation.begin().thenLoop("animation.starfish.idle");

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(StarfishEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public StarfishEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.setPathfindingMalus(PathType.WATER, 0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 16F);

        if (!level.isClientSide())
            this.setVariant(this.getRandom().nextInt(5));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    public int getVariant() {
        return this.getEntityData().get(VARIANT);
    }

    public void setVariant(int variant) {
        this.getEntityData().set(VARIANT, variant);
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        var level = this.level();

        ((Brain<StarfishEntity>) this.getBrain()).tick(serverLevel, this);

        StarfishAI.updateActivity(this);

        super.customServerAiStep(serverLevel);
    }

    @Override
    public void tick() {
        super.tick();

        var level = this.level();

        if (level.isClientSide())
            return;

        if (!this.isBaby()) {
            var targets = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox(), candidate -> !(candidate instanceof StarfishEntity));

            for (var target : targets)
                if (target.hurtOrSimulate(ReAnimalDamageTypes.source(this, ReAnimalDamageTypes.SEA_URCHIN_SPIKES), 5F))
                    target.addEffect(new MobEffectInstance(ReAnimalMobEffects.CRAMPS, 200, 0));
        }
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
        if (this.isEffectiveAi() && this.isInWater() && this.onGround()) {
            var speed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 25;

            this.moveRelative(speed * 0.1F, vec);
            this.move(MoverType.SELF, this.getDeltaMovement());
        } else
            super.travel(vec);
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
    protected Brain.Provider<StarfishEntity> brainProvider() {
        return StarfishAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return StarfishAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(VARIANT, 0);
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
        var baby = ReAnimalEntities.STARFISH.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);

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

    private PlayState mainPredicate(AnimationTest<StarfishEntity> state) {
        var controller = state.controller();
        var entity = state.animatable();

        if (!entity.onGround() || entity.getDeltaMovement().multiply(1, 0, 1).length() > 0)
            controller.setAnimation(WOBBLE);
        else
            controller.setAnimation(IDLE);

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
