package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ReAnimalPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, ReAnimal.MODID);

    public static final DeferredHolder<Potion, Potion> QUILL = POTIONS.register("quill", () -> new Potion(new MobEffectInstance(ReAnimalMobEffects.QUILL, 20 * 270)));
    public static final DeferredHolder<Potion, Potion> LONG_QUILL = POTIONS.register("long_quill", () -> new Potion(new MobEffectInstance(ReAnimalMobEffects.QUILL, 20 * 540)));
    public static final DeferredHolder<Potion, Potion> STRONG_QUILL = POTIONS.register("strong_quill", () -> new Potion(new MobEffectInstance(ReAnimalMobEffects.QUILL, 20 * 120, 1)));
}
