package it.hurts.shatterbyte.reanimal.common.entity.starfish;

import com.mojang.serialization.Dynamic;
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
import net.minecraft.world.entity.MobSpawnType;
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
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

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
    protected void customServerAiStep() {
        var level = this.level();
        var profiler = level.getProfiler();

        profiler.push("starfishBrain");
        ((Brain<StarfishEntity>) this.getBrain()).tick((ServerLevel) this.level(), this);
        profiler.pop();

        profiler.push("starfishActivityUpdate");
        StarfishAI.updateActivity(this);
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
            var targets = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox(), candidate -> !(candidate instanceof StarfishEntity));

            for (var target : targets)
                if (target.hurt(this.damageSources().source(ReAnimalDamageTypes.SEA_URCHIN_SPIKES, this), 5F))
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
        if (this.isAlive() && !this.isInWaterOrBubble()) {
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
    public boolean hurt(DamageSource source, float amount) {
        var result = super.hurt(source, amount);

        if (result && !this.level().isClientSide()) {
            var attacker = source.getEntity();

            if (attacker instanceof LivingEntity entity && entity.getMainHandItem().isEmpty())
                if (entity.hurt(this.damageSources().source(ReAnimalDamageTypes.SEA_URCHIN_SPIKES, this), 3F))
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
    public void jumpInFluid(FluidType type) {

    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType type) {
        return true;
    }

    @Override
    protected Brain.Provider<StarfishEntity> brainProvider() {
        return StarfishAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return StarfishAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(VARIANT, 0);
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
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.SEA_URCHIN_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.SEA_URCHIN_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.SEA_URCHIN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.SEA_URCHIN_DEATH.get();
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.STARFISH.get().create(level);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationState<StarfishEntity> state) {
        var controller = state.getController();
        var entity = state.getAnimatable();

        if (!entity.onGround() || entity.getDeltaMovement().multiply(1, 0, 1).length() > 0)
            controller.setAnimation(WOBBLE);
        else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 4D)
                .add(Attributes.MOVEMENT_SPEED, 0.025D)
                .add(NeoForgeMod.SWIM_SPEED, 0.1D)
                .add(Attributes.FOLLOW_RANGE, 4D)
                .add(Attributes.STEP_HEIGHT, 1.1D);
    }
}
