package it.hurts.shatterbyte.reanimal.common.entity.capybara;

import com.mojang.serialization.Dynamic;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CapybaraEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.capybara.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.capybara.walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CapybaraEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.getNavigation().setCanFloat(true);
    }

    @Override
    protected void customServerAiStep() {
        var level = this.level();
        var profiler = level.getProfiler();

        profiler.push("capybaraBrain");
        ((Brain<CapybaraEntity>) this.getBrain()).tick((ServerLevel) this.level(), this);
        profiler.pop();

        profiler.push("capybaraActivityUpdate");
        CapybaraAI.updateActivity(this);
        profiler.pop();

        super.customServerAiStep();
    }

    @Override
    protected Brain.Provider<CapybaraEntity> brainProvider() {
        return CapybaraAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return CapybaraAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.CAPYBARA_FOOD);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.CAPYBARA.get().create(level);

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

    private PlayState mainPredicate(AnimationState<CapybaraEntity> state) {
        var controller = state.getController();

        if (state.isMoving())
            controller.setAnimation(WALK);
        else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 8D);
    }
}
