package it.hurts.shatterbyte.reanimal.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;

public class PigeonSplatParticle extends SingleQuadParticle {
    private static final float FADE_START = 0.75F;

    public PigeonSplatParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D, sprite);

        this.lifetime = 2400 + this.random.nextInt(1200);
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.quadSize = 0.1F + this.random.nextFloat() * 0.08F;
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
    }

    @Override
    public void extract(QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTickTime) {
        var progress = (this.age + partialTickTime) / this.lifetime;

        this.alpha = 1.0F - Mth.clamp((progress - FADE_START) / (1.0F - FADE_START), 0.0F, 1.0F);

        var rotation = new Quaternionf();

        rotation.rotationX(-Mth.HALF_PI);
        rotation.rotateZ(this.roll);

        this.extractRotatedQuad(particleTypeRenderState, camera, rotation, partialTickTime);
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new PigeonSplatParticle(level, x, y, z, this.sprites.get(random));
        }
    }
}
