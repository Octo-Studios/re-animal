package it.hurts.shatterbyte.reanimal.mixin;

import it.hurts.shatterbyte.reanimal.common.entity.red_panda.RedPandaEntity;
import it.hurts.shatterbyte.reanimal.common.entity.red_panda.RedPandaNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityCollisionContext.class)
public abstract class EntityCollisionContextMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void reanimal$passThroughLeaves(BlockState state, CollisionGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        var context = (EntityCollisionContext) (Object) this;

        if (context.getEntity() instanceof RedPandaEntity panda && state.is(BlockTags.LEAVES)) {
            var navigation = (RedPandaNavigation) panda.getNavigation();

            if (panda.isClimbing() || (navigation.isOnSurfacePath() && navigation.getMotion().y < 0D)
                    || !context.isAbove(state.getCollisionShape(level, pos), pos, false))
                cir.setReturnValue(Shapes.empty());
        }
    }
}
