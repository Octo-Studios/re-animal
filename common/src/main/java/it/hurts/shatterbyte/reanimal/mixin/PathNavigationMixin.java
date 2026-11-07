package it.hurts.shatterbyte.reanimal.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PathNavigation.class)
public abstract class PathNavigationMixin {
    @Shadow
    @Final
    protected Mob mob;

    @ModifyReturnValue(method = "isStableDestination", at = @At("RETURN"))
    private boolean reanimal$allowStrollingOnSolidGround(boolean stable, BlockPos pos) {
        if (!stable && (Object) this instanceof GroundPathNavigation
                && ReAnimal.MODID.equals(EntityType.getKey(this.mob.getType()).getNamespace())) {
            var below = pos.below();

            return this.mob.level().getBlockState(below).entityCanStandOn(this.mob.level(), below, this.mob);
        }

        return stable;
    }
}
