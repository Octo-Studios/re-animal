package it.hurts.shatterbyte.reanimal.mixin;

import it.hurts.shatterbyte.reanimal.common.entity.ball.BallEntity;
import it.hurts.shatterbyte.reanimal.common.entity.ball.BallPlay;
import it.hurts.shatterbyte.reanimal.common.entity.ball.WolfPlayWithBallGoal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Wolf.class)
public abstract class WolfMixin extends TamableAnimal {
    @Unique
    private BallPlay reanimal$ballPlay;

    protected WolfMixin(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return passenger instanceof BallEntity ball ? ball.getCarriedPosition((Wolf) (Object) this, 1F).subtract(this.position())
                : super.getPassengerAttachmentPoint(passenger, dimensions, scale);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void reanimal$addBallGoal(CallbackInfo ci) {
        var wolf = (Wolf) (Object) this;

        this.reanimal$ballPlay = new BallPlay(wolf);
        this.goalSelector.addGoal(6, new WolfPlayWithBallGoal(wolf, this.reanimal$ballPlay));
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
