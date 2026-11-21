package it.hurts.shatterbyte.reanimal.common.particle;

import com.mojang.serialization.MapCodec;
import it.hurts.shatterbyte.reanimal.init.ReAnimalParticleTypes;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ScalableParticleOptionsBase;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class ImpactParticleOptions extends ScalableParticleOptionsBase {
    public static final MapCodec<ImpactParticleOptions> CODEC = SCALE.optionalFieldOf("scale", 1F)
            .xmap(ImpactParticleOptions::new, ImpactParticleOptions::getScale);
    public static final StreamCodec<RegistryFriendlyByteBuf, ImpactParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ImpactParticleOptions::getScale,
            ImpactParticleOptions::new
    );

    public ImpactParticleOptions(float scale) {
        super(scale);
    }

    @Override
    public ParticleType<ImpactParticleOptions> getType() {
        return ReAnimalParticleTypes.IMPACT;
    }
}
