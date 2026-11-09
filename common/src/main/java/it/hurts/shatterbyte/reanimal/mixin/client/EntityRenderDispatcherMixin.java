package it.hurts.shatterbyte.reanimal.mixin.client;

import it.hurts.shatterbyte.reanimal.common.entity.PerchingAnimalEntity;
import it.hurts.shatterbyte.reanimal.common.entity.glow_stick.GlowStickEntity;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void reanimal$hideFirstPersonAttachments(Entity entity, Frustum culler, double camX, double camY, double camZ, float partialTicks, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof PerchingAnimalEntity || entity instanceof HedgehogEntity || entity instanceof GlowStickEntity))
            return;

        var camera = Minecraft.getInstance().gameRenderer.mainCamera();

        if (!camera.isDetached() && camera.entity() instanceof Player
                && (entity.getVehicle() == camera.entity() || entity instanceof GlowStickEntity stick && stick.getAttachedEntity() == camera.entity()))
            cir.setReturnValue(false);
    }
}
