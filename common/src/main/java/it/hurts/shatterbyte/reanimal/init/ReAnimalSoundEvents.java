package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ReAnimalSoundEvents {
    public static SoundEvent HEDGEHOG_IDLE;
    public static SoundEvent HEDGEHOG_HURT;
    public static SoundEvent HEDGEHOG_DEATH;
    public static SoundEvent HEDGEHOG_ROLL;
    public static SoundEvent HEDGEHOG_UNROLL;

    public static SoundEvent OSTRICH_IDLE;
    public static SoundEvent OSTRICH_HURT;
    public static SoundEvent OSTRICH_DEATH;

    public static SoundEvent KIWI_IDLE;
    public static SoundEvent KIWI_HURT;
    public static SoundEvent KIWI_DEATH;

    public static SoundEvent PIGEON_IDLE;
    public static SoundEvent PIGEON_HURT;
    public static SoundEvent PIGEON_DEATH;
    public static SoundEvent PIGEON_TAKEOFF;

    public static SoundEvent BUTTERFLY_IDLE;
    public static SoundEvent BUTTERFLY_HURT;
    public static SoundEvent BUTTERFLY_DEATH;
    public static SoundEvent BUTTERFLY_LOOP;

    public static SoundEvent DRAGONFLY_HURT;
    public static SoundEvent DRAGONFLY_DEATH;
    public static SoundEvent DRAGONFLY_LOOP;

    public static SoundEvent JELLYFISH_IDLE;
    public static SoundEvent JELLYFISH_HURT;
    public static SoundEvent JELLYFISH_DEATH;

    public static SoundEvent CAPYBARA_IDLE;
    public static SoundEvent CAPYBARA_HURT;
    public static SoundEvent CAPYBARA_DEATH;

    public static SoundEvent RED_PANDA_IDLE;
    public static SoundEvent RED_PANDA_HURT;
    public static SoundEvent RED_PANDA_DEATH;

    public static SoundEvent SEAL_IDLE;
    public static SoundEvent SEAL_HURT;
    public static SoundEvent SEAL_DEATH;

    public static SoundEvent HIPPOPOTAMUS_IDLE;
    public static SoundEvent HIPPOPOTAMUS_HURT;
    public static SoundEvent HIPPOPOTAMUS_DEATH;
    public static SoundEvent HIPPOPOTAMUS_BITE;

    public static SoundEvent CROCODILE_IDLE;
    public static SoundEvent CROCODILE_HURT;
    public static SoundEvent CROCODILE_DEATH;
    public static SoundEvent CROCODILE_BITE;

    public static SoundEvent GIRAFFE_IDLE;
    public static SoundEvent GIRAFFE_HURT;
    public static SoundEvent GIRAFFE_DEATH;

    public static SoundEvent VULTURE_IDLE;
    public static SoundEvent VULTURE_HURT;
    public static SoundEvent VULTURE_DEATH;

    public static SoundEvent PENGUIN_IDLE_ADULT;
    public static SoundEvent PENGUIN_IDLE_BABY;
    public static SoundEvent PENGUIN_HURT_ADULT;
    public static SoundEvent PENGUIN_HURT_BABY;
    public static SoundEvent PENGUIN_DEATH_ADULT;
    public static SoundEvent PENGUIN_DEATH_BABY;

    public static SoundEvent SEA_URCHIN_IDLE;
    public static SoundEvent SEA_URCHIN_HURT;
    public static SoundEvent SEA_URCHIN_DEATH;

    public static void init() {
        HEDGEHOG_IDLE = register("entity.hedgehog.idle");
        HEDGEHOG_HURT = register("entity.hedgehog.hurt");
        HEDGEHOG_DEATH = register("entity.hedgehog.death");
        HEDGEHOG_ROLL = register("entity.hedgehog.roll");
        HEDGEHOG_UNROLL = register("entity.hedgehog.unroll");

        OSTRICH_IDLE = register("entity.ostrich.idle");
        OSTRICH_HURT = register("entity.ostrich.hurt");
        OSTRICH_DEATH = register("entity.ostrich.death");

        KIWI_IDLE = register("entity.kiwi.idle");
        KIWI_HURT = register("entity.kiwi.hurt");
        KIWI_DEATH = register("entity.kiwi.death");

        PIGEON_IDLE = register("entity.pigeon.idle");
        PIGEON_HURT = register("entity.pigeon.hurt");
        PIGEON_DEATH = register("entity.pigeon.death");
        PIGEON_TAKEOFF = register("entity.pigeon.takeoff");

        BUTTERFLY_IDLE = register("entity.butterfly.idle");
        BUTTERFLY_HURT = register("entity.butterfly.hurt");
        BUTTERFLY_DEATH = register("entity.butterfly.death");
        BUTTERFLY_LOOP = register("entity.butterfly.loop");

        DRAGONFLY_HURT = register("entity.dragonfly.hurt");
        DRAGONFLY_DEATH = register("entity.dragonfly.death");
        DRAGONFLY_LOOP = register("entity.dragonfly.loop");

        JELLYFISH_IDLE = register("entity.jellyfish.idle");
        JELLYFISH_HURT = register("entity.jellyfish.hurt");
        JELLYFISH_DEATH = register("entity.jellyfish.death");

        CAPYBARA_IDLE = register("entity.capybara.idle");
        CAPYBARA_HURT = register("entity.capybara.hurt");
        CAPYBARA_DEATH = register("entity.capybara.death");

        RED_PANDA_IDLE = register("entity.red_panda.idle");
        RED_PANDA_HURT = register("entity.red_panda.hurt");
        RED_PANDA_DEATH = register("entity.red_panda.death");

        SEAL_IDLE = register("entity.seal.idle");
        SEAL_HURT = register("entity.seal.hurt");
        SEAL_DEATH = register("entity.seal.death");

        HIPPOPOTAMUS_IDLE = register("entity.hippopotamus.idle");
        HIPPOPOTAMUS_HURT = register("entity.hippopotamus.hurt");
        HIPPOPOTAMUS_DEATH = register("entity.hippopotamus.death");
        HIPPOPOTAMUS_BITE = register("entity.hippopotamus.bite");

        CROCODILE_IDLE = register("entity.crocodile.idle");
        CROCODILE_HURT = register("entity.crocodile.hurt");
        CROCODILE_DEATH = register("entity.crocodile.death");
        CROCODILE_BITE = register("entity.crocodile.bite");

        GIRAFFE_IDLE = register("entity.giraffe.idle");
        GIRAFFE_HURT = register("entity.giraffe.hurt");
        GIRAFFE_DEATH = register("entity.giraffe.death");

        VULTURE_IDLE = register("entity.vulture.idle");
        VULTURE_HURT = register("entity.vulture.hurt");
        VULTURE_DEATH = register("entity.vulture.death");

        PENGUIN_IDLE_ADULT = register("entity.penguin.idle_adult");
        PENGUIN_IDLE_BABY = register("entity.penguin.idle_baby");
        PENGUIN_HURT_ADULT = register("entity.penguin.hurt_adult");
        PENGUIN_HURT_BABY = register("entity.penguin.hurt_baby");
        PENGUIN_DEATH_ADULT = register("entity.penguin.death_adult");
        PENGUIN_DEATH_BABY = register("entity.penguin.death_baby");

        SEA_URCHIN_IDLE = register("entity.sea_urchin.idle");
        SEA_URCHIN_HURT = register("entity.sea_urchin.hurt");
        SEA_URCHIN_DEATH = register("entity.sea_urchin.death");
    }

    private static SoundEvent register(String name) {
        var rl = Identifier.fromNamespaceAndPath(ReAnimal.MODID, name);
        var sound = SoundEvent.createVariableRangeEvent(rl);
        Registry.register(BuiltInRegistries.SOUND_EVENT, rl, sound);
        return sound;
    }
}
