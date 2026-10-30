package it.hurts.shatterbyte.reanimal.mixin.client;

import it.hurts.shatterbyte.reanimal.client.light.ColoredGlow;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFeatureRenderer.class)
public abstract class ItemFeatureRendererMixin {
    @Unique
    private static int reanimal$tint = ColoredGlow.WHITE;

    @Inject(method = "prepareMainSubmit", at = @At("HEAD"))
    private void reanimal$beginTint(ItemFeatureRenderer.Submit submit, CallbackInfo ci) {
        reanimal$tint = ColoredGlow.tintAtPose(submit.pose(), submit.lightCoords());
    }

    @Inject(method = "prepareMainSubmit", at = @At("RETURN"))
    private void reanimal$endTint(ItemFeatureRenderer.Submit submit, CallbackInfo ci) {
        reanimal$tint = ColoredGlow.WHITE;
    }

    @Inject(method = "getLayerColorSafe([ILnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;)I", at = @At("RETURN"), cancellable = true)
    private static void reanimal$tintLayer(int[] layers, BakedQuad.MaterialInfo info, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ColoredGlow.apply(cir.getReturnValueI(), reanimal$tint));
    }
}
