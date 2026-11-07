package it.hurts.shatterbyte.reanimal.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import it.hurts.shatterbyte.reanimal.common.entity.PerchingAnimalEntity;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @ModifyExpressionValue(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
    private boolean reanimal$allowPlayerPerching(boolean canSerialize, Entity host, boolean force, boolean sendEventAndTriggers) {
        return canSerialize || ((Object) this instanceof PerchingAnimalEntity || (Object) this instanceof HedgehogEntity) && host instanceof Player;
    }

    @Inject(method = {"addPassenger", "removePassenger"}, at = @At("TAIL"))
    private void reanimal$syncPlayerPerching(Entity passenger, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player && (passenger instanceof PerchingAnimalEntity || passenger instanceof HedgehogEntity))
            player.connection.send(new ClientboundSetPassengersPacket(player));
    }
}
