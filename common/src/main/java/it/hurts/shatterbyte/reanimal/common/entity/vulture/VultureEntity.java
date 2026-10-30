package it.hurts.shatterbyte.reanimal.common.entity.vulture;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class VultureEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.vulture.idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.vulture.fly");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.vulture.walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int eggTime = this.random.nextInt(6000) + 6000;

    public VultureEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.updateMoveControl();

        this.getNavigation().setCanFloat(true);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.tickEggLaying();
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        ((Brain<VultureEntity>) this.getBrain()).tick(serverLevel, this);

        VultureAI.updateActivity(this);

        super.customServerAiStep(serverLevel);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.updateMoveControl();
    }

    @Override
    public void setAge(int age) {
        var wasBaby = this.isBaby();

        super.setAge(age);

        if (wasBaby != this.isBaby())
            this.updateMoveControl();
    }

    @Override
    public boolean causeFallDamage(double distance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        this.resetFallDistance();
    }

    protected Brain.Provider<VultureEntity> brainProvider() {
        return VultureAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return VultureAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.VULTURE_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.VULTURE_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.VULTURE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.VULTURE_DEATH;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.VULTURE.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);

        if (baby != null) {
            baby.setBaby(true);
            baby.updateMoveControl();
        }

        return baby;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::mainPredicate));
    }

    public void updateMoveControl() {
        if (this.isBaby()) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigation(this, this.level());
        } else {
            this.moveControl = new FlyingMoveControl(this, 10, false);
            this.navigation = new FlyingPathNavigation(this, this.level());
        }
    }

    private void tickEggLaying() {
        if (this.level().isClientSide() || this.isBaby())
            return;

        if (--this.eggTime > 0)
            return;

        this.playSound(SoundEvents.CHICKEN_EGG, 1F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1F);
        this.spawnAtLocation((ServerLevel) this.level(), ReAnimalItems.VULTURE_EGG);
        this.gameEvent(GameEvent.ENTITY_PLACE, this);

        this.eggTime = this.random.nextInt(6000) + 6000;
    }

    private PlayState mainPredicate(AnimationTest<VultureEntity> state) {
        var controller = state.controller();
        var entity = state.animatable();

        if (state.isMoving()) {
            if (entity.onGround())
                controller.setAnimation(WALK);
            else
                controller.setAnimation(FLY);
        } else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    public static boolean checkVultureSpawnRules(EntityType<VultureEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return Animal.checkAnimalSpawnRules(type, level, reason, pos, random);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 10D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 16D)
                .add(Attributes.ATTACK_DAMAGE, 3D);
    }
}
