package it.hurts.shatterbyte.reanimal.init;

import com.mojang.serialization.MapCodec;
import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.particle.ImpactParticleOptions;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public class ReAnimalParticleTypes {
    public static SimpleParticleType PIGEON_SPLAT;
    public static ParticleType<ImpactParticleOptions> IMPACT;

    public static void init() {
        PIGEON_SPLAT = register("pigeon_splat", new SimpleParticleType(false) {
        });
        IMPACT = register("impact", new ParticleType<ImpactParticleOptions>(false) {
            @Override
            public MapCodec<ImpactParticleOptions> codec() {
                return ImpactParticleOptions.CODEC;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, ImpactParticleOptions> streamCodec() {
                return ImpactParticleOptions.STREAM_CODEC;
            }
        });
    }

    private static <T extends ParticleType<?>> T register(String id, T type) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(ReAnimal.MODID, id), type);
    }
}
