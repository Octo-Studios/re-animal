package it.hurts.shatterbyte.reanimal.client.light;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public final class GlowLightmap {
    private static volatile int[] colors = new int[256];

    private GlowLightmap() {
    }

    public static void update(LightmapRenderState state) {
        if (!state.needsUpdate)
            return;

        var updated = new int[256];
        var previous = colors;
        var backgroundChanged = false;
        var ambient = new Vector3f(state.ambientColor).max(new Vector3f(state.nightVisionColor).mul(state.nightVisionEffectIntensity));

        for (var sky = 0; sky < 16; sky++) {
            var background = new Vector3f(state.skyLightColor).mul(brightness(sky) * state.skyFactor).add(ambient);

            for (var block = 0; block < 16; block++) {
                var level = block / 15.0F;
                var mix = 2.0F * level - 1.0F;
                var color = new Vector3f(state.blockLightTint).lerp(LightmapRenderStateExtractor.WHITE, 0.9F * mix * mix)
                        .mul(brightness(block) * state.blockFactor).add(background)
                        .mul(1.0F - 0.3F * state.bossOverlayWorldDarkening, 1.0F - 0.4F * state.bossOverlayWorldDarkening, 1.0F - 0.4F * state.bossOverlayWorldDarkening)
                        .sub(state.darknessEffectScale, state.darknessEffectScale, state.darknessEffectScale);

                color.set(Math.clamp(color.x, 0.0F, 1.0F), Math.clamp(color.y, 0.0F, 1.0F), Math.clamp(color.z, 0.0F, 1.0F));

                var inverse = 1.0F - Math.max(color.x, Math.max(color.y, color.z));

                color.mul(1.0F + state.brightness * (inverse + inverse * inverse + inverse * inverse * inverse));

                updated[sky * 16 + block] = Math.round(color.x * 255.0F) << 16 | Math.round(color.y * 255.0F) << 8 | Math.round(color.z * 255.0F);
            }

            backgroundChanged |= updated[sky * 16] != previous[sky * 16];
        }

        colors = updated;

        if (backgroundChanged)
            ColoredGlow.onLightmapChanged();
    }

    public static int tint(int lightCoords, float red, float green, float blue, float amount) {
        var snapshot = colors;
        var block = LightCoordsUtil.smoothBlock(lightCoords) / 16.0F;
        var sky = LightCoordsUtil.smoothSky(lightCoords) / 16.0F;

        return 0xFF000000
                | channel(snapshot, block, sky, 16, red, amount) << 16
                | channel(snapshot, block, sky, 8, green, amount) << 8
                | channel(snapshot, block, sky, 0, blue, amount);
    }

    private static float brightness(int level) {
        var value = level / 15.0F;

        return value / (4.0F - 3.0F * value);
    }

    private static int channel(int[] snapshot, float block, float sky, int shift, float mix, float amount) {
        var lit = sample(snapshot, block, sky, shift);

        if (lit == 0.0F)
            return 255;

        var background = sample(snapshot, 0.0F, sky, shift);
        var contribution = Math.max(0.0F, 1.0F - background / lit);

        return Math.round((1.0F - amount * contribution * (1.0F - mix)) * 255.0F);
    }

    private static float sample(int[] snapshot, float block, float sky, int shift) {
        var x = (int) block;
        var y = (int) sky;
        var nextX = Math.min(x + 1, 15);
        var nextY = Math.min(y + 1, 15);

        return Mth.lerp(sky - y,
                Mth.lerp(block - x, snapshot[y * 16 + x] >> shift & 0xFF, snapshot[y * 16 + nextX] >> shift & 0xFF),
                Mth.lerp(block - x, snapshot[nextY * 16 + x] >> shift & 0xFF, snapshot[nextY * 16 + nextX] >> shift & 0xFF));
    }
}
