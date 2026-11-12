package it.hurts.shatterbyte.reanimal.mixin;

import it.hurts.shatterbyte.reanimal.common.entity.ball.BallPlay;
import it.hurts.shatterbyte.reanimal.common.entity.ball.DolphinPlayWithBallGoal;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Dolphin.class)
public abstract class DolphinMixin extends AgeableWaterCreature {
    @Unique
    private BallPlay reanimal$ballPlay;

    protected DolphinMixin(EntityType<? extends AgeableWaterCreature> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void reanimal$addBallGoal(CallbackInfo ci) {
        var dolphin = (Dolphin) (Object) this;

        this.reanimal$ballPlay = new BallPlay(dolphin);
        this.goalSelector.addGoal(1, new DolphinPlayWithBallGoal(dolphin, this.reanimal$ballPlay));
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void reanimal$giveBall(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!this.isInWater() || !player.getItemInHand(hand).is(ReAnimalItems.BALL))
            return;

        if (!this.level().isClientSide())
            this.reanimal$ballPlay.giveBall(player, hand);

        cir.setReturnValue(this.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void reanimal$saveBallPlay(ValueOutput output, CallbackInfo ci) {
        if (!this.level().isClientSide())
            this.reanimal$ballPlay.save(output);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void reanimal$loadBallPlay(ValueInput input, CallbackInfo ci) {
        if (!this.level().isClientSide())
            this.reanimal$ballPlay.load(input);
    }
}
