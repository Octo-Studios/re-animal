package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.item.QuillArrowItem;
import it.hurts.shatterbyte.reanimal.common.item.KiwiEgg;
import it.hurts.shatterbyte.reanimal.common.item.OstrichEgg;
import it.hurts.shatterbyte.reanimal.common.item.PigeonEgg;
import it.hurts.shatterbyte.reanimal.common.item.QuillFoodProperties;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ReAnimalItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ReAnimal.MODID);

    public static final DeferredHolder<Item, SpawnEggItem> HEDGEHOG_SPAWN_EGG = ITEMS.register("hedgehog_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.HEDGEHOG.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> OSTRICH_SPAWN_EGG = ITEMS.register("ostrich_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.OSTRICH.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> KIWI_SPAWN_EGG = ITEMS.register("kiwi_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.KIWI.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> PIGEON_SPAWN_EGG = ITEMS.register("pigeon_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.PIGEON.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> BUTTERFLY_SPAWN_EGG = ITEMS.register("butterfly_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.BUTTERFLY.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> CAPYBARA_SPAWN_EGG = ITEMS.register("capybara_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.CAPYBARA.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> HIPPOPOTAMUS_SPAWN_EGG = ITEMS.register("hippopotamus_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.HIPPOPOTAMUS.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> GIRAFFE_SPAWN_EGG = ITEMS.register("giraffe_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.GIRAFFE.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> DRAGONFLY_SPAWN_EGG = ITEMS.register("dragonfly_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.DRAGONFLY.get(), 0xffffff, 0xffffff, new Item.Properties()));

    public static final DeferredHolder<Item, KiwiEgg> KIWI_EGG = ITEMS.register("kiwi_egg", () -> new KiwiEgg(new Item.Properties()));
    public static final DeferredHolder<Item, OstrichEgg> OSTRICH_EGG = ITEMS.register("ostrich_egg", () -> new OstrichEgg(new Item.Properties()));
    public static final DeferredHolder<Item, PigeonEgg> PIGEON_EGG = ITEMS.register("pigeon_egg", () -> new PigeonEgg(new Item.Properties()));

    public static final DeferredHolder<Item, Item> QUILL = ITEMS.register("quill", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, QuillArrowItem> QUILL_ARROW = ITEMS.register("quill_arrow", () -> new QuillArrowItem(new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> QUILL_PLATFORM = ITEMS.register("quill_platform", () -> new BlockItem(ReAnimalBlocks.QUILL_PLATFORM.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> RAW_HEDGEHOG_MEAT = ITEMS.register("raw_hedgehog_meat", () -> new Item(new Item.Properties().food(QuillFoodProperties.RAW_HEDGEHOG_MEAT)));
    public static final DeferredHolder<Item, Item> COOKED_HEDGEHOG_MEAT = ITEMS.register("cooked_hedgehog_meat", () -> new Item(new Item.Properties().food(QuillFoodProperties.COOKED_HEDGEHOG_MEAT)));
    public static final DeferredHolder<Item, Item> RAW_CAPYBARA_MEAT = ITEMS.register("raw_capybara_meat", () -> new Item(new Item.Properties().food(QuillFoodProperties.RAW_CAPYBARA_MEAT)));
    public static final DeferredHolder<Item, Item> COOKED_CAPYBARA_MEAT = ITEMS.register("cooked_capybara_meat", () -> new Item(new Item.Properties().food(QuillFoodProperties.COOKED_CAPYBARA_MEAT)));
    public static final DeferredHolder<Item, Item> RAW_OSTRICH_MEAT = ITEMS.register("raw_ostrich_meat", () -> new Item(new Item.Properties().food(QuillFoodProperties.RAW_OSTRICH_MEAT)));
    public static final DeferredHolder<Item, Item> COOKED_OSTRICH_MEAT = ITEMS.register("cooked_ostrich_meat", () -> new Item(new Item.Properties().food(QuillFoodProperties.COOKED_OSTRICH_MEAT)));
    public static final DeferredHolder<Item, Item> RAW_KIWI_MEAT = ITEMS.register("raw_kiwi_meat", () -> new Item(new Item.Properties().food(QuillFoodProperties.RAW_KIWI_MEAT)));
    public static final DeferredHolder<Item, Item> COOKED_KIWI_MEAT = ITEMS.register("cooked_kiwi_meat", () -> new Item(new Item.Properties().food(QuillFoodProperties.COOKED_KIWI_MEAT)));
    public static final DeferredHolder<Item, Item> KIWI_FLUFF = ITEMS.register("kiwi_fluff", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> BUTTERFLY_POLLEN = ITEMS.register("butterfly_pollen", () -> new BoneMealItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_PIGEON_MEAT = ITEMS.register("raw_pigeon_meat", () -> new Item(new Item.Properties().food(QuillFoodProperties.RAW_PIGEON_MEAT)));
    public static final DeferredHolder<Item, Item> COOKED_PIGEON_MEAT = ITEMS.register("cooked_pigeon_meat", () -> new Item(new Item.Properties().food(QuillFoodProperties.COOKED_PIGEON_MEAT)));
}
