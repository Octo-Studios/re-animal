package it.hurts.shatterbyte.reanimal.client.particle;

import it.hurts.shatterbyte.reanimal.common.particle.ImpactParticleOptions;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ImpactParticle extends SingleQuadParticle {
    private static final float PIXEL_SIZE = 1F / 16F;

    private final Vec3 normal;
    private final Quaternionf orientation;
    private final Quaternionf backOrientation;

    private final float strength;
    private final float sizeScale;

    public ImpactParticle(ClientLevel level, double x, double y, double z, Vec3 impulse, TextureAtlasSprite sprite, float sizeScale) {
        super(level, x, y, z, sprite);

        var direction = impulse.normalize();

        this.normal = direction.equals(Vec3.ZERO) ? Direction.UP.getUnitVec3() : direction;
        this.orientation = new Quaternionf().rotationTo(0F, 0F, 1F, (float) this.normal.x, (float) this.normal.y, (float) this.normal.z);
        this.backOrientation = new Quaternionf(this.orientation).rotateY((float) Math.PI);
        this.strength = (float) Math.min(impulse.length(), 1D);
        this.sizeScale = sizeScale;
        this.lifetime = 10;
        this.hasPhysics = false;
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTick) {
        var progress = Math.min(1F, (this.age + partialTick) / this.lifetime);
        var radius = Math.max(1, Math.round((2F + progress * (5F + this.strength * 4F)) * this.sizeScale));
        var inner = radius * (radius - 1);
        var outer = radius * (radius + 1);
        var remaining = 1F - progress;
        var color = ARGB.colorFromFloat(remaining * remaining * (0.4F + this.strength * 0.4F), 1F, 1F, 1F);
        var light = this.getLightCoords(partialTick);
        var u = this.sprite.getU(0.5F);
        var v = this.sprite.getV(0.5F);

        var toCamera = camera.position().subtract(this.x, this.y, this.z);
        var facing = this.normal.dot(toCamera) < 0D ? this.backOrientation : this.orientation;
        var right = this.orientation.transformPositiveX(new Vector3f()).mul(PIXEL_SIZE);
        var up = this.orientation.transformPositiveY(new Vector3f()).mul(PIXEL_SIZE);
        var originX = (float) -toCamera.x;
        var originY = (float) -toCamera.y;
        var originZ = (float) -toCamera.z;

        for (var y = -radius; y <= radius; y++) {
            for (var x = -radius; x <= radius; x++) {
                var distance = x * x + y * y;

                if (distance <= inner || distance > outer)
                    continue;

                renderState.add(this.getLayer(),
                        originX + x * right.x + y * up.x,
                        originY + x * right.y + y * up.y,
                        originZ + x * right.z + y * up.z,
                        facing.x, facing.y, facing.z, facing.w, PIXEL_SIZE * 0.5F,
                        u, u, v, v, color, light);
            }
        }
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<ImpactParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ImpactParticleOptions options, ClientLevel level, double x, double y, double z,
                                       double xAux, double yAux, double zAux, RandomSource random) {
            return new ImpactParticle(level, x, y, z, new Vec3(xAux, yAux, zAux), this.sprites.get(random), options.getScale());
        }
    }
}
