package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ReAnimalSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, ReAnimal.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> HEDGEHOG_IDLE = register("entity.hedgehog.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEDGEHOG_HURT = register("entity.hedgehog.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEDGEHOG_DEATH = register("entity.hedgehog.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEDGEHOG_ROLL = register("entity.hedgehog.roll");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEDGEHOG_UNROLL = register("entity.hedgehog.unroll");

    public static final DeferredHolder<SoundEvent, SoundEvent> OSTRICH_IDLE = register("entity.ostrich.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> OSTRICH_HURT = register("entity.ostrich.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OSTRICH_DEATH = register("entity.ostrich.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> KIWI_IDLE = register("entity.kiwi.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> KIWI_HURT = register("entity.kiwi.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> KIWI_DEATH = register("entity.kiwi.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> PIGEON_IDLE = register("entity.pigeon.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIGEON_HURT = register("entity.pigeon.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIGEON_DEATH = register("entity.pigeon.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIGEON_TAKEOFF = register("entity.pigeon.takeoff");

    public static final DeferredHolder<SoundEvent, SoundEvent> BUTTERFLY_IDLE = register("entity.butterfly.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUTTERFLY_HURT = register("entity.butterfly.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUTTERFLY_DEATH = register("entity.butterfly.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> DRAGONFLY_HURT = register("entity.dragonfly.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRAGONFLY_DEATH = register("entity.dragonfly.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRAGONFLY_LOOP = register("entity.dragonfly.loop");

    public static final DeferredHolder<SoundEvent, SoundEvent> CAPYBARA_IDLE = register("entity.capybara.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAPYBARA_HURT = register("entity.capybara.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAPYBARA_DEATH = register("entity.capybara.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> SEAL_IDLE = register("entity.seal.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEAL_HURT = register("entity.seal.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEAL_DEATH = register("entity.seal.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> HIPPOPOTAMUS_IDLE = register("entity.hippopotamus.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIPPOPOTAMUS_HURT = register("entity.hippopotamus.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIPPOPOTAMUS_DEATH = register("entity.hippopotamus.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIPPOPOTAMUS_BITE = register("entity.hippopotamus.bite");

    public static final DeferredHolder<SoundEvent, SoundEvent> CROCODILE_IDLE = register("entity.crocodile.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> CROCODILE_HURT = register("entity.crocodile.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CROCODILE_DEATH = register("entity.crocodile.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CROCODILE_BITE = register("entity.crocodile.bite");

    public static final DeferredHolder<SoundEvent, SoundEvent> GIRAFFE_IDLE = register("entity.giraffe.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIRAFFE_HURT = register("entity.giraffe.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIRAFFE_DEATH = register("entity.giraffe.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> VULTURE_IDLE = register("entity.vulture.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> VULTURE_HURT = register("entity.vulture.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> VULTURE_DEATH = register("entity.vulture.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> PENGUIN_IDLE_ADULT = register("entity.penguin.idle_adult");
    public static final DeferredHolder<SoundEvent, SoundEvent> PENGUIN_IDLE_BABY = register("entity.penguin.idle_baby");
    public static final DeferredHolder<SoundEvent, SoundEvent> PENGUIN_HURT_ADULT = register("entity.penguin.hurt_adult");
    public static final DeferredHolder<SoundEvent, SoundEvent> PENGUIN_HURT_BABY = register("entity.penguin.hurt_baby");
    public static final DeferredHolder<SoundEvent, SoundEvent> PENGUIN_DEATH_ADULT = register("entity.penguin.death_adult");
    public static final DeferredHolder<SoundEvent, SoundEvent> PENGUIN_DEATH_BABY = register("entity.penguin.death_baby");

    public static final DeferredHolder<SoundEvent, SoundEvent> SEA_URCHIN_IDLE = register("entity.sea_urchin.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEA_URCHIN_HURT = register("entity.sea_urchin.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEA_URCHIN_DEATH = register("entity.sea_urchin.death");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, name)));
    }
}
