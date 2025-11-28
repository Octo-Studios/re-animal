package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.block.QuillPlatformBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ReAnimalBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ReAnimal.MODID);

    public static final DeferredHolder<Block, QuillPlatformBlock> QUILL_PLATFORM = BLOCKS.register("quill_platform", () ->
            new QuillPlatformBlock(BlockBehaviour.Properties.of()
                    .strength(0.3F)
                    .sound(SoundType.WOOD)
                    .noCollission()
                    .noOcclusion()));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
