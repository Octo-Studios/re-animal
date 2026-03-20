package it.hurts.shatterbyte.reanimal.common.entity.red_panda;

import com.mojang.serialization.Dynamic;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class RedPandaEntity extends TamableAnimal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.red_panda.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.red_panda.walk");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.red_panda.sleep");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RedPandaEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.getNavigation().setCanFloat(true);
    }

    @Override
    protected Brain.Provider<RedPandaEntity> brainProvider() {
        return RedPandaAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return RedPandaAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    protected void customServerAiStep() {
        var level = this.level();
        var profiler = level.getProfiler();

        profiler.push("redPandaBrain");
        ((Brain<RedPandaEntity>) this.getBrain()).tick((ServerLevel) this.level(), this);
        profiler.pop();

        profiler.push("redPandaActivityUpdate");
        RedPandaAI.updateActivity(this);
        profiler.pop();

        if (this.isTame() && this.tickCount % 10 == 0) {
            scareHostiles();
        }

        super.customServerAiStep();
    }

    private void scareHostiles() {
        List<Monster> monsters = this.level().getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(10.0D));
        for (Monster monster : monsters) {
            Vec3 fleePos = DefaultRandomPos.getPosAway(monster, 16, 7, this.position());
            if (fleePos != null) {
                monster.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.2D);
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (itemstack.is(Items.SWEET_BERRIES) && !this.isTame()) {
            if (!this.level().isClientSide) {
                this.usePlayerItem(player, hand, itemstack);
                if (this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.navigation.stop();
                    this.setOrderedToSit(true);
                    this.level().broadcastEntityEvent(this, (byte)7);
                } else {
                    this.level().broadcastEntityEvent(this, (byte)6);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        var result = super.hurt(source, amount);
        if (result) {
            this.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);
            RedPandaAI.updateActivity(this);
        }
        return result;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.RED_PANDA_FOOD);
    }

    public boolean isSleeping() {
        return this.getBrain().isActive(Activity.REST);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.RED_PANDA.get().create(level);
        if (baby != null) baby.setBaby(true);
        return baby;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 16D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 16D);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationState<RedPandaEntity> state) {
        var controller = state.getController();

        if (this.isSleeping()) {
            controller.setAnimation(SLEEP);
        } else if (state.isMoving()) {
            controller.setAnimation(WALK);
        } else {
            controller.setAnimation(IDLE);
        }

        return PlayState.CONTINUE;
    }
}
