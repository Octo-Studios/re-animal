package it.hurts.shatterbyte.reanimal.common.entity.capybara;

import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

public class CapybaraEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.capybara.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.capybara.walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CapybaraEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.getNavigation().setCanFloat(true);
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        var level = this.level();

        ((Brain<CapybaraEntity>) this.getBrain()).tick(serverLevel, this);

        CapybaraAI.updateActivity(this);

        super.customServerAiStep(serverLevel);
    }
    protected Brain.Provider<CapybaraEntity> brainProvider() {
        return CapybaraAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return CapybaraAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.CAPYBARA_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.CAPYBARA_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.CAPYBARA_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.CAPYBARA_DEATH;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.CAPYBARA.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result) {
            this.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);

            CapybaraAI.updateActivity(this);
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

    private PlayState mainPredicate(AnimationTest<CapybaraEntity> state) {
        var controller = state.controller();

        if (state.isMoving())
            controller.setAnimation(WALK);
        else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 8D);
    }
}
