package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.blockentity.GlowLightBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ReAnimalBlockEntities {
    public static BlockEntityType<GlowLightBlockEntity> GLOW_LIGHT;

    public static void init() {
        GLOW_LIGHT = register("glow_light",
                new BlockEntityType<>(GlowLightBlockEntity::new, java.util.Set.of(ReAnimalBlocks.GLOW_LIGHT)));
    }

    private static <T extends BlockEntityType<?>> T register(String id, T type) {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(ReAnimal.MODID, id), type);
        return type;
    }
}
