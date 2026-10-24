package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.effect.CrampsMobEffect;
import it.hurts.shatterbyte.reanimal.common.effect.QuillMobEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

public class ReAnimalMobEffects {
    public static Holder<MobEffect> QUILL;
    public static Holder<MobEffect> CRAMPS;

    public static void init() {
        QUILL = register("quill", new QuillMobEffect());
        CRAMPS = register("cramps", new CrampsMobEffect());
    }

    private static Holder<MobEffect> register(String id, MobEffect effect) {
        var rl = Identifier.fromNamespaceAndPath(ReAnimal.MODID, id);
        Registry.register(BuiltInRegistries.MOB_EFFECT, rl, effect);
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }
}
