package it.hurts.shatterbyte.reanimal.mixin;

import it.hurts.shatterbyte.reanimal.common.entity.PigeonEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Cat.class)
public class CatMixin {
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void addPigeonTargetGoal(CallbackInfo ci) {
        Cat cat = (Cat) (Object) this;
        cat.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(cat, PigeonEntity.class, true));
    }
}
