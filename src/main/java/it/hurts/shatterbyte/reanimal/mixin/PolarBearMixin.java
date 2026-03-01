package it.hurts.shatterbyte.reanimal.mixin;

import it.hurts.shatterbyte.reanimal.common.entity.seal.SealEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PolarBear.class)
public abstract class PolarBearMixin extends Animal {

    protected PolarBearMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void reanimal$addSealTargetGoal(CallbackInfo ci) {
        PolarBear bear = (PolarBear) (Object) this;

        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(bear, SealEntity.class, true) {
            @Override
            public boolean canUse() {
                return !bear.isBaby() && super.canUse();
            }
        });
    }
}
