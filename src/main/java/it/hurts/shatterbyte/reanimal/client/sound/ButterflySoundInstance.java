package it.hurts.shatterbyte.reanimal.client.sound;

import it.hurts.shatterbyte.reanimal.common.entity.butterfly.ButterflyEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ButterflySoundInstance extends AbstractTickableSoundInstance {
    private static final float VOLUME_MAX = 0.9F;
    private static final float SPEED_MIN = 0.01F;
    private final ButterflyEntity butterfly;

    public ButterflySoundInstance(ButterflyEntity butterfly) {
        super(ReAnimalSoundEvents.BUTTERFLY_LOOP.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.butterfly = butterfly;
        this.x = (double) ((float) butterfly.getX());
        this.y = (double) ((float) butterfly.getY());
        this.z = (double) ((float) butterfly.getZ());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
    }

    @Override
    public void tick() {
        if (!butterfly.isAlive() || butterfly.isRemoved()) {
            this.stop();
            return;
        }

        this.x = (double) ((float) butterfly.getX());
        this.y = (double) ((float) butterfly.getY());
        this.z = (double) ((float) butterfly.getZ());

        if (butterfly.onGround()) {
            this.pitch = 0.0F;
            this.volume = 0.0F;
            return;
        }

        float speed = (float) butterfly.getDeltaMovement().horizontalDistance();
        if (speed >= SPEED_MIN) {
            this.pitch = Mth.lerp(Mth.clamp(speed, getMinPitch(), getMaxPitch()), getMinPitch(), getMaxPitch());
            this.volume = Mth.lerp(Mth.clamp(speed, 0.0F, 0.5F), 0.0F, VOLUME_MAX);
        } else {
            this.pitch = 0.0F;
            this.volume = 0.0F;
        }
    }

    private float getMinPitch() {
        return butterfly.isBaby() ? 1.3F : 0.9F;
    }

    private float getMaxPitch() {
        return butterfly.isBaby() ? 1.6F : 1.2F;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        return !butterfly.isSilent();
    }
}
