package it.hurts.shatterbyte.reanimal.common.effect;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class CrampsMobEffect extends MobEffect {
    private static final Identifier MOVE_SPEED_MODIFIER = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "cramps_move_speed");
    private static final Identifier SWIM_SPEED_MODIFIER = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "cramps_swim_speed");

    public CrampsMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x991616);

        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, MOVE_SPEED_MODIFIER, -0.1D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(net.minecraft.server.level.ServerLevel level, LivingEntity entity, int amplifier) {
        entity.hurt(entity.damageSources().cactus(), 1 + amplifier);

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int i = 25 >> amplifier;

        return i == 0 || duration % i == 0;
    }
}
