package it.hurts.shatterbyte.reanimal.common.entity.penguin;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

public class PenguinEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.penguin.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.penguin.walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int eggTime = this.random.nextInt(6000) + 6000;

    public PenguinEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.getNavigation().setCanFloat(true);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.tickEggLaying();
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        var level = this.level();

        ((Brain<PenguinEntity>) this.getBrain()).tick(serverLevel, this);

        PenguinAI.updateActivity(this);

        super.customServerAiStep(serverLevel);
    }
    protected Brain.Provider<PenguinEntity> brainProvider() {
        return PenguinAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return PenguinAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.PENGUIN_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isBaby() ? ReAnimalSoundEvents.PENGUIN_IDLE_BABY : ReAnimalSoundEvents.PENGUIN_IDLE_ADULT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isBaby() ? ReAnimalSoundEvents.PENGUIN_HURT_BABY : ReAnimalSoundEvents.PENGUIN_HURT_ADULT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isBaby() ? ReAnimalSoundEvents.PENGUIN_DEATH_BABY : ReAnimalSoundEvents.PENGUIN_DEATH_ADULT;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.PENGUIN.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("eggTime", this.eggTime);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.eggTime = tag.getIntOr("eggTime", this.random.nextInt(6000) + 6000);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result) {
            this.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);

            PenguinAI.updateActivity(this);
        }

        return result;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationTest<PenguinEntity> state) {
        var controller = state.controller();

        if (state.isMoving())
            controller.setAnimation(WALK);
        else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    private void tickEggLaying() {
        if (this.level().isClientSide() || this.isBaby())
            return;

        if (--this.eggTime > 0)
            return;

        this.playSound(SoundEvents.CHICKEN_EGG, 1F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1F);
        this.spawnAtLocation((ServerLevel) this.level(), ReAnimalItems.PENGUIN_EGG);
        this.gameEvent(GameEvent.ENTITY_PLACE, this);

        this.eggTime = this.random.nextInt(6000) + 6000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 8D)
                .add(Attributes.MOVEMENT_SPEED, 0.175D)
                .add(Attributes.FOLLOW_RANGE, 8D);
    }
}
