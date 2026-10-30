package it.hurts.shatterbyte.reanimal.mixin.client;

import com.geckolib.renderer.base.RenderPassInfo;
import it.hurts.shatterbyte.reanimal.client.light.ColoredGlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderPassInfo.class)
public abstract class RenderPassInfoMixin {
    @Inject(method = "renderColor", at = @At("RETURN"), cancellable = true)
    private void reanimal$tintGlowLight(CallbackInfoReturnable<Integer> cir) {
        var info = (RenderPassInfo<?>) (Object) this;
        var tint = ColoredGlow.tintOf(info.renderState(), info.packedLight(), info.poseStack().last());

        cir.setReturnValue(ColoredGlow.apply(cir.getReturnValueI(), tint));
    }
}
