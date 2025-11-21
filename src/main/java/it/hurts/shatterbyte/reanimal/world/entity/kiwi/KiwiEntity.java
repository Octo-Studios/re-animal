package it.hurts.shatterbyte.reanimal.world.entity.kiwi;

import com.mojang.serialization.Dynamic;
import it.hurts.shatterbyte.reanimal.registry.ReAnimalEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class KiwiEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.kiwi.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.kiwi.walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public KiwiEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void customServerAiStep() {
        var level = this.level();
        var profiler = level.getProfiler();

        profiler.push("kiwiBrain");
        ((Brain<KiwiEntity>) this.getBrain()).tick((ServerLevel) this.level(), this);
        profiler.pop();

        profiler.push("kiwiActivityUpdate");
        KiwiAI.updateActivity(this);
        profiler.pop();

        super.customServerAiStep();
    }

    @Override
    protected Brain.Provider<KiwiEntity> brainProvider() {
        return KiwiAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return KiwiAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SPIDER_EYE);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.KIWI.get().create(level);

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

    private PlayState mainPredicate(AnimationState<KiwiEntity> state) {
        var controller = state.getController();

        if (state.isMoving())
            controller.setAnimation(WALK);
        else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 4D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 8D);
    }
}