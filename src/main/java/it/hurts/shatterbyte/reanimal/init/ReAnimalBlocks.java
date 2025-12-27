package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.block.CrocodileEggBlock;
import it.hurts.shatterbyte.reanimal.common.block.GlowLightBlock;
import it.hurts.shatterbyte.reanimal.common.block.QuillPlatformBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
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

    public static final DeferredHolder<Block, GlowLightBlock> GLOW_LIGHT = BLOCKS.register("glow_light", () ->
            new GlowLightBlock(BlockBehaviour.Properties.of()
                    .strength(0.0F)
                    .lightLevel(state -> 15)
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()));

    public static final DeferredHolder<Block, CrocodileEggBlock> CROCODILE_EGG = BLOCKS.register("crocodile_egg", () ->
            new CrocodileEggBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .forceSolidOn()
                    .strength(0.5F)
                    .sound(SoundType.METAL)
                    .randomTicks()
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
