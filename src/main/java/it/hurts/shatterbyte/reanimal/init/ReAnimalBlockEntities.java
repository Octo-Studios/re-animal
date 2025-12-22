package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.blockentity.GlowLightBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ReAnimalBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ReAnimal.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GlowLightBlockEntity>> GLOW_LIGHT =
            BLOCK_ENTITIES.register("glow_light", () -> BlockEntityType.Builder.of(GlowLightBlockEntity::new, ReAnimalBlocks.GLOW_LIGHT.get()).build(null));
}
