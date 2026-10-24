package it.hurts.shatterbyte.reanimal.common.effect;

import it.hurts.shatterbyte.reanimal.init.ReAnimalMobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class QuillMobEffect extends MobEffect {
    public QuillMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x523d38);
    }

    public static void onLivingHurt(LivingEntity target, DamageSource source, float amount) {
        var effect = target.getEffect(ReAnimalMobEffects.QUILL);

        if (effect == null)
            return;

        var attackerEntity = source.getEntity();

        if (!(attackerEntity instanceof LivingEntity attacker))
            return;

        if (attacker == target)
            return;

        if (target.level().isClientSide())
            return;

        var baseMultiplier = (effect.getAmplifier() + 1) * 0.2F;
        var reflected = amount * baseMultiplier;

        if (reflected > 0F)
            attacker.hurt(target.damageSources().thorns(target), reflected);
    }
}