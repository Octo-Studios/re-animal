package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public class ReAnimalDamageTypes {
    public static final ResourceKey<DamageType> HEDGEHOG_SPIKES = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "hedgehog_spikes")
    );
    public static final ResourceKey<DamageType> SEA_URCHIN_SPIKES = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "sea_urchin_spikes")
    );
    public static final ResourceKey<DamageType> JELLYFISH_STING = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "jellyfish_sting")
    );
}
