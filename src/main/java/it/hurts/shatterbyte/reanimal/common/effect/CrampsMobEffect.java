package it.hurts.shatterbyte.reanimal.common.effect;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForgeMod;

public class CrampsMobEffect extends MobEffect {
    private static final ResourceLocation MOVE_SPEED_MODIFIER = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "cramps_move_speed");
    private static final ResourceLocation SWIM_SPEED_MODIFIER = ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, "cramps_swim_speed");

    public CrampsMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x991616);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        this.applyModifiers(entity, amplifier);
    }

    @Override
    public void onEffectAdded(LivingEntity entity, int amplifier) {
        this.applyModifiers(entity, amplifier);
    }

    @Override
    public void onMobRemoved(LivingEntity entity, int amplifier, Entity.RemovalReason reason) {
        this.removeModifiers(entity);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }

    private void applyModifiers(LivingEntity entity, int amplifier) {
        var moveAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        var swimAttr = entity.getAttribute(NeoForgeMod.SWIM_SPEED);

        if (moveAttr != null)
            this.replaceModifier(moveAttr, MOVE_SPEED_MODIFIER, -0.1D * (amplifier + 1));

        if (swimAttr != null)
            this.replaceModifier(swimAttr, SWIM_SPEED_MODIFIER, -0.2D * (amplifier + 1));
    }

    private void removeModifiers(LivingEntity entity) {
        var moveAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        var swimAttr = entity.getAttribute(NeoForgeMod.SWIM_SPEED);

        if (moveAttr != null)
            moveAttr.removeModifier(MOVE_SPEED_MODIFIER);

        if (swimAttr != null)
            swimAttr.removeModifier(SWIM_SPEED_MODIFIER);
    }

    private void replaceModifier(AttributeInstance attribute, ResourceLocation id, double amount) {
        attribute.removeModifier(id);
        attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
