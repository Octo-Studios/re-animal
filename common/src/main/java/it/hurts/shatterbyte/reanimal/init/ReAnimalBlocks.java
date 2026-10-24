package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.block.CrocodileEggBlock;
import it.hurts.shatterbyte.reanimal.common.block.GlowLightBlock;
import it.hurts.shatterbyte.reanimal.common.block.QuillPlatformBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class ReAnimalBlocks {
    public static QuillPlatformBlock QUILL_PLATFORM;
    public static GlowLightBlock GLOW_LIGHT;
    public static CrocodileEggBlock CROCODILE_EGG;

    public static void init() {
        QUILL_PLATFORM = register("quill_platform", props -> new QuillPlatformBlock(
                props
                        .strength(0.3F)
                        .sound(SoundType.WOOD)
                        .noCollision()
                        .noOcclusion()));

        GLOW_LIGHT = register("glow_light", props -> new GlowLightBlock(
                props
                        .strength(0.0F)
                        .lightLevel(state -> 15)
                        .noCollision()
                        .noOcclusion()
                        .noLootTable()));

        CROCODILE_EGG = register("crocodile_egg", props -> new CrocodileEggBlock(
                props
                        .mapColor(MapColor.SAND)
                        .forceSolidOn()
                        .strength(0.5F)
                        .sound(SoundType.METAL)
                        .randomTicks()
                        .noOcclusion()
                        .pushReaction(PushReaction.POPPED)));
    }

    private static <T extends Block> T register(String id, java.util.function.Function<BlockBehaviour.Properties, T> factory) {
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, Identifier.fromNamespaceAndPath(ReAnimal.MODID, id));
        var block = factory.apply(BlockBehaviour.Properties.of().setId(key));

        Registry.register(BuiltInRegistries.BLOCK, key, block);
        return block;
    }
}
