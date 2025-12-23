package it.hurts.shatterbyte.reanimal.client.sound;

import it.hurts.shatterbyte.reanimal.common.entity.dragonfly.DragonflyEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DragonflySoundInstance extends AbstractTickableSoundInstance {
    private static final float VOLUME_MAX = 1.2F;
    private static final float SPEED_MIN = 0.01F;
    private final DragonflyEntity dragonfly;

    public DragonflySoundInstance(DragonflyEntity dragonfly) {
        super(ReAnimalSoundEvents.DRAGONFLY_LOOP.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.dragonfly = dragonfly;
        this.x = (double) ((float) dragonfly.getX());
        this.y = (double) ((float) dragonfly.getY());
        this.z = (double) ((float) dragonfly.getZ());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
    }

    @Override
    public void tick() {
        if (!dragonfly.isAlive() || dragonfly.isRemoved()) {
            this.stop();
            return;
        }

        this.x = (double) ((float) dragonfly.getX());
        this.y = (double) ((float) dragonfly.getY());
        this.z = (double) ((float) dragonfly.getZ());

        if (dragonfly.onGround()) {
            this.pitch = 0.0F;
            this.volume = 0.0F;
            return;
        }

        float speed = (float) dragonfly.getDeltaMovement().horizontalDistance();
        if (speed >= SPEED_MIN) {
            this.pitch = Mth.lerp(Mth.clamp(speed, getMinPitch(), getMaxPitch()), getMinPitch(), getMaxPitch());
            this.volume = Mth.lerp(Mth.clamp(speed, 0.0F, 0.5F), 0.0F, VOLUME_MAX);
        } else {
            this.pitch = 0.0F;
            this.volume = 0.0F;
        }
    }

    private float getMinPitch() {
        return dragonfly.isBaby() ? 1.1F : 0.7F;
    }

    private float getMaxPitch() {
        return dragonfly.isBaby() ? 1.5F : 1.1F;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        return !dragonfly.isSilent();
    }
}
