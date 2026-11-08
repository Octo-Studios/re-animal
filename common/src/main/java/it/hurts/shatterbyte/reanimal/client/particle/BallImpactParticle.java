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
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class BallImpactParticle extends SingleQuadParticle {
    private final Quaternionf orientation;
    private final Quaternionf backOrientation;

    private final float strength;

    public BallImpactParticle(ClientLevel level, double x, double y, double z, Vec3 impulse, TextureAtlasSprite sprite) {
        super(level, x, y, z, 0D, 0D, 0D, sprite);

        var normal = impulse.normalize();

        this.orientation = new Quaternionf().rotationTo(0F, 0F, 1F, (float) normal.x, (float) normal.y, (float) normal.z);
        this.backOrientation = new Quaternionf(this.orientation).rotateY((float) Math.PI);
        this.strength = (float) impulse.length();
        this.lifetime = 10;
        this.hasPhysics = false;
        this.xd = 0D;
        this.yd = 0D;
        this.zd = 0D;
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTick) {
        var progress = (this.age + partialTick) / this.lifetime;

        this.quadSize = 0.15F + progress * (0.4F + this.strength * 0.3F);
        this.alpha = (1F - progress) * (1F - progress) * (0.4F + this.strength * 0.4F);

        this.extractRotatedQuad(renderState, camera, this.orientation, partialTick);
        this.extractRotatedQuad(renderState, camera, this.backOrientation, partialTick);
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
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z,
                                       double xAux, double yAux, double zAux, RandomSource random) {
            return new BallImpactParticle(level, x, y, z, new Vec3(xAux, yAux, zAux), this.sprites.get(random));
        }
    }
}
