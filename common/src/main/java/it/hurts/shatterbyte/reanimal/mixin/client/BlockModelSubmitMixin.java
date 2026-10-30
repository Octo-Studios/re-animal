package it.hurts.shatterbyte.reanimal.mixin.client;

import it.hurts.shatterbyte.reanimal.client.light.ColoredGlow;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelFeatureRenderer.Submit.class)
public abstract class BlockModelSubmitMixin {
    @Inject(method = "tintColor", at = @At("RETURN"), cancellable = true)
    private void reanimal$tintGlowLight(CallbackInfoReturnable<Integer> cir) {
        var submit = (BlockModelFeatureRenderer.Submit) (Object) this;
        var tint = ColoredGlow.tintAtPose(submit.pose(), submit.lightCoords());

        cir.setReturnValue(ColoredGlow.apply(cir.getReturnValueI(), tint));
    }
}
