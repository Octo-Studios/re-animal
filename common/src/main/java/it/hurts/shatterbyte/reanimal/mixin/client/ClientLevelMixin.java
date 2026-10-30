package it.hurts.shatterbyte.reanimal.mixin.client;

import it.hurts.shatterbyte.reanimal.client.light.ColoredGlow;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void reanimal$updateColoredGlow(BooleanSupplier haveTime, CallbackInfo ci) {
        ColoredGlow.update((ClientLevel) (Object) this);
    }

    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void reanimal$refloodGlow(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        ColoredGlow.onBlockChanged(pos, oldState, newState);
    }
}
