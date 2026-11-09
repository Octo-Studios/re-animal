package it.hurts.shatterbyte.reanimal.mixin.client;

import it.hurts.shatterbyte.reanimal.client.renderer.glow_stick.GlowStickRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "raycastHitResult", at = @At("RETURN"), cancellable = true)
    private void reanimal$pickAttachedGlowSticks(float partialTick, Entity cameraEntity, CallbackInfoReturnable<HitResult> cir) {
        cir.setReturnValue(GlowStickRenderer.pickAttached((LocalPlayer) (Object) this, cameraEntity, partialTick, cir.getReturnValue()));
    }
}
