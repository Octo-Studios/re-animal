package it.hurts.shatterbyte.reanimal.common.effect;

import it.hurts.shatterbyte.reanimal.init.ReAnimalMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber
public class QuillMobEffect extends MobEffect {
    public QuillMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x523d38);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        var target = event.getEntity();

        var effect = target.getEffect(ReAnimalMobEffects.QUILL);

        if (effect == null)
            return;

        var attackerEntity = event.getSource().getEntity();

        if (!(attackerEntity instanceof LivingEntity attacker))
            return;

        if (attacker == target)
            return;

        if (target.level().isClientSide())
            return;

        var baseMultiplier = (effect.getAmplifier() + 1) * 0.2F;
        var reflected = event.getAmount() * baseMultiplier;

        if (reflected > 0F)
            attacker.hurt(target.damageSources().thorns(target), reflected);
    }
}