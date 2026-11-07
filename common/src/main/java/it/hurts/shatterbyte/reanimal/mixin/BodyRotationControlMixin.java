package it.hurts.shatterbyte.reanimal.mixin;

import it.hurts.shatterbyte.reanimal.common.entity.PerchingAnimalEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BodyRotationControl.class)
public abstract class BodyRotationControlMixin {
    @Shadow
    @Final
    private Mob mob;

    @Inject(method = "notCarryingMobPassengers", at = @At("HEAD"), cancellable = true)
    private void reanimal$allowBodyRotationWhilePerched(CallbackInfoReturnable<Boolean> cir) {
        if (this.mob.getFirstPassenger() instanceof PerchingAnimalEntity)
            cir.setReturnValue(true);
    }
}
