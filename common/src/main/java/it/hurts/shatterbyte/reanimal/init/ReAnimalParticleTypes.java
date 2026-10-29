package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class ReAnimalParticleTypes {
    public static SimpleParticleType PIGEON_SPLAT;

    public static void init() {
        PIGEON_SPLAT = register("pigeon_splat", new SimpleParticleType(false) {
        });
    }

    private static SimpleParticleType register(String id, SimpleParticleType type) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(ReAnimal.MODID, id), type);
    }
}
