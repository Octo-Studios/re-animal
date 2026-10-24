package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;

public class ReAnimalPotions {
    public static Holder<Potion> QUILL;
    public static Holder<Potion> LONG_QUILL;
    public static Holder<Potion> STRONG_QUILL;
    public static Holder<Potion> CRAMPS;
    public static Holder<Potion> LONG_CRAMPS;
    public static Holder<Potion> STRONG_CRAMPS;
    public static Holder<Potion> GLOWING;
    public static Holder<Potion> LONG_GLOWING;
    public static Holder<Potion> STRONG_GLOWING;

    public static void init() {
        QUILL = register("quill", new Potion("quill", new MobEffectInstance(ReAnimalMobEffects.QUILL, 20 * 270)));
        LONG_QUILL = register("long_quill", new Potion("long_quill", new MobEffectInstance(ReAnimalMobEffects.QUILL, 20 * 540)));
        STRONG_QUILL = register("strong_quill", new Potion("strong_quill", new MobEffectInstance(ReAnimalMobEffects.QUILL, 20 * 120, 1)));
        CRAMPS = register("cramps", new Potion("cramps", new MobEffectInstance(ReAnimalMobEffects.CRAMPS, 20 * 180)));
        LONG_CRAMPS = register("long_cramps", new Potion("long_cramps", new MobEffectInstance(ReAnimalMobEffects.CRAMPS, 20 * 480)));
        STRONG_CRAMPS = register("strong_cramps", new Potion("strong_cramps", new MobEffectInstance(ReAnimalMobEffects.CRAMPS, 20 * 90, 1)));
        GLOWING = register("glowing", new Potion("glowing", new MobEffectInstance(MobEffects.GLOWING, 20 * 180)));
        LONG_GLOWING = register("long_glowing", new Potion("long_glowing", new MobEffectInstance(MobEffects.GLOWING, 20 * 480)));
        STRONG_GLOWING = register("strong_glowing", new Potion("strong_glowing", new MobEffectInstance(MobEffects.GLOWING, 20 * 90, 1)));
    }

    private static Holder<Potion> register(String id, Potion potion) {
        var rl = Identifier.fromNamespaceAndPath(ReAnimal.MODID, id);
        Registry.register(BuiltInRegistries.POTION, rl, potion);
        return BuiltInRegistries.POTION.wrapAsHolder(potion);
    }
}
