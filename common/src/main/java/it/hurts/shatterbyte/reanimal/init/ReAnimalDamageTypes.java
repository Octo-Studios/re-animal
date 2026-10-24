package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

public class ReAnimalDamageTypes {
    public static final ResourceKey<DamageType> HEDGEHOG_SPIKES = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hedgehog_spikes")
    );
    public static final ResourceKey<DamageType> SEA_URCHIN_SPIKES = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(ReAnimal.MODID, "sea_urchin_spikes")
    );
    public static final ResourceKey<DamageType> JELLYFISH_STING = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(ReAnimal.MODID, "jellyfish_sting")
    );

    public static DamageSource source(Entity entity, ResourceKey<DamageType> key) {
        return new DamageSource(entity.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key), entity);
    }
}
