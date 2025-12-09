package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.effect.CrampsMobEffect;
import it.hurts.shatterbyte.reanimal.common.effect.QuillMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ReAnimalMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ReAnimal.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> QUILL = MOB_EFFECTS.register("quill", QuillMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> CRAMPS = MOB_EFFECTS.register("cramps", CrampsMobEffect::new);
}
