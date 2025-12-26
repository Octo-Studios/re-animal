package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.item.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;

public class ReAnimalItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ReAnimal.MODID);

    public static final DeferredHolder<Item, SpawnEggItem> HEDGEHOG_SPAWN_EGG = ITEMS.register("hedgehog_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.HEDGEHOG.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> OSTRICH_SPAWN_EGG = ITEMS.register("ostrich_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.OSTRICH.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> KIWI_SPAWN_EGG = ITEMS.register("kiwi_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.KIWI.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> PIGEON_SPAWN_EGG = ITEMS.register("pigeon_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.PIGEON.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> BUTTERFLY_SPAWN_EGG = ITEMS.register("butterfly_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.BUTTERFLY.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> CAPYBARA_SPAWN_EGG = ITEMS.register("capybara_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.CAPYBARA.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> SEAL_SPAWN_EGG = ITEMS.register("seal_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.SEAL.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> HIPPOPOTAMUS_SPAWN_EGG = ITEMS.register("hippopotamus_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.HIPPOPOTAMUS.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> CROCODILE_SPAWN_EGG = ITEMS.register("crocodile_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.CROCODILE.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> GIRAFFE_SPAWN_EGG = ITEMS.register("giraffe_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.GIRAFFE.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> DRAGONFLY_SPAWN_EGG = ITEMS.register("dragonfly_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.DRAGONFLY.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> VULTURE_SPAWN_EGG = ITEMS.register("vulture_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.VULTURE.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> PENGUIN_SPAWN_EGG = ITEMS.register("penguin_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.PENGUIN.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> SEA_URCHIN_SPAWN_EGG = ITEMS.register("sea_urchin_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.SEA_URCHIN.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> JELLYFISH_SPAWN_EGG = ITEMS.register("jellyfish_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.JELLYFISH.get(), 0xffffff, 0xffffff, new Item.Properties()));

    public static final DeferredHolder<Item, KiwiEgg> KIWI_EGG = ITEMS.register("kiwi_egg", () -> new KiwiEgg(new Item.Properties()));
    public static final DeferredHolder<Item, OstrichEgg> OSTRICH_EGG = ITEMS.register("ostrich_egg", () -> new OstrichEgg(new Item.Properties()));
    public static final DeferredHolder<Item, PigeonEgg> PIGEON_EGG = ITEMS.register("pigeon_egg", () -> new PigeonEgg(new Item.Properties()));
    public static final DeferredHolder<Item, VultureEgg> VULTURE_EGG = ITEMS.register("vulture_egg", () -> new VultureEgg(new Item.Properties()));
    public static final DeferredHolder<Item, PenguinEgg> PENGUIN_EGG = ITEMS.register("penguin_egg", () -> new PenguinEgg(new Item.Properties()));
    public static final DeferredHolder<Item, CrocodileEgg> CROCODILE_EGG = ITEMS.register("crocodile_egg", () -> new CrocodileEgg(new Item.Properties()));

    public static final DeferredHolder<Item, Item> BUTTERFLY_POLLEN = ITEMS.register("butterfly_pollen", () -> new BoneMealItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> QUILL = ITEMS.register("quill", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, QuillArrowItem> QUILL_ARROW = ITEMS.register("quill_arrow", () -> new QuillArrowItem(new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> QUILL_PLATFORM = ITEMS.register("quill_platform", () -> new BlockItem(ReAnimalBlocks.QUILL_PLATFORM.get(), new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_WHITE = ITEMS.register("glow_stick_white", () -> new GlowStickItem(new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_ORANGE = ITEMS.register("glow_stick_orange", () -> new GlowStickItem(new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_MAGENTA = ITEMS.register("glow_stick_magenta", () -> new GlowStickItem(new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_LIGHT_BLUE = ITEMS.register("glow_stick_light_blue", () -> new GlowStickItem(new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_YELLOW = ITEMS.register("glow_stick_yellow", () -> new GlowStickItem(new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_LIME = ITEMS.register("glow_stick_lime", () -> new GlowStickItem(new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_PINK = ITEMS.register("glow_stick_pink", () -> new GlowStickItem(new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_GRAY = ITEMS.register("glow_stick_gray", () -> new GlowStickItem(new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_LIGHT_GRAY = ITEMS.register("glow_stick_light_gray", () -> new GlowStickItem(new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_CYAN = ITEMS.register("glow_stick_cyan", () -> new GlowStickItem(new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_PURPLE = ITEMS.register("glow_stick_purple", () -> new GlowStickItem(new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_BLUE = ITEMS.register("glow_stick_blue", () -> new GlowStickItem(new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_BROWN = ITEMS.register("glow_stick_brown", () -> new GlowStickItem(new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_GREEN = ITEMS.register("glow_stick_green", () -> new GlowStickItem(new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_RED = ITEMS.register("glow_stick_red", () -> new GlowStickItem(new Item.Properties()));
    public static final DeferredHolder<Item, GlowStickItem> GLOW_STICK_BLACK = ITEMS.register("glow_stick_black", () -> new GlowStickItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> RAW_HEDGEHOG_MEAT = ITEMS.register("raw_hedgehog_meat", () -> new Item(new Item.Properties().food(new FoodProperties(3, 1.8F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> COOKED_HEDGEHOG_MEAT = ITEMS.register("cooked_hedgehog_meat", () -> new Item(new Item.Properties().food(new FoodProperties(5, 6F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> RAW_CAPYBARA_MEAT = ITEMS.register("raw_capybara_meat", () -> new Item(new Item.Properties().food(new FoodProperties(3, 1.8F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> COOKED_CAPYBARA_MEAT = ITEMS.register("cooked_capybara_meat", () -> new Item(new Item.Properties().food(new FoodProperties(8, 12.8F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> RAW_SEAL_MEAT = ITEMS.register("raw_seal_meat", () -> new Item(new Item.Properties().food(new FoodProperties(3, 1.8F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> COOKED_SEAL_MEAT = ITEMS.register("cooked_seal_meat", () -> new Item(new Item.Properties().food(new FoodProperties(8, 12.8F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> RAW_OSTRICH_MEAT = ITEMS.register("raw_ostrich_meat", () -> new Item(new Item.Properties().food(new FoodProperties(2, 1.2F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> COOKED_OSTRICH_MEAT = ITEMS.register("cooked_ostrich_meat", () -> new Item(new Item.Properties().food(new FoodProperties(6, 7.2F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> RAW_KIWI_MEAT = ITEMS.register("raw_kiwi_meat", () -> new Item(new Item.Properties().food(new FoodProperties(3, 1.8F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> COOKED_KIWI_MEAT = ITEMS.register("cooked_kiwi_meat", () -> new Item(new Item.Properties().food(new FoodProperties(5, 6F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> RAW_PIGEON_MEAT = ITEMS.register("raw_pigeon_meat", () -> new Item(new Item.Properties().food(new FoodProperties(3, 1F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> COOKED_PIGEON_MEAT = ITEMS.register("cooked_pigeon_meat", () -> new Item(new Item.Properties().food(new FoodProperties(5, 5F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> RAW_HIPPOPOTAMUS_MEAT = ITEMS.register("raw_hippopotamus_meat", () -> new Item(new Item.Properties().food(new FoodProperties(3, 1.8F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> COOKED_HIPPOPOTAMUS_MEAT = ITEMS.register("cooked_hippopotamus_meat", () -> new Item(new Item.Properties().food(new FoodProperties(10, 14.8F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> RAW_CROCODILE_MEAT = ITEMS.register("raw_crocodile_meat", () -> new Item(new Item.Properties().food(new FoodProperties(3, 1.8F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> COOKED_CROCODILE_MEAT = ITEMS.register("cooked_crocodile_meat", () -> new Item(new Item.Properties().food(new FoodProperties(9, 12.8F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> RAW_GIRAFFE_MEAT = ITEMS.register("raw_giraffe_meat", () -> new Item(new Item.Properties().food(new FoodProperties(3, 1.8F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> COOKED_GIRAFFE_MEAT = ITEMS.register("cooked_giraffe_meat", () -> new Item(new Item.Properties().food(new FoodProperties(8, 12.8F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> SEA_URCHIN_CAVIAR = ITEMS.register("sea_urchin_caviar", () -> new Item(new Item.Properties().food(new FoodProperties(3, 5F, false, 1.6F, Optional.empty(), List.of()))));
    public static final DeferredHolder<Item, Item> JELLYFISH_JELLY = ITEMS.register("jellyfish_jelly", () -> new Item(new Item.Properties().food(new FoodProperties(2, 0.6F, false, 1.6F, Optional.empty(), List.of()))));
}
