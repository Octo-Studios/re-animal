package it.hurts.shatterbyte.reanimal.common.blockentity;

import it.hurts.shatterbyte.reanimal.common.block.GlowLightBlock;
import it.hurts.shatterbyte.reanimal.init.ReAnimalBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GlowLightBlockEntity extends BlockEntity {
    private int age;

    public GlowLightBlockEntity(BlockPos pos, BlockState state) {
        super(ReAnimalBlockEntities.GLOW_LIGHT.get(), pos, state);
    }

    public void refresh() {
        this.age = 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GlowLightBlockEntity entity) {
        if (level.isClientSide())
            return;

        entity.age++;

        if (entity.age < 2)
            return;

        var replace = state.getValue(GlowLightBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        level.setBlock(pos, replace, 3);
    }
}
